package dynastxu.zijingurpviewer.ui.grades;

import static dynastxu.zijingurpviewer.network.NetWork.disableSSLCertificateChecking;
import static dynastxu.zijingurpviewer.network.NetWork.getResponseBytes;
import static dynastxu.zijingurpviewer.network.NetWork.tryDecodeResponse;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.AccessPath;
import dynastxu.zijingurpviewer.network.StandardVPNCookies;

public class GradesFailViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Integer> loadResult = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> failGrades = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<Integer> getLoadResult() {
        return loadResult;
    }

    public MutableLiveData<List<Map<String, Object>>> getFailGrades() {
        return failGrades;
    }

    public void fetchFailGrades(){
        isLoading.postValue(true);
        if (GlobalState.getInstance().getAccessPath() == AccessPath.OnCampus) {
            // TODO: 校内访问
        } else {
            new Thread(() -> {
                NetWork netWork = new NetWork();
                try {
                    if (!netWork.fetchFailGradesOffCampus()) return;
                } finally {
                    isLoading.postValue(false);
                }
            }).start();
        }
    }

    class NetWork {
        public boolean fetchFailGradesOffCampus() {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/gradeLnAllAction.do?type=ln&oper=bjg");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Cookie", new StandardVPNCookies().buildCookieHeader());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte[] responseBytes = getResponseBytes(connection);
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("历年成绩")) {
                        List<Map<String, Object>> semesterData = parseGradeData(responseString);
                        failGrades.postValue(semesterData);

                        Log.d("GradesFail", "获取失败成绩成功\n" + GradesString(semesterData));
                        return true;
                    } else if (responseString.contains("教学评估")) {
                        Log.i("GradesFail", "未完成教学评估，无法查看成绩");
                        loadResult.postValue(R.string.teachingAssessment);
                    } else {
                        Log.e("GradesFail", "获取成绩列表错误: " + responseString);
                        loadResult.postValue(R.string.getFailGradesFailed);
                    }
                } else {
                    Log.e("GradesFail", "获取成绩列表错误: " + connection.getResponseCode());
                    loadResult.postValue(R.string.getFailGradesFailed);
                }
            } catch (Exception e) {
                Log.e("GradesFail", "获取不及格成绩错误: " + e.getMessage());
                loadResult.postValue(R.string.getFailGradesFailed);
            }
            return false;
        }
    }

    @NonNull
    private static List<Map<String, Object>> parseGradeData(String html) {
        List<Map<String, Object>> result = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        // 1. 解析"尚不及格"部分
        Map<String, Object> currentFailed = parseFailedSection(doc, "尚不及格", "未通过课程学分");
        if (!currentFailed.isEmpty()) {
            result.add(currentFailed);
        }

        // 2. 解析"曾不及格"部分
        Map<String, Object> historyFailed = parseFailedSection(doc, "曾不及格", "曾经未通过课程学分");
        if (!historyFailed.isEmpty()) {
            result.add(historyFailed);
        }

        return result;
    }

    @NonNull
    private static Map<String, Object> parseFailedSection(@NonNull Document doc, String sectionTitle, String summaryKeyword) {
        Map<String, Object> sectionData = new HashMap<>();
        sectionData.put("title", sectionTitle);

        // 1. 定位标题元素
        Element titleElement = null;
        Elements titleElements = doc.select("table.title:contains(" + sectionTitle + ")");
        if (!titleElements.isEmpty()) {
            titleElement = titleElements.first();
        }

        if (titleElement == null) {
            return sectionData; // 返回空Map
        }

        // 2. 查找课程表格
        Element nextSibling = titleElement.nextElementSibling();
        while (nextSibling != null) {
            if ("table".equals(nextSibling.tagName()) &&
                    nextSibling.hasClass("titleTop2")) {
                // 在titleTop2表格中查找displayTag表格
                Element courseTable = nextSibling.selectFirst("table.displayTag");
                if (courseTable != null) {
                    // 解析课程数据
                    List<Map<String, String>> courses = parseCourseTable(courseTable);
                    sectionData.put("courses", courses);
                }
                break;
            }
            nextSibling = nextSibling.nextElementSibling();
        }

        // 3. 查找汇总信息
        if (nextSibling != null) {
            Element summaryTable = nextSibling.nextElementSibling();
            while (summaryTable != null) {
                if ("table".equals(summaryTable.tagName())) {
                    Element summaryTd = summaryTable.selectFirst("td:contains(" + summaryKeyword + ")");
                    if (summaryTd != null) {
                        sectionData.put("credit_info", summaryTd.text());
                        break;
                    }
                }
                summaryTable = summaryTable.nextElementSibling();
            }
        }

        return sectionData;
    }

    @NonNull
    private static List<Map<String, String>> parseCourseTable(@NonNull Element table) {
        List<Map<String, String>> courses = new ArrayList<>();

        // 获取表头
        List<String> headers = new ArrayList<>();
        Elements headerElements = table.select("thead th");
        for (Element header : headerElements) {
            headers.add(header.text());
        }

        // 获取课程行
        Elements rows = table.select("tbody tr.odd");
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < headers.size()) continue;

            Map<String, String> course = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                String value = cells.get(i).text();

                // 处理特殊格式的成绩字段
                if (header.equals("成绩") && value.contains("&nbsp;")) {
                    value = value.replace("&nbsp;", "").trim();
                }

                course.put(header, value);
            }
            courses.add(course);
        }

        return courses;
    }

    @NonNull
    private static String GradesString(@NonNull List<Map<String, Object>> data) {
        StringBuilder str = new StringBuilder();
        for (Map<String, Object> semester : data) {
            str.append("学期标题: ").append(semester.get("title")).append("\n");
            str.append("学分情况: ").append(semester.get("credit_info")).append("\n");

            List<Map<String, String>> courses = (List<Map<String, String>>) semester.get("courses");

            str.append("课程列表:\n");
            assert courses != null;
            for (Map<String, String> course : courses) {
                str.append("\t课程名: ").append(course.get("课程名")).append("\n");
                str.append("\t课程号: ").append(course.get("课程号")).append("\n");
                str.append("\t课序号: ").append(course.get("课序号")).append("\n");
                str.append("\t学分: ").append(course.get("学分")).append("\n");
                str.append("\t成绩: ").append(course.get("成绩")).append("\n");
                str.append("\t课程属性: ").append(course.get("课程属性")).append("\n");
                str.append("\t------------------").append("\n");
            }
            str.append("==============================================").append("\n");
        }
        return str.toString();
    }
}
