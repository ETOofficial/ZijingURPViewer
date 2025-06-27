package dynastxu.zijingurpviewer.network;

import dynastxu.zijingurpviewer.global.GlobalState;

public class StandardVPNCookies extends Cookies{
    public StandardVPNCookies() {
        super(new Cookies()
                .put("VSG_VERIFYCODE_CONF", "0-0")
                .put("VSG_CLIENT_RUNNING", "false")
                .put("VSG_LANGUAGE", "zh_CN")
                .put("JSESSIONID", GlobalState.getInstance().getJSESSIONID())
                .put("VSG_SESSIONID", GlobalState.getInstance().getVSG_SESSIONID())
                .put("route", GlobalState.getInstance().getRoute())
                .put("mapid", "7b68f983")
                .getCookies()
        );
    }
}
