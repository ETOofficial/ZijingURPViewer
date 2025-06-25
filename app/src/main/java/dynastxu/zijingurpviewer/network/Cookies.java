package dynastxu.zijingurpviewer.network;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Cookies {
    private final Map<String, String> cookies;
    public Cookies(){
        this.cookies = new HashMap<>();
    }
    public Cookies(Map<String, String> cookies){
        this.cookies = cookies;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    @NonNull
    public String buildCookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        Log.d("cookie", "构建的 Cookie: " + sb);
        return sb.toString();
    }

    @NonNull
    public String buildCookieHeader(@NonNull List<String> cookieNames, Cookies defaultCookies) {
        StringBuilder sb = new StringBuilder();
        for (String cookieName : cookieNames) {
            if (sb.length() > 0) sb.append("; ");
            String defaultValue = defaultCookies.getCookies().getOrDefault(cookieName, "");
            String cookieValue = cookies.getOrDefault(cookieName, defaultValue);
            if (Objects.equals(cookieValue, ""))
                Log.w("cookie", "Cookie " + cookieName + " not found");
            sb.append(cookieName).append("=").append(cookieValue);
        }
        Log.d("cookie", "构建的 Cookie: " + sb);
        return sb.toString();
    }

    public String buildCookieHeader(List<String> cookieNames) {
        return buildCookieHeader(cookieNames, new Cookies());
    }

    public void saveCookies(@NonNull Map<String, List<String>> headers) {
        java.util.List<String> cookiesHeader = headers.get("Set-Cookie");
        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                String[] parts = cookie.split(";")[0].split("=");
                if (parts.length >= 2) {
                    cookies.put(parts[0], parts[1]);
                }
            }
        }
        Log.d("cookie", "响应的 Cookies: " + cookies.toString());
    }

    public void clear(){
        cookies.clear();
    }

    public Cookies put(String key, String value){
        cookies.put(key, value);
        return this;
    }
}
