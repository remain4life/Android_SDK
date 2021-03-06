package ru.usedesk.sdk.internal.data.framework.api.standard;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import ru.usedesk.sdk.R;
import ru.usedesk.sdk.external.entity.chat.UsedeskActionListener;
import ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.request.BaseRequest;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.BaseResponse;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.ErrorResponse;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.InitChatResponse;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.NewMessageResponse;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.SendFeedbackResponse;
import ru.usedesk.sdk.internal.data.framework.api.standard.entity.response.SetEmailResponse;
import ru.usedesk.sdk.internal.domain.entity.chat.OnMessageListener;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.BAD_REQUEST_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.DISCONNECTED;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.FORBIDDEN_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.INTERNAL_SERVER_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.IO_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.JSON_ERROR;
import static ru.usedesk.sdk.external.entity.exceptions.UsedeskSocketException.Error.UNKNOWN_FROM_SERVER_ERROR;

public class SocketApi {
    private static final String EVENT_SERVER_ACTION = "dispatch";

    private final Map<String, Emitter.Listener> emitterListeners = new HashMap<>(5);

    private final Gson gson;

    private Socket socket;

    private UsedeskActionListener actionListener;
    private final Emitter.Listener disconnectEmitterListener = args -> actionListener.onDisconnected();
    private final Emitter.Listener connectErrorEmitterListener = args -> {
        actionListener.onError(R.string.message_connecting_error);
        actionListener.onException(new UsedeskSocketException(DISCONNECTED));
    };
    private OnMessageListener onMessageListener;
    private final Emitter.Listener connectEmitterListener = args -> onMessageListener.onInitChat();
    private final Emitter.Listener baseEventEmitterListener = args -> {
        String rawResponse = args[0].toString();

        BaseResponse response = process(rawResponse);

        if (response != null) {
            switch (response.getType()) {
                case ErrorResponse.TYPE:
                    ErrorResponse errorResponse = (ErrorResponse) response;
                    UsedeskSocketException usedeskSocketException;
                    switch (errorResponse.getCode()) {
                        case HTTP_FORBIDDEN:
                            onMessageListener.onTokenError();
                            usedeskSocketException = new UsedeskSocketException(FORBIDDEN_ERROR, errorResponse.getMessage());
                            break;
                        case HTTP_BAD_REQUEST:
                            usedeskSocketException = new UsedeskSocketException(BAD_REQUEST_ERROR, errorResponse.getMessage());
                            break;
                        case HTTP_INTERNAL_ERROR:
                            usedeskSocketException = new UsedeskSocketException(INTERNAL_SERVER_ERROR, errorResponse.getMessage());
                            break;
                        default:
                            usedeskSocketException = new UsedeskSocketException(UNKNOWN_FROM_SERVER_ERROR, errorResponse.getMessage());
                            break;
                    }
                    actionListener.onException(usedeskSocketException);
                    break;
                case InitChatResponse.TYPE:
                    InitChatResponse initChatResponse = (InitChatResponse) response;
                    onMessageListener.onInit(initChatResponse.getToken(), initChatResponse.getSetup());
                    break;
                case SetEmailResponse.TYPE:
                    break;
                case NewMessageResponse.TYPE:
                    NewMessageResponse newMessageResponse = (NewMessageResponse) response;
                    onMessageListener.onNew(newMessageResponse.getMessage());
                    break;
                case SendFeedbackResponse.TYPE:
                    onMessageListener.onFeedback();
                    break;
            }
        }
    };

    @Inject
    SocketApi(Gson gson) {
        this.gson = gson;
    }

    public boolean isConnected() {
        return socket.connected();
    }

    public void setSocket(@NonNull String url) throws UsedeskSocketException {
        try {
            socket = IO.socket(url);
        } catch (URISyntaxException e) {
            throw new UsedeskSocketException(IO_ERROR, e.getMessage());
        }
    }

    public void connect(UsedeskActionListener actionListener, OnMessageListener onMessageListener) {
        if (socket == null) {
            return;
        }

        this.actionListener = actionListener;
        this.onMessageListener = onMessageListener;

        emitterListeners.put(EVENT_SERVER_ACTION, baseEventEmitterListener);
        emitterListeners.put(Socket.EVENT_CONNECT_ERROR, connectErrorEmitterListener);
        emitterListeners.put(Socket.EVENT_CONNECT_TIMEOUT, connectErrorEmitterListener);
        emitterListeners.put(Socket.EVENT_DISCONNECT, disconnectEmitterListener);
        emitterListeners.put(Socket.EVENT_CONNECT, connectEmitterListener);

        for (String event : emitterListeners.keySet()) {
            socket.on(event, emitterListeners.get(event));
        }

        socket.connect();
    }

    public void disconnect() {
        for (String event : emitterListeners.keySet()) {
            socket.off(event, emitterListeners.get(event));
        }

        emitterListeners.clear();

        socket.disconnect();
    }

    public void emitterAction(BaseRequest baseRequest) throws UsedeskSocketException {
        if (socket == null) {
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(gson.toJson(baseRequest));

            socket.emit(EVENT_SERVER_ACTION, jsonObject);
        } catch (JSONException e) {
            throw new UsedeskSocketException(JSON_ERROR, e.getMessage());
        }
    }

    private BaseResponse process(String rawResponse) {
        try {
            BaseRequest baseRequest = gson.fromJson(rawResponse, BaseRequest.class);

            if (baseRequest != null && baseRequest.getType() != null) {
                switch (baseRequest.getType()) {
                    case InitChatResponse.TYPE:
                        return gson.fromJson(rawResponse, InitChatResponse.class);
                    case ErrorResponse.TYPE:
                        return gson.fromJson(rawResponse, ErrorResponse.class);
                    case NewMessageResponse.TYPE:
                        return gson.fromJson(rawResponse, NewMessageResponse.class);
                    case SendFeedbackResponse.TYPE:
                        return gson.fromJson(rawResponse, SendFeedbackResponse.class);
                    case SetEmailResponse.TYPE:
                        return gson.fromJson(rawResponse, SetEmailResponse.class);
                }
            }
        } catch (JsonParseException e) {
            actionListener.onException(new UsedeskSocketException(JSON_ERROR, e.getMessage()));
        }

        return null;
    }
}
