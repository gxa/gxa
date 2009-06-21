package ae3.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.File;

/**
 * @author ostolop
 */
public class ListviewDownloadServlet extends FileDownloadServlet {
    protected final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public void init() throws ServletException {
        setBasePath(System.getProperty("java.io.tmpdir"));
    }


    @Override
    /**
     * Returns filename where the gene identifiers are dumped to. If the file doesn't exist for some reason,
     * generates the dump.
     *
     */
    protected String getRequestedFilename(HttpServletRequest request) {
        final String reqPI = request.getPathInfo();

        log.info("Request for list view export d/l {}", reqPI);

        return reqPI;
    }
}
