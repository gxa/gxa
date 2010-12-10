package ae3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 7, 2010
 * Time: 2:48:34 PM
 * The intention behind this class is to allow Atlas code to slot atlasProperties.getDasBase() value into MY_DAS_CONFIG_FILE,
 * used by MydasServlet at servlet initialisation time. As currently no setter methods are provided by this external library,
 * what we can do is replace MydasServlet's ServletContext with our own, in which we implement getResourceAsStream() method.
 * This method then replaces all occurrances of the placeholder field with the value from atlasProperties (see also MydasGxaServlet)
 */
public class MydasGxaServletContext implements ServletContext {

    private final static String MY_DAS_CONFIG_FILE = "MydasServerConfig.xml";
    private final static String DASBASE_PLACEHOLDER_REGEX = "\\$\\{atlas\\.dasbase\\}";

    private AtlasProperties atlasProperties;
    // ServletContext in which we want to override getResourceAsStream() method - in order
    // to slot atlasProperties.getDasBase() into the config content returned by getResourceAsStream()
    private ServletContext sc;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public MydasGxaServletContext(ServletContext sc, AtlasProperties atlasProperties) {
        this.sc = sc;
        this.atlasProperties = atlasProperties;

    }

    /**
     * @param resourcePath
     * @return resource in path resourcePath as InputStream
     */
    public InputStream getResourceAsStream(String resourcePath) {
        if (resourcePath != null && resourcePath.endsWith(MY_DAS_CONFIG_FILE)) {
            // Override DASBASE_PLACEHOLDER_REGEX in MY_DAS_CONFIG_FILE with atlasProperties.getDasBase()
            InputStream is = sc.getResourceAsStream(resourcePath);
            String atlasDasBase = atlasProperties.getDasBase();
            InputStream result = null;
            try {
                String myDasConfig = convertInputStreamToString(is);
                myDasConfig = myDasConfig.replaceAll(DASBASE_PLACEHOLDER_REGEX, atlasDasBase);
                result = convertStringToInputStream(myDasConfig);
            } catch (Exception e) {
                log.error("Error replacing dasbase placeholder in: " + MY_DAS_CONFIG_FILE + " with " + atlasDasBase, e);
            }
            return result;
        }
        return sc.getResourceAsStream(resourcePath);
    }


    // All public getter methods below delegate to ServletContext sc this class is composed with

    public ServletContext getContext(String s) {
        return sc.getContext(s);
    }

    public int getMajorVersion() {
        return sc.getMajorVersion();
    }

    public int getMinorVersion() {
        return sc.getMinorVersion();
    }

    public String getMimeType(String s) {
        return sc.getMimeType(s);
    }

    public Set getResourcePaths(String s) {
        return sc.getResourcePaths(s);
    }

    public URL getResource(String s) throws MalformedURLException {
        return sc.getResource(s);
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return sc.getRequestDispatcher(s);
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return sc.getNamedDispatcher(s);
    }


    public Servlet getServlet(String s) throws ServletException {
        return sc.getServlet(s);
    }

    public Enumeration getServlets() {
        return sc.getServlets();
    }

    public Enumeration getServletNames() {
        return sc.getServletNames();
    }

    public void log(String s) {
        sc.log(s);
    }

    public void log(Exception e, String s) {
        sc.log(e, s);
    }

    public void log(String s, Throwable throwable) {
        sc.log(s, throwable);
    }

    public String getRealPath(String s) {
        return sc.getRealPath(s);
    }

    public String getServerInfo() {
        return sc.getServerInfo();
    }

    public String getInitParameter(String s) {
        return sc.getInitParameter(s);
    }

    public Enumeration getInitParameterNames() {
        return sc.getInitParameterNames();
    }

    public Object getAttribute(String s) {
        return sc.getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return sc.getAttributeNames();
    }

    public void setAttribute(String s, Object o) {
        sc.setAttribute(s, o);
    }

    public void removeAttribute(String s) {
        sc.removeAttribute(s);
    }

    public String getServletContextName() {
        return sc.getServletContextName();
    }

    /**
     * Source: http://www.kodejava.org/examples/266.html
     * <p/>
     * To convert the InputStream to String we use the
     * Reader.read(char[] buffer) method. We iterate until the
     * Reader return -1 which means there's no more data to
     * read. We use the StringWriter class to produce the string.
     *
     * @param is
     * @return String representation of InputStream is
     * @throws IOException
     */
    private String convertInputStreamToString(InputStream is) throws IOException {
        String s = null;
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
                s = writer.toString();
            } finally {
                is.close();
            }

        }
        return s;
    }

    /**
     * Source: http://www.kodejava.org/examples/265.html
     * <p/>
     * Convert String to InputStream using ByteArrayInputStream
     * class. This class constructor takes the string byte array
     * which can be done by calling the getBytes() method.
     *
     * @param s String
     * @return InputStream representation of s
     */
    private InputStream convertStringToInputStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.getBytes("UTF-8"));
    }
}

