package dynastxu.zijingurpviewer.network;

import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetWork {
    public static void disableSSLCertificateChecking(HttpURLConnection connection) {
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

    // 尝试多种解码方式
    @NonNull
    @Contract("_ -> new")
    public static String tryDecodeResponse(byte[] responseBytes) {
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

    @NonNull
    public static String encodeToBase64(@NonNull String input) {
        byte[] bytes = input.getBytes();
        StringBuilder encoded = new StringBuilder(Base64.getEncoder().encodeToString(bytes));
        while (encoded.length() % 4 != 0) {
            encoded.append("=");
        }
        return encoded.toString();
    }

    @NonNull
    public static byte[] getResponseBytes(@NonNull HttpURLConnection connection) throws IOException {
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
