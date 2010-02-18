package uk.ac.ebi.gxa.requesthandlers.base;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * REST API base servlet, implementing common functions as output format and style parameters handling,
 * GET/POST unification, exception handling etc.
 * @author pashky
 */
public abstract class AbstractRestRequestHandler implements HttpRequestHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());

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

    /**
     * Use this function to set REST output formatter profile, to deal properly with the result of doRest() method
     * @param profile profile class
     */
    protected void setRestProfile(Class profile) {
        this.profile = profile;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean indent = request.getParameter("indent") != null;
        Format format = Format.parse(request.getParameter("format"));
        try {
            Object o;
            try {
                o = process(request);
            } catch (final RuntimeException e) {
                log.error("Exception in servlet process()", e);
                o = new ErrorResult("Exception occured: " + exceptionToString(e));
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
                    String jsonCallback = request.getParameter("callback");
                    if(jsonCallback != null)
                        jsonCallback = jsonCallback.replaceAll("[^a-zA-Z0-9_]", "");                    
                    renderer = new JsonRestResultRenderer(indent, 4, jsonCallback);
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

    /**
     * Implement this method to process REST API requests
     * @param request HTTP request to handle
     * @return result object to be formatted with REST output formatter according to chosen by setRestProfile() mthod
     * profile.
     */
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

