package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;


/**
 * Request wrapper utility class alowing to safely fetch HTTP parameters with type conversion and defaults
 * @author pashky
 */
public class RequestWrapper {
    private HttpServletRequest request;

    public RequestWrapper(HttpServletRequest request) {
        this.request = request;
    }

    public int getInt(String name) {
        return getInt(name, 0);
    }

    public int getInt(String name, int def) {
        try {
            return Integer.valueOf(request.getParameter(name));
        } catch(Exception e) {
            return def;
        }
    }

    public int getInt(String name, int def, int min) {
        return Math.max(getInt(name, def), min);
    }

    public int getInt(String name, int def, int min, int max) {
        return Math.max(Math.min(getInt(name, def), max), min);
    }

    public String[] getStrArray(String name) {
        String[] v = request.getParameterValues(name);
        return v == null ? new String[0] : v;
    }

    public String getStr(String name) {
        String v = request.getParameter(name);
        return v == null ? "" : v;
    }

    public boolean getBool(String name) {
        String v = request.getParameter(name);
        return v != null && ("1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v));
    }

    @SuppressWarnings("unchecked")
    public Map<String,String[]> getMap() {
        return request.getParameterMap();
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String name, T def) {
        try {
            return Enum.valueOf((Class<T>)def.getClass(), request.getParameter(name));
        } catch(Exception e) {
            return def; 
        }
    }



    public String getRemoteHost() {
        String remoteId = request.getRemoteHost();
        if(remoteId == null || "".equals(remoteId))
            remoteId = request.getRemoteAddr();
        if(remoteId == null || "".equals(remoteId))
            remoteId = "unknown";
        return remoteId;
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }
    
}
