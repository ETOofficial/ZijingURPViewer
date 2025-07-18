package dynastxu.zijingurpviewer.ui.login;

import static dynastxu.zijingurpviewer.network.NetWork.disableSSLCertificateChecking;
import static dynastxu.zijingurpviewer.network.NetWork.encodeToBase64;
import static dynastxu.zijingurpviewer.network.NetWork.tryDecodeResponse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.AccessPath;
import dynastxu.zijingurpviewer.network.Cookies;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData<Bitmap> captchaImage = new MutableLiveData<>();
    private final MutableLiveData<Integer> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> ID = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingUserInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLogging = new MutableLiveData<>();

    private final Cookies cookies = new Cookies();
    private final Cookies defaultCookies = new Cookies();
    private final Random random = new Random();

    public LiveData<Boolean> getIsLoadingUserInfo() {
        return isLoadingUserInfo;
    }

    public LiveData<Bitmap> getCaptchaImage() {
        return captchaImage;
    }

    public LiveData<Integer> getLoginResult() {
        return loginResult;
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getID() {
        return ID;
    }

    public LiveData<Boolean> getIsLogging() {
        return isLogging;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // 获取验证码图片
    public void fetchCaptcha() {
        Log.d("Captcha", "尝试以校内访问获取验证码");
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
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");

                // 处理cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader());
                }

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 保存cookies
                    cookies.saveCookies(connection.getHeaderFields());

                    // 读取图片
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    captchaImage.postValue(bitmap);
                    loginResult.postValue(R.string.empty);
                } else {
                    loginResult.postValue(R.string.get_captcha_failed);
                    Log.e("captcha", "验证码获取失败：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.get_captcha_failed);
                Log.e("captcha", "验证码获取失败：" + e.getMessage());
            }
        }).start();
    }

    public void fetchCaptcha(String route, String VSG_SESSIONID, String JSESSIONID) {
        Log.d("Captcha", "尝试以校外访问获取验证码");
        new Thread(() -> {
            try{
                double randomParam = random.nextDouble();
                String captchaUrl = "https://223.112.21.198:6443/7b68f983/validateCodeAction.do?random=" + randomParam;

                // 创建连接
                URL url = new URL(captchaUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                // 设置请求头
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/");
                connection.setRequestProperty("Connection", "keep-alive");

//                if (!cookies.getCookies().containsKey("JSESSIONID")) Log.w("cookie", "JSESSIONID not found");
                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("route", route)
                        .put("VSG_SESSIONID", VSG_SESSIONID)
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .put("JSESSIONID", JSESSIONID)
                        .buildCookieHeader()
                );

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 保存cookies
                    cookies.saveCookies(connection.getHeaderFields());

                    // 读取图片
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    captchaImage.postValue(bitmap);
//                    loginResult.postValue(R.string.empty);
                } else {
                    loginResult.postValue(R.string.get_captcha_failed);
                    Log.e("captcha", "验证码获取失败：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.get_captcha_failed);
                Log.e("captcha", "验证码获取失败：" + e.getMessage());
            }
        }).start();
    }

    public void fetch() {
        isLoading.postValue(true);
        cookies.clear();
        new Thread(() -> {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/auth_home.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://zj.njust.edu.cn/");
                // 添加cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader());
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());

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
                        loginResult.postValue(R.string.website_maintenance);
                        Log.i("Fetch", "网站维护");
                    } else if (responseString.contains("用户登录")) {
                        loginResult.postValue(R.string.empty);
                        Log.i("Fetch", "已连接");
                    } else {
                        loginResult.postValue(R.string.fetch_failed);
                        Log.e("Fetch", "请求失败响应: " + responseString);
                    }
                } else {
                    loginResult.postValue(R.string.fetch_failed);
                    Log.e("Fetch", "请求失败: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.fetch_failed);
                Log.e("Fetch", "错误: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // 执行登录
    public void performLogin(String account, String password, String captcha) {
        isLogging.postValue(true);
        if (GlobalState.getInstance().getAccessPath() == AccessPath.OnCampus) {
            new Thread(() -> {
                try {
                    URL url = new URL("http://192.168.16.207:9001/loginAction.do");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // 设置请求方法
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    // 设置请求头
                    connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                    connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");
                    connection.setRequestProperty("Origin", "http://192.168.16.207:9001");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    // 添加cookies
                    if (!cookies.getCookies().isEmpty()) {
                        connection.setRequestProperty("Cookie", cookies.buildCookieHeader());
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
                        cookies.saveCookies(connection.getHeaderFields());

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
                            loginResult.postValue(R.string.login_success);
                            GlobalState.getInstance().setLogin(true);
                            Log.i("Login", "登录成功响应: " + responseString);
                        } else {
                            loginResult.postValue(R.string.login_failed);
                            Log.e("Login", "登录失败响应: " + responseString);
                        }
                    } else {
                        loginResult.postValue(R.string.login_failed);
                        Log.e("Login", "登录请求失败: " + connection.getResponseCode());
                    }
                } catch (Exception e) {
                    loginResult.postValue(R.string.login_failed);
                    Log.e("Login", "登录错误: " + e.getMessage());
                }
                isLogging.postValue(false);
            }).start();
        } else if (GlobalState.getInstance().getAccessPath() == AccessPath.OffCampus) {
            String route = GlobalState.getInstance().getRoute();
            String VSG_SESSIONID = GlobalState.getInstance().getVSG_SESSIONID();
            String JSESSIONID = GlobalState.getInstance().getJSESSIONID();
            performLogin(route, VSG_SESSIONID, JSESSIONID, account, password, captcha);
        }
    }

    public void performLogin(String route, String VSG_SESSIONID, String JSESSIONID, String account, String password, String captcha) {
        isLogging.postValue(true);
        new Thread(() -> {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/loginAction.do");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/");
                connection.setRequestProperty("Origin", "https://223.112.21.198:6443");

//                if (!cookies.getCookies().containsKey("JSESSIONID")) Log.w("Cookies", "JSESSIONID 不存在");
                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("route", route)
                        .put("VSG_SESSIONID", VSG_SESSIONID)
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .put("JSESSIONID", JSESSIONID)
                        .buildCookieHeader()
                );

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
                    cookies.saveCookies(connection.getHeaderFields());

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
                        loginResult.postValue(R.string.login_success);
                        GlobalState.getInstance().setLogin(true);
                        parseUserInfo();
                    } else if (responseString.contains("验证码错误")) {
                        loginResult.postValue(R.string.login_captcha_error);
                        Log.e("Login", "验证码错误");
                    } else {
                        loginResult.postValue(R.string.login_failed);
                        Log.e("Login", "登录失败响应: " + responseString);
                    }
                } else {
                    loginResult.postValue(R.string.login_failed);
                    Log.e("Login", "登录请求失败: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "登录错误: " + e.getMessage());
            }
            isLogging.postValue(false);
        }).start();
    }

    public void performLogin(String username, String password) {
        isLogging.postValue(true);
        cookies.clear()
                .put("VSG_VERIFYCODE_CONF", "0-0")
                .put("VSG_CLIENT_RUNNING", "false")
                .put("VSG_LANGUAGE", "zh_CN");
        new Thread(() -> {
            NetWork netWork = new NetWork();
            try {
                if (!netWork.VPNLogin(username, password)) return;
                if (!netWork.fetchURPByVPN()) return;
            } finally {
                isLogging.postValue(false);
            }
            isLogging.postValue(false);
        }).start();
    }

    public void performLoginWithCookies(String route, String VSG_SESSIONID) {
        isLogging.postValue(true);
        new Thread(() -> {
            try {
                NetWork netWork = new NetWork();
                netWork.fetchURPByVPN(route, VSG_SESSIONID);

            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "登录错误: " + e.getMessage());
            }
            isLogging.postValue(false);
        }).start();
    }

    private void parseUserInfo() {
        isLoadingUserInfo.postValue(true);
        AccessPath accessPath = GlobalState.getInstance().getAccessPath();
        if (accessPath == AccessPath.OnCampus) {
            // TODO 校内线路
        } else if (accessPath == AccessPath.OffCampus) {
            new Thread(() -> {
                NetWork netWork = new NetWork();
                try {
                    if (!netWork.parseUserInfo()) return;
                } finally {
                    isLoadingUserInfo.postValue(false);
                }
            }).start();
        }
    }

    private class NetWork {
        private boolean successFetchURPByVPN = false;
        private boolean successFetchCampusPage = false;

        public boolean isSuccessFetchURPByVPN() {
            return successFetchURPByVPN;
        }

        public boolean isSuccessFetchCampusPage(){
            return successFetchCampusPage;
        }

        public void fetchCampusPage() {
            try {
                URL url = new URL("https://zj.njust.edu.cn/jwc/8041/list.psp");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());

                    StringBuilder response = getResponse(connection);

                    String responseString = response.toString();

                    if (responseString.contains("URP教务系统")) {
                        cookies.saveCookies(connection.getHeaderFields());
                        loginResult.postValue(R.string.empty);
                        successFetchCampusPage = true;
                        Log.i("Campus", "获取学校网页页面成功");
                    } else {
                        loginResult.postValue(R.string.fetch_failed);
                        Log.e("Campus", "获取学校网页页面错误: " + responseString);
                    }
                } else {
                    loginResult.postValue(R.string.fetch_failed);
                    Log.e("Campus", "获取学校网页页面错误: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.fetch_failed);
                Log.e("Campus", "获取学校网页页面错误: " + e.getMessage());
            }
        }

        @NonNull
        private StringBuilder getResponse(@NonNull HttpURLConnection connection) throws IOException {
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
            return response;
        }

        public boolean VPNReLogin(){
            try{
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/auth_home.html?relogin=true");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/vpn/theme/portal_home.html");
                connection.setRequestProperty("Cookie", new Cookies().put("VSG_VERIFYCODE_CONF", "0-0").buildCookieHeader());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return true;
                } else {
                    loginResult.postValue(R.string.login_failed);
                    Log.e("Login", "VPN 登录错误：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "VPN 登录错误：" + e.getMessage());
            }
            return false;
        }

        public boolean VPNLogin(String account, String password) {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/user/auth/password");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/vpn/theme/auth_home.html");
                connection.setRequestProperty("Origin", "https://223.112.21.198:6443");

                // 添加cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader());
                } else Log.w("Cookies", "cookies 为空");

                String formData = "username=" + encodeToBase64(account) + "&password=" + encodeToBase64(password) + "&encode=1&rmbpwd_browser=0";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = formData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());
                    if (!cookies.getCookies().containsKey("VSG_SESSIONID")) {
                        Log.w("Login", "cookies 中缺少 VSG_SESSIONID");
                    } else GlobalState.getInstance().setVSG_SESSIONID(cookies.get("VSG_SESSIONID"));

                    String response = getResponse(connection).toString();
                    if (response.contains("用户名或密码错误")) {
                        loginResult.postValue(R.string.wrong_password_or_username);
                        return false;
                    } else {
                        loginResult.postValue(R.string.empty);
                        return true;
                    }
                } else {
                    loginResult.postValue(R.string.login_failed);
                    Log.e("Login", "VPN 登录错误：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "VPN 登录错误：" + e.getMessage());
            }
            return false;
        }

        public void fetchVPNLoggedPage() {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/portal_home.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/vpn/theme/auth_home.html");

                // 添加cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader(List.of("VSG_VERIFYCODE_CONF", "VSG_CLIENT_RUNNING", "VSG_LANGUAGE", "VSG_SESSIONID"), defaultCookies));
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());

                    StringBuilder response = getResponse(connection);

                    String responseString = response.toString();

                    if (responseString.contains("欢迎访问安全网关")) {
                        loginResult.postValue(R.string.login_vpn_success);
                        Log.i("Login", "VPN登录成功");
                        GlobalState.getInstance().setLoginVPN(true);
                        fetchCaptcha();
                    } else {
                        loginResult.postValue(R.string.login_failed);
                        Log.e("Login", "VPN登录失败响应: " + responseString);
                    }
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "VPN登录错误: " + e.getMessage());
            }
        }

        public void fetchURPByVPN(String route, String VSG_SESSIONID) {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Host", "223.112.21.198:6443");

                if (!cookies.getCookies().containsKey("JSESSIONID")) Log.w("Cookies", "JSESSIONID 不存在");
                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("route", route)
                        .put("VSG_SESSIONID", VSG_SESSIONID)
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .buildCookieHeader()
                        + ";" + cookies.buildCookieHeader(List.of("JSESSIONID"))
                );

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {// 保存cookies
                    cookies.saveCookies(connection.getHeaderFields());

                    byte[] responseBytes = getResponseBytes(connection);

                    // 尝试解码响应
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("URP综合教务系统")) {
                        loginResult.postValue(R.string.login_vpn_success);
                        successFetchURPByVPN = true;
                        Log.i("Login", "成功通过VPN获取URP教务系统页面");
                        GlobalState.getInstance().setLoginVPN(true);
                    } else {
                        loginResult.postValue(R.string.fetch_failed);
                        Log.e("Captcha", "无法访问URP教务系统页面: " + responseString);
                    }
                } else {
                    loginResult.postValue(R.string.fetch_failed);
                    Log.e("Captcha", "无法访问URP教务系统页面: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.fetch_failed);
                Log.e("Captcha", "无法访问URP教务系统页面: " + e.getMessage());
            }
        }

        public boolean fetchURPByVPN() {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Host", "223.112.21.198:6443");

                if (!cookies.getCookies().containsKey("JSESSIONID")) Log.w("Cookies", "JSESSIONID 不存在");
                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("VSG_SESSIONID", GlobalState.getInstance().getVSG_SESSIONID())
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .buildCookieHeader()
                        + ";" + cookies.buildCookieHeader(List.of("JSESSIONID"))
                );

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {// 保存cookies
                    cookies.saveCookies(connection.getHeaderFields());
                    if (!cookies.containsKey("JSESSIONID")) {
                        Log.w("Cookies", "JSESSIONID 不存在");
                    } else {
                        GlobalState.getInstance().setJSESSIONID(cookies.get("JSESSIONID"));
                    }
                    if (!cookies.containsKey("route")) {
                        Log.w("Cookies", "route 不存在");
                    } else {
                        GlobalState.getInstance().setRoute(cookies.get("route"));
                    }

                    byte[] responseBytes = getResponseBytes(connection);

                    // 尝试解码响应
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("URP综合教务系统")) {
                        loginResult.postValue(R.string.login_vpn_success);
                        successFetchURPByVPN = true;
                        Log.i("Login", "成功通过VPN获取URP教务系统页面");
                        GlobalState.getInstance().setLoginVPN(true);
                        return true;
                    } else {
                        loginResult.postValue(R.string.fetch_failed);
                        Log.e("Captcha", "无法访问URP教务系统页面: " + responseString);
                    }
                } else {
                    loginResult.postValue(R.string.fetch_failed);
                    Log.e("Captcha", "无法访问URP教务系统页面: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.fetch_failed);
                Log.e("Captcha", "无法访问URP教务系统页面: " + e.getMessage());
            }
            return false;
        }

        public void fetchURPCaptchaByVPN() {
            try {

                double randomParam = random.nextDouble();
                String captchaUrl = "https://223.112.21.198:6443/7b68f983/validateCodeAction.do?random=" + randomParam;

                URL url = new URL(captchaUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/");

                defaultCookies.put("mapid", "7b68f983");
                // 处理cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader(List.of("VSG_VERIFYCODE_CONF", "VSG_CLIENT_RUNNING", "VSG_LANGUAGE", "VSG_SESSIONID", "mapid", "route"), defaultCookies));
                } else Log.w("Captcha", "cookies 为空");

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 保存cookies
                    cookies.saveCookies(connection.getHeaderFields());

                    // 读取图片
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap == null) {
                        Log.e("captcha", "验证码获取失败：图片为空");
                        loginResult.postValue(R.string.get_captcha_failed);
                    } else {
                        Log.d("captcha", "验证码获取成功");
                        captchaImage.postValue(bitmap);
                        loginResult.postValue(R.string.empty);
                    }
                } else {
                    loginResult.postValue(R.string.get_captcha_failed);
                    Log.e("captcha", "验证码获取失败：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.get_captcha_failed);
                Log.e("captcha", "验证码获取失败：" + e.getMessage());
            }
        }

        public boolean parseUserInfo(){
            Log.d("username", "开始解析用户名");
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/menu/top.jsp");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/loginAction.do");

                connection.setRequestProperty("Cookie", new Cookies()
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .put("JSESSIONID", GlobalState.getInstance().getJSESSIONID())
                        .put("VSG_SESSIONID", GlobalState.getInstance().getVSG_SESSIONID())
                        .put("route", GlobalState.getInstance().getRoute())
                        .put("mapid", "7b68f983")
                        .buildCookieHeader()
                );

                // 获取响应
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());

                    byte[] responseBytes = getResponseBytes(connection);

                    // 尝试解码响应
                    String responseString = tryDecodeResponse(responseBytes);

                    // 检查登录结果
                    if (responseString.contains("当前用户")) {
                        // 正则表达式匹配模式
                        String pattern = "当前用户:(\\d+)\\(([\\u4e00-\\u9fa5]+)\\)";
                        Pattern r = Pattern.compile(pattern);
                        Matcher m = r.matcher(responseString);

                        if (m.find()) {
                            String studentId = m.group(1);  // 提取学号
                            String studentName = m.group(2); // 提取姓名
                            GlobalState.getInstance().setID(studentId);
                            GlobalState.getInstance().setUsername(studentName);
                            Log.i("ParseUsername", "学号: " + studentId);
                            Log.i("ParseUsername", "姓名: " + studentName);
                            ID.postValue(studentId);
                            username.postValue(studentName);
                            return true;
                        } else {
                            Log.e("ParseUsername", "匹配学号和姓名失败");
                        }
                    } else {
                        Log.e("ParseUsername", "请求失败");
                    }
                } else {
                    Log.e("ParseUsername", "GET请求失败: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("ParseUsername", "获取用户名错误: " + e.getMessage());
            }
            return false;
        }
        @NonNull
        private byte[] getResponseBytes(@NonNull HttpURLConnection connection) throws IOException {
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
            return byteArrayOutputStream.toByteArray();
        }
    }
}
