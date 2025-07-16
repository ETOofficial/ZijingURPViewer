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

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.AccessPath;
import dynastxu.zijingurpviewer.network.StandardVPNCookies;

public class GradesAllViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Integer> loadResult = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> allGrades = new MutableLiveData<>();

    public MutableLiveData<Integer> getLoadResult() {
        return loadResult;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<List<Map<String, Object>>> getAllGrades() {
        return allGrades;
    }

    public void fetchAllGrades() {
        isLoading.postValue(true);
        if (GlobalState.getInstance().getAccessPath() == AccessPath.OnCampus) {

        } else {
            new Thread(() -> {
                NetWork netWork = new NetWork();
                try {
                    if (!netWork.fetchGradesCheckList()) return;
                    if (!netWork.fetchAllGrades()) return;
                } finally {
                    isLoading.postValue(false);
                }
            }).start();
        }
    }

    private class NetWork {
        private String latestSemester = "";

        public boolean fetchGradesCheckList() {
            try {
                URL url = new URL("https://223.112.21.198:6443/7b68f983/gradeLnAllAction.do?type=ln&oper=qbcj");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/gradeLnAllAction.do?type=ln&oper=qb&yzxh=" + GlobalState.getInstance().getID());
                connection.setRequestProperty("Cookie", new StandardVPNCookies().buildCookieHeader());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte[] responseBytes = getResponseBytes(connection);
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("历年成绩")) {
                        // 正则表达式匹配学期字符串模式
                        Pattern pattern = Pattern.compile("(\\d{4}-\\d{4}学年[秋冬春夏]\\(.学期\\))");
                        Matcher matcher = pattern.matcher(responseString);

                        // 找到最后一个匹配的学期（最新学期）
                        while (matcher.find()) {
                            latestSemester = matcher.group(1);
                        }

                        Log.d("GradesAll", "最新学期: " + latestSemester);
                        return true;
                    } else if (responseString.contains("教学评估")) {
                        Log.i("GradesAll", "未完成教学评估，无法查看成绩");
                        loadResult.postValue(R.string.teachingAssessment);
                    } else {
                        Log.e("GradesAll", "获取成绩列表错误: " + responseString);
                        loadResult.postValue(R.string.getGradesCheckListFailed);
                    }
                } else {
                    Log.e("GradesAll", "获取成绩列表错误: " + connection.getResponseCode());
                    loadResult.postValue(R.string.getGradesCheckListFailed);
                }
            } catch (Exception e) {
                Log.e("GradesAll", "获取成绩列表错误: " + e.getMessage());
                loadResult.postValue(R.string.getGradesCheckListFailed);
            }
            return false;
        }

        public boolean fetchAllGrades() {
            try {
                String encodedSemester = URLEncoder.encode(latestSemester, "UTF-8");
                URL url = new URL("https://223.112.21.198:6443/7b68f983/gradeLnAllAction.do?type=ln&oper=qbcjinfo&lnxndm=" + encodedSemester);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                disableSSLCertificateChecking(connection);

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("User-Agent", GlobalState.UserAgent);
                connection.setRequestProperty("Referer", "https://223.112.21.198:6443/7b68f983/gradeLnAllAction.do?type=ln&oper=qbcj");
                connection.setRequestProperty("Cookie", new StandardVPNCookies().buildCookieHeader());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    byte[] responseBytes = getResponseBytes(connection);
                    String responseString = tryDecodeResponse(responseBytes);

                    if (responseString.contains("课程号")) {
                        List<Map<String, Object>> semesterData = parseGradeData(responseString);
                        allGrades.postValue(semesterData);

                        Log.d("GradesAll", "获取所有成绩成功\n" + GradesString(semesterData));
                        return true;
                    } else {
                        Log.e("GradesAll", "获取所有成绩错误: " + responseString);
                        loadResult.postValue(R.string.getAllGradesFailed);
                    }
                } else {
                    Log.e("GradesAll", "获取所有成绩错误: " + connection.getResponseCode());
                    loadResult.postValue(R.string.getAllGradesFailed);
                }
            } catch (UnsupportedEncodingException e) {
                Log.e("GradesAll", "参数编码错误: " + e.getMessage());
                loadResult.postValue(R.string.getAllGradesFailed);
            } catch (Exception e) {
                Log.e("GradesAll", "获取所有成绩错误: " + e.getMessage());
                loadResult.postValue(R.string.getAllGradesFailed);
            }
            return false;
        }
    }
    @NonNull
    private static List<Map<String, Object>> parseGradeData(String html) {
        List<Map<String, Object>> result = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        // 1. 找到所有学期标题
        Elements semesterAnchors = doc.select("a[name]");

        for (Element anchor : semesterAnchors) {
            String semesterTitle = anchor.attr("name");
            if (!semesterTitle.contains("学年")) continue; // 过滤非学期标题

            Map<String, Object> semesterMap = new HashMap<>();
            semesterMap.put("title", semesterTitle);

            // 2. 找到学期标题后面的课程表格
            Element courseTable = anchor.nextElementSibling();
            while (courseTable != null && (!courseTable.tagName().equals("table") || !courseTable.hasClass("titleTop2"))) {
                courseTable = courseTable.nextElementSibling();
            }

            if (courseTable == null) continue;

            // 3. 解析课程数据
            List<Map<String, String>> courses = parseCourseTable(courseTable);
            semesterMap.put("courses", courses);

            // 4. 找到学分信息表格
            Element creditTable = courseTable;
            Element creditTd = creditTable.select("td[height=\"21\"]").first();
            assert creditTd != null;
            semesterMap.put("credit_info", creditTd.text());
            result.add(semesterMap);
        }

        return result;
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
