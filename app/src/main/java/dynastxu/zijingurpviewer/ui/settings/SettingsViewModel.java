package dynastxu.zijingurpviewer.ui.settings;

import static dynastxu.zijingurpviewer.global.GlobalState.KEY_AUTO_LOGIN;
import static dynastxu.zijingurpviewer.global.GlobalState.KEY_CAPTCHA_PRE_FILLED;
import static dynastxu.zijingurpviewer.global.GlobalState.KEY_SAVE_PASSWORD;
import static dynastxu.zijingurpviewer.global.GlobalState.PREF_NAME;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {
    private SharedPreferences sharedPreferences;

    // 初始化 SharedPreferences
    public void init(@NonNull Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean getCAPTCHAPreFilled() {
        if (sharedPreferences == null) return false;
        return sharedPreferences.getBoolean(KEY_CAPTCHA_PRE_FILLED, false);
    }

    public void setCAPTCHAPreFilled(boolean value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit()
                    .putBoolean(KEY_CAPTCHA_PRE_FILLED, value)
                    .apply();
            if (!value) {
                setAutoLogin(false);
            }
        }
    }

    public boolean getSavePassword() {
        if (sharedPreferences == null) return false;
        return sharedPreferences.getBoolean(KEY_SAVE_PASSWORD, false);
    }

    public void setSavePassword(boolean value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit()
                    .putBoolean(KEY_SAVE_PASSWORD, value)
                    .apply();
            if (!value) {
                setAutoLogin(false);
            }
        }
    }

    public boolean getAutoLogin() {
        if (sharedPreferences == null) return false;
        return sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false);
    }

    public void setAutoLogin(boolean value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit()
                    .putBoolean(KEY_AUTO_LOGIN, value)
                    .apply();
            if (value) {
                setCAPTCHAPreFilled(true);
                setSavePassword(true);
            }
        }
    }
}
