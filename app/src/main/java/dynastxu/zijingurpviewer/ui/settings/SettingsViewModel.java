package dynastxu.zijingurpviewer.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {
    private static final String PREF_NAME = "settings_pref";
    private static final String KEY_CAPTCHA_PRE_FILLED = "captcha_pre_filled";
    private static final String KEY_SAVE_PASSWORD = "save_password";
    private static final String KEY_AUTO_LOGIN = "auto_login";

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
