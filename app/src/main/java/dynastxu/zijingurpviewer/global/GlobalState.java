package dynastxu.zijingurpviewer.global;

import android.util.Log;

import java.util.Objects;

import dynastxu.zijingurpviewer.network.AccessPath;

public class GlobalState {
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0";
    private static GlobalState instance;
    private boolean login = false;
    private boolean loginVPN = false;
    private AccessPath accessPath = AccessPath.OffCampus;
    private String VSG_SESSIONID = "";
    private String route = "";
    private String JSESSIONID = "";
    private String username = "";
    private String ID = "";

    private GlobalState() {
    }

    public static synchronized GlobalState getInstance() {
        if (instance == null) {
            instance = new GlobalState();
        }
        return instance;
    }

    // Getter/Setter 方法
    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public boolean isLoginVPN() {
        return loginVPN;
    }

    public void setLoginVPN(boolean loginVPN) {
        this.loginVPN = loginVPN;
    }

    public AccessPath getAccessPath() {
        return accessPath;
    }

    public void setAccessPath(AccessPath accessPath) {
        this.accessPath = accessPath;
    }

    public String getVSG_SESSIONID() {
        if (Objects.equals(VSG_SESSIONID, "")) Log.w("cookie", "VSG_SESSIONID is empty");
        return VSG_SESSIONID;
    }

    public void setVSG_SESSIONID(String VSG_SESSIONID) {
        this.VSG_SESSIONID = VSG_SESSIONID;
    }

    public String getRoute() {
        if (Objects.equals(route, "")) Log.w("cookie", "route is empty");
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getJSESSIONID() {
        if (Objects.equals(JSESSIONID, "")) Log.w("cookie", "JSESSIONID is empty");
        return JSESSIONID;
    }

    public void setJSESSIONID(String JSESSIONID) {
        this.JSESSIONID = JSESSIONID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }
}

