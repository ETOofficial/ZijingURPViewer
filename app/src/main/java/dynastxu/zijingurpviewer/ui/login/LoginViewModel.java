package dynastxu.zijingurpviewer.ui.login;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dynastxu.zijingurpviewer.network.AccessPath;

public class LoginViewModel extends ViewModel {
    private static boolean login = false;
    private AccessPath accessPath;

    private final MutableLiveData<Bitmap> captchaImage = new MutableLiveData<>();
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private final Map<String, String> cookies = new HashMap<>();
    private final Random random = new Random();

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public static boolean isLogin() {
        return login;
    }

    public static void setLogin(boolean login) {
        LoginViewModel.login = login;
    }

    public AccessPath getAccessPath() {
        return accessPath;
    }

    public void setAccessPath(AccessPath accessPath) {
        this.accessPath = accessPath;
    }

    public LiveData<Bitmap> getCaptchaImage() {
        return captchaImage;
    }

    public LiveData<String> getLoginResult() {
        return loginResult;
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username.postValue(username);
    }

    // 获取验证码图片
    public void fetchCaptcha() {
        cookies.clear();
        new Thread(() -> {
            try {
                // 生成随机参数
                double randomParam = random.nextDouble();
                String captchaUrl = "http://192.168.16.207:9001/validateCodeAction.do?random=" + randomParam;

                // 创建连接
                URL url = new URL(captchaUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求头
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");

                // 处理cookies
                if (!cookies.isEmpty()) {
                    connection.setRequestProperty("Cookie", buildCookieHeader());
                }

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 保存cookies
                    saveCookies(connection.getHeaderFields());

                    // 读取图片
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    captchaImage.postValue(bitmap);
                    loginResult.postValue("");
                } else {
                    loginResult.postValue("验证码获取失败" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue("验证码获取失败" + e.getMessage());
            }
        }).start();
    }

    // 构建cookie请求头
    private String buildCookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public void fetch(){
        isLoading.postValue(true);
        cookies.clear();
        new Thread(() -> {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/auth_home.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "https://zj.njust.edu.cn/");
                // 添加cookies
                if (!cookies.isEmpty()) {
                    connection.setRequestProperty("Cookie", buildCookieHeader());
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    saveCookies(connection.getHeaderFields());

                    InputStream inputStream = connection.getInputStream();

                    if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                        inputStream = new GZIPInputStream(inputStream);
                    }

                    // 读取响应内容
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseString = response.toString();

                    if (responseString.contains("网站维护")) {
                        loginResult.postValue("网站维护");
                        Log.println(Log.INFO, "Fetch", "网站维护");
                    } else {
                        loginResult.postValue("请求失败");
                        Log.println(Log.ERROR, "Fetch", "请求失败响应: " + responseString);
                    }
                } else {
                    loginResult.postValue("请求失败: " + connection.getResponseCode());
                    Log.println(Log.ERROR, "Fetch", "请求失败: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue("错误: " + e.getMessage());
                Log.println(Log.ERROR, "Fetch", "错误: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // 保存 cookies
    private void saveCookies(Map<String, java.util.List<String>> headers) {
        java.util.List<String> cookiesHeader = headers.get("Set-Cookie");
        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                String[] parts = cookie.split(";")[0].split("=");
                if (parts.length >= 2) {
                    cookies.put(parts[0], parts[1]);
                }
            }
        }
    }

    // 执行登录
    public void performLogin(String account, String password, String captcha) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.16.207:9001/loginAction.do");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                // 设置请求头
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");
                connection.setRequestProperty("Origin", "http://192.168.16.207:9001");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // 添加cookies
                if (!cookies.isEmpty()) {
                    connection.setRequestProperty("Cookie", buildCookieHeader());
                }

                // 构建表单数据
                String formData = "zjh1=&tips=&lx=&evalue=&eflag=&fs=&dzslh=&zjh=" +
                        account + "&mm=" + password + "&v_yzm=" + captcha;

                // 发送请求
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = formData.getBytes("GBK");
                    os.write(input, 0, input.length);
                }

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 保存cookies
                    saveCookies(connection.getHeaderFields());

                    InputStream inputStream = connection.getInputStream();

                    if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                        inputStream = new GZIPInputStream(inputStream);
                    }

                    // 完整读取字节数据
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byte[] responseBytes = byteArrayOutputStream.toByteArray();

                    // 尝试解码响应
                    String responseString = tryDecodeResponse(responseBytes);

                    // 检查登录结果（根据实际响应内容调整）
                    if (responseString.contains("学分制综合教务")) {
                        loginResult.postValue("登陆成功");
                        setLogin(true);
                        Log.println(Log.INFO, "Login", "登录成功响应: " + responseString);
                    } else {
                        loginResult.postValue("登陆失败");
                        Log.println(Log.ERROR, "Login", "登录失败响应: " + responseString);
                    }
                } else {
                    loginResult.postValue("登录请求失败: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue("登录错误: " + e.getMessage());
            }
        }).start();
    }

    public void performLogin(String account, String password) {
        new Thread(() -> {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/auth_home.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/vpn/theme/auth_home.html");
                connection.setRequestProperty("Origin", "https://223.112.21.198:6443");
            } catch (Exception e) {
                loginResult.postValue("登录错误: " + e.getMessage());
            }
        }).start();
    }

    // 尝试多种解码方式
    private String tryDecodeResponse(byte[] responseBytes) {
        // 尝试1: 使用 GB2312
        try {
            return new String(responseBytes, "GB2312");
        } catch (UnsupportedEncodingException e) {
            Log.w("Encoding", "GB2312 编码不可用");
        }

        // 尝试2: 使用 GBK（GBK 是 GB2312 的扩展）
        try {
            return new String(responseBytes, "GBK");
        } catch (UnsupportedEncodingException e) {
            Log.w("Encoding", "GBK 编码不可用");
        }

        // 尝试3: 使用 UTF-8
        try {
            return new String(responseBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.w("Encoding", "UTF-8 编码不可用");
        }

        // 尝试4: 使用 ISO-8859-1
        return new String(responseBytes, StandardCharsets.ISO_8859_1);
    }

    private void parseUsername(){
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.16.207:9001/menu/top.jsp");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");

                // 添加cookies
                if (!cookies.isEmpty()) {
                    connection.setRequestProperty("Cookie", buildCookieHeader());
                }

                // 获取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    InputStream inputStream = connection.getInputStream();

                    if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                        inputStream = new GZIPInputStream(inputStream);
                    }

                    // 完整读取字节数据
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byte[] responseBytes = byteArrayOutputStream.toByteArray();

                    // 尝试解码响应
                    String responseString = tryDecodeResponse(responseBytes);

                    // 检查登录结果（根据实际响应内容调整）
                    if (responseString.contains("当前用户")) {
                        loginResult.postValue("登录成功");
                        Log.println(Log.INFO, "Login", "登录成功响应: " + responseString);
                    } else {
                        loginResult.postValue("登录失败，请检查凭证");
                    }
                } else {
                    Log.e("HTTP", "GET请求失败: " + responseCode);
                }

            }  catch (Exception e) {
                Log.println(Log.ERROR, "Login", "获取用户名错误: " + e.getMessage());
            }
        }).start();
    }

    public static String encodeToBase64(String input) {
        byte[] bytes = input.getBytes();
        StringBuilder encoded = new StringBuilder(Base64.getEncoder().encodeToString(bytes));
        while (encoded.length() % 4 != 0) {
            encoded.append("=");
        }
        return encoded.toString();
    }

    // 在 LoginViewModel 类中添加这个方法
    private void disableSSLCertificateChecking(HttpURLConnection connection) {
        if (connection != null) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            try {
                // 创建信任所有证书的TrustManager
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // 配置SSLContext
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                httpsConnection.setSSLSocketFactory(sc.getSocketFactory());

                // 忽略主机名验证
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                Log.e("SSL", "禁用证书验证失败", e);
            }
        }
    }

}
