package ae3.servlet.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import ae3.restresult.*;

/**
 * @author pashky
 */
public abstract class RestServlet extends HttpServlet {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doRest(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doRest(httpServletRequest, httpServletResponse);
    }

    private static enum Format {
        JSON, XML;
        static Format parse(String s) {
            try {
                return Format.valueOf(s.toUpperCase());
            } catch(Exception e) {
                return JSON;
            }
        }
    }

    private Class profile = Object.class;

    protected void setRestProfile(Class profile) {
        this.profile = profile;
    }

    private void doRest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean indent = request.getParameter("indent") != null;
        Format format = Format.parse(request.getParameter("format"));
        try {
            Object o;
            try {
                o = process(request);
            } catch (final RuntimeException e) {
                log.error("Exception in servlet process()", e);
                o = new Object() {
                    public String getError() { return "Exception occured"; }
                    public String getException() { return exceptionToString(e); }
                };
            }

            RestResultRenderer renderer;
            switch (format) {
                case XML: {
                    response.setContentType("text/xml");
                    response.setCharacterEncoding("utf-8");
                    renderer = new XmlRestResultRenderer(indent, 4);
                }
                break;
                case JSON: {
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("utf-8");
                    renderer = new JsonRestResultRenderer(indent, 4);
                }
                break;
                default:
                    renderer = null;
            }

            renderer.render(o, response.getWriter(), profile);
        } catch (Exception e) {
            fatal(format, "Response render exception", e, response.getWriter());
        }
    }

    private void fatal(Format f, String text, Throwable e, PrintWriter out) {
        log.error(text, e);
        switch (f) {
            case JSON:
                out.println("{error:\"Fatal error\"}");
                break;
            case XML:
                out.println("<?xml version=\"1.0\"?><atlasResponse><error>Fatal error</error></atlasResponse>");
                break;
        }
    }

    public abstract Object process(HttpServletRequest request);

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
}

