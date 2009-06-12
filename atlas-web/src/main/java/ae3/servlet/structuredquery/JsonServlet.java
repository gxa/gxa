package ae3.servlet.structuredquery;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author pashky
 */
public abstract class JsonServlet extends HttpServlet {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doJson(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doJson(httpServletRequest, httpServletResponse);
    }

    private void doJson(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        try {
            JSONObject o;
            try {
                o = process(request);
            } catch (RuntimeException e) {
                log.error("Exception in JSON servlet", e);
                o = new JSONObject();
                o.put("error", "Exception occured");
                o.put("exception", exceptionToString(e));
            }
            o.write(response.getWriter());
        } catch (JSONException e) {
            throw new ServletException("JSON Exception", e);
        }
    }

    public abstract JSONObject process(HttpServletRequest request) throws JSONException;

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
}
