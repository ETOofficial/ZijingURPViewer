package dynastxu.zijingurpviewer.ui.exam;

import static dynastxu.zijingurpviewer.network.NetWork.disableSSLCertificateChecking;
import static dynastxu.zijingurpviewer.network.NetWork.tryDecodeResponse;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.StandardVPNCookies;

public class ExamViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Integer> loadResult = new MutableLiveData<>();
    private final MutableLiveData<List<List<String>>> examData = new MutableLiveData<>();

    public MutableLiveData<Integer> getLoadResult() {
        return loadResult;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<List<List<String>>> getExamData() {
        return examData;
    }

    public void fetchExamData() {
        isLoading.postValue(true);
        new Thread(() -> {
            Log.d("Exam", "尝试获取考试数据");
            NetWork netWork = new NetWork();
            netWork.fetchExamData();
            isLoading.postValue(false);
        }).start();
    }

    private List<List<String>> extractExamTable(String html) {
        List<List<String>> result = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(html);

            // 定位考试安排表格（第二个class="displayTag"的表格）
            Elements tables = doc.select("table.displayTag");
            if (tables.size() < 2) {
                return result;
            }

            Element examTable = tables.get(1); // 第二个表格是考试安排表

            // 提取所有数据行（跳过表头）
            for (Element row : examTable.select("tr")) {
                // 跳过表头行（包含<th>元素）
                if (!row.select("th").isEmpty()) {
                    continue;
                }

                List<String> rowData = new ArrayList<>();
                for (Element cell : row.select("td")) {
                    rowData.add(cell.text().trim());
                }

                // 只添加有数据的行（10列）
                if (rowData.size() == 10) {
                    result.add(rowData);
                }
            }

        } catch (Exception e) {
            Log.e("Exam", "解析考试数据错误: " + e.getMessage());
            loadResult.postValue(R.string.extractExamDataFailed);
        }

        return result;
    }

    private class NetWork{
        public boolean fetchExamData() {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/ksApCxAction.do?oper=getKsapXx&yzxh=" + GlobalState.getInstance().getID());
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/menu/menu.jsp");

                connection.setRequestProperty("Cookie", new StandardVPNCookies().buildCookieHeader());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte[] responseBytes = getResponseBytes(connection);
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("考试安排信息查询")) {
                        examData.postValue(extractExamTable(responseString));
                        loadResult.postValue(R.string.getExamDataSuccess);
                        return true;
                    } else {
                        Log.e("Exam", "获取考试数据错误: " + responseString);
                        loadResult.postValue(R.string.getExamDataFailed);
                    }
                } else {
                    Log.e("Exam", "获取考试数据错误: " + connection.getResponseCode());
                    loadResult.postValue(R.string.getExamDataFailed);
                }
            } catch (Exception e) {
                Log.e("Exam", "获取考试数据错误: " + e.getMessage());
                loadResult.postValue(R.string.getExamDataFailed);
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
