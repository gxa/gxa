package ae3.servlet.structuredquery;

import com.jamesmurty.utils.XMLBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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

    private void doJson(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean indent = request.getParameter("indent") != null;
        Format format = Format.parse(request.getParameter("format"));
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

            switch (format) {
                case XML: {
                    response.setContentType("text/xml");
                    response.setCharacterEncoding("utf-8");
                    XMLBuilder xml = writeJsonAsXml(o, XMLBuilder.create("atlasResponse"));
                    Properties props = new Properties();
                    if (indent) {
                        props.put("indent", "yes");
                        props.put("{http://xml.apache.org/xalan}indent-amount", "4");
                    }
                    xml.toWriter(response.getWriter(), props);
                }
                break;
                case JSON: {
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("utf-8");
                    if (indent)
                        response.getWriter().write(o.toString(4));
                    else
                        o.write(response.getWriter());
                }
                break;
            }
        } catch (JSONException e) {
            fatal(format, "JSON Exception", e, response.getWriter());
        } catch (ParserConfigurationException e) {
            fatal(format, "JSON/XML Parser Exception", e, response.getWriter());
        } catch (TransformerException e) {
            fatal(format, "JSON/XML Transformer Exception", e, response.getWriter());
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

    public abstract JSONObject process(HttpServletRequest request) throws JSONException;

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    private static Iterator makeIterator(Object o) {
        if(o instanceof Iterable)
            return ((Iterable)o).iterator();
        if(o instanceof JSONArray) {
            final JSONArray a = (JSONArray)o;
            return new Iterator() {
                int i = 0;
                public boolean hasNext() {
                    return i < a.length();
                }

                public Object next() {
                    try {
                        return a.get(i++);
                    } catch(JSONException e) {
                        return null;
                    }
                }

                public void remove() { }
            };
        }
        return null;
    }

    private static XMLBuilder writeJsonArrayAsXml(String iname, Iterator i, XMLBuilder xml) {
        while(i.hasNext()) {
            Object value = i.next();
            xml = xml.e(iname);
            if(value instanceof JSONObject) {
                xml = writeJsonAsXml((JSONObject)value, xml);
            } else if(value instanceof Map) {
                xml = writeJsonAsXml(new JSONObject((Map)value), xml);
            } else {
                Iterator iter = makeIterator(value);
                if(iter != null)
                    xml = writeJsonArrayAsXml(iname, iter, xml);
                else if(value != null)
                    xml = xml.t(value.toString());
            }
            xml = xml.up();
        }
        return xml;
    }

    private static XMLBuilder writeJsonAsXml(JSONObject o, XMLBuilder xml) {
        Iterator keys = o.keys();

        while (keys.hasNext()) {
            String k = keys.next().toString();
            Object v = null;
            try {
                v = o.get(k);
            } catch (JSONException e) {
                // nothing
            }
            if (v instanceof JSONObject) {
                xml = xml.e(k);
                xml = writeJsonAsXml(((JSONObject)v), xml);
                xml = xml.up();
            } else if(v instanceof Map) {
                xml.e(k);
                xml = writeJsonAsXml(new JSONObject((Map)v), xml);
                xml.up();
            } else {
                Iterator iter = makeIterator(v);
                if(iter != null) {
                    xml = xml.e(k.endsWith("s") ? k : k + "s");
                    xml = writeJsonArrayAsXml(k.endsWith("s") ? k.substring(0, k.length() - 1) : k, iter, xml);
                    xml = xml.up();
                } else if(v != null)
                    xml = xml.e(k).t(v.toString()).up();
            }
        }
        
        return xml;
    }
}

