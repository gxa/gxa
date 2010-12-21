package ae3.service;

import com.google.common.io.CharStreams;
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
 * Custom MyDAS servlet allowing external configuration
 * <p/>
 * The intention behind this class is to allow Atlas code to slot atlasProperties.getDasBase() value into
 * {@link #MY_DAS_CONFIG_FILE}, used by {@link uk.ac.ebi.mydas.controller.MydasServlet} at servlet initialisation time.
 * As currently no setter methods are provided by this external library,  what we can do is replace
 * {@link uk.ac.ebi.mydas.controller.MydasServlet}'s {@link ServletContext} with our own, in which we implement
 * {@link ServletContext#getResourceAsStream} method.
 * This method then replaces all occurrences of the placeholder field with the value from {@link AtlasProperties}
 *
 * @see MydasGxaServlet
 */
public class MydasGxaServletContext implements ServletContext {

    private final static String MY_DAS_CONFIG_FILE = "MydasServerConfig.xml";
    private final static String DASBASE_PLACEHOLDER_REGEX = "\\$\\{atlas\\.dasbase\\}";
    private AtlasProperties atlasProperties;

    /**
     * ServletContext in which we want to override getResourceAsStream() method - in order
     * to slot atlasProperties.getDasBase() into the config content returned by getResourceAsStream()
     */
    private ServletContext sc;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public MydasGxaServletContext(ServletContext sc, AtlasProperties atlasProperties) {
        this.sc = sc;
        this.atlasProperties = atlasProperties;
    }

    /**
     * Returns the resource located at the named path as
     * an <code>InputStream</code> object&mdash;perhaps, filtered
     * by {@link #filter}.
     * <p/>
     *
     * @param path a <code>String</code> specifying the path
     *             to the resource
     * @return the <code>InputStream</code> returned to the
     *         servlet, or <code>null</code> if no resource exists at the
     *         specified path
     */
    public InputStream getResourceAsStream(String path) {
        if (path == null || !path.endsWith(MY_DAS_CONFIG_FILE)) {
            return sc.getResourceAsStream(path);
        }
        // if it's a MY_DAS_CONFIG_FILE, filter it
        return filter(sc.getResourceAsStream(path));
    }

    /**
     * Override DASBASE_PLACEHOLDER_REGEX  with atlasProperties.getDasBase() and return the result
     *
     * @param is the stream to filter
     * @return is with all occurrences of placeHolder replaced with newValue
     */
    public InputStream filter(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            String resource = CharStreams.toString(new InputStreamReader(is));
            String filteredResource = resource.replaceAll(DASBASE_PLACEHOLDER_REGEX, atlasProperties.getDasBase());
            return convertStringToInputStream(filteredResource);
        } catch (IOException e) {
            String msg = "Error replacing dasbase placeholder in: " + MY_DAS_CONFIG_FILE + " with " + atlasProperties.getDasBase();
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Source: http://www.kodejava.org/examples/265.html
     * <p/>
     * Convert String to InputStream using ByteArrayInputStream
     * class. This class constructor takes the string byte array
     * which can be done by calling the getBytes() method.
     * <p/>
     * Method is public to make junit testable.
     *
     * @param s String
     * @return InputStream representation of s
     */
    public InputStream convertStringToInputStream(String s) {
        try {
            return new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported, cannot work.", e);
        }
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

    @Deprecated
    public Servlet getServlet(String s) throws ServletException {
        return sc.getServlet(s);
    }

    @Deprecated
    public Enumeration getServlets() {
        return sc.getServlets();
    }

    @Deprecated
    public Enumeration getServletNames() {
        return sc.getServletNames();
    }

    public void log(String s) {
        sc.log(s);
    }

    @Deprecated
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
}

