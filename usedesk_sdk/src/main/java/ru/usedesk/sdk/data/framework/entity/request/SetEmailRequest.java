package ru.usedesk.sdk.data.framework.entity.request;

public class SetEmailRequest extends BaseRequest {

    public static final String TYPE = "@@server/chat/SET_EMAIL";

    private String email;

    public SetEmailRequest(String token, String email) {
        super(TYPE, token);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}