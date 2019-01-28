package ru.usedesk.sdk.data.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import ru.usedesk.sdk.domain.entity.UsedeskConfiguration;
import ru.usedesk.sdk.domain.entity.exceptions.DataNotFoundException;

@Singleton
public class ConfigurationLoader extends DataLoader<UsedeskConfiguration> {
    private static final String PREF_NAME = "usedeskSdkConfiguration";
    private static final String KEY_ID = "id";
    private static final String KEY_URL = "url";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences sharedPreferences;

    @Inject
    public ConfigurationLoader(@NonNull Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    @Nullable
    protected UsedeskConfiguration loadData(){
        final String id = sharedPreferences.getString(KEY_ID, null);
        final String url = sharedPreferences.getString(KEY_URL, null);
        final String email = sharedPreferences.getString(KEY_EMAIL, null);

        if (id == null || url == null || email == null) {
            return null;
        }

        return new UsedeskConfiguration(id, email, url);
    }

    @Override
    protected void saveData(@NonNull UsedeskConfiguration configuration) {
        sharedPreferences.edit()
                .putString(KEY_ID, configuration.getCompanyId())
                .putString(KEY_URL, configuration.getUrl())
                .putString(KEY_EMAIL, configuration.getEmail())
                .apply();
    }
}
