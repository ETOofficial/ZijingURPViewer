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
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.Cookies;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData<Bitmap> captchaImage = new MutableLiveData<>();
    private final MutableLiveData<Integer> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLogging = new MutableLiveData<>();

    private final Cookies cookies = new Cookies();
    private final Cookies defaultCookies = new Cookies();
    private final Random random = new Random();

    public LiveData<Bitmap> getCaptchaImage() {
        return captchaImage;
    }

    public LiveData<Integer> getLoginResult() {
        return loginResult;
    }

    public LiveData<String> getUsername() {
        return username;
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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
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

    public void fetchCaptcha(String route, String VSG_SESSIONID) {
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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");

                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("route", route)
                        .put("VSG_SESSIONID", VSG_SESSIONID)
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .buildCookieHeader());

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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
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
        }).start();
    }

    public void performLogin(String route, String VSG_SESSIONID) {
        new Thread(() -> {
            try {
                NetWork netWork = new NetWork();
                netWork.fetchURPByVPN(route, VSG_SESSIONID);

            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "登录错误: " + e.getMessage());
            }
        }).start();
    }

    private void parseUsername() {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.16.207:9001/menu/top.jsp");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "http://192.168.16.207:9001/loginAction.do");

                // 添加cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader());
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
                        Log.i("ParseUsername", "响应: " + responseString);
                    } else {
                    }
                } else {
                    Log.e("ParseUsername", "GET请求失败: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("ParseUsername", "获取用户名错误: " + e.getMessage());
            }
        }).start();
    }

    public class NetWork {
        private boolean successFetchWithVPNUserInfo = false;
        private boolean successFetchURPByVPN = false;
        private boolean successFetchCampusPage = false;

        public boolean isSuccessFetchURPByVPN() {
            return successFetchURPByVPN;
        }

        public boolean isSuccessFetchWithVPNUserInfo() {
            return successFetchWithVPNUserInfo;
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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());

                    StringBuilder response = getResponse(connection);

                    String responseString = response.toString();

                    if (responseString.contains("URP教务系统")) {
                        cookies.saveCookies(connection.getHeaderFields());
                        loginResult.postValue(R.string.empty);
                        successFetchCampusPage = true;
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

        public void VPNLogin(String account, String password) {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/user/auth/password");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/vpn/theme/auth_home.html");
                connection.setRequestProperty("Origin", "https://223.112.21.198:6443");

                defaultCookies.put("VSG_CLIENT_RUNNING", "false");
                defaultCookies.put("VSG_LANGUAGE", "zh_CN");
                // 添加cookies
                if (!cookies.getCookies().isEmpty()) {
                    connection.setRequestProperty("Cookie", cookies.buildCookieHeader(List.of("VSG_VERIFYCODE_CONF", "VSG_CLIENT_RUNNING", "VSG_LANGUAGE", "VSG_SESSIONID"), defaultCookies));
                } else Log.w("Cookies", "cookies 为空");

                String formData = "username=" + encodeToBase64(account) + "&password=" + encodeToBase64(password) + "&encode=1&rmbpwd_browser=0";

                try (OutputStream ignored = connection.getOutputStream()) {
                    formData.getBytes(StandardCharsets.UTF_8);
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    cookies.saveCookies(connection.getHeaderFields());
                    successFetchWithVPNUserInfo = true;
                    loginResult.postValue(R.string.empty);
                } else {
                    loginResult.postValue(R.string.login_failed);
                    Log.e("Login", "VPN 登录错误：" + connection.getResponseCode());
                }
            } catch (Exception e) {
                loginResult.postValue(R.string.login_failed);
                Log.e("Login", "VPN 登录错误：" + e.getMessage());
            }
        }

        public void fetchVPNLoggedPage() {
            try {
                URL url = new URL("https://223.112.21.198:6443/vpn/theme/portal_home.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
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

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Host", "223.112.21.198:6443");

                connection.setRequestProperty("Cookie", new Cookies()
                        .put("mapid", "7b68f983")
                        .put("route", route)
                        .put("VSG_SESSIONID", VSG_SESSIONID)
                        .put("VSG_VERIFYCODE_CONF", "0-0")
                        .put("VSG_CLIENT_RUNNING", "false")
                        .put("VSG_LANGUAGE", "zh_CN")
                        .buildCookieHeader());

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

        public void fetchURPCaptchaByVPN() {
            try {

                double randomParam = random.nextDouble();
                String captchaUrl = "https://223.112.21.198:6443/7b68f983/validateCodeAction.do?random=" + randomParam;

                URL url = new URL(captchaUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
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
    }
}
