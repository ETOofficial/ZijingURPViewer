package dynastxu.zijingurpviewer.global;

import dynastxu.zijingurpviewer.network.AccessPath;

public class GlobalState {
    private static GlobalState instance;
    private boolean login = false;
    private boolean loginVPN = false;
    private AccessPath accessPath = AccessPath.OffCampus;

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
}

