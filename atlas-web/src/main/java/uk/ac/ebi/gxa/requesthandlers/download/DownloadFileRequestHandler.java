package uk.ac.ebi.gxa.requesthandlers.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import ae3.util.FileDownloadServer;

import java.io.File;
import java.io.IOException;

/**
 * @author ostolop
 */
public class DownloadFileRequestHandler implements HttpRequestHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        final String reqPI = httpServletRequest.getPathInfo();

        log.info("Request for list view export d/l {}", reqPI);

        File file = new File(System.getProperty("java.io.tmpdir"), reqPI);
        FileDownloadServer.processRequest(file, "text/plain", httpServletRequest, httpServletResponse);
    }
}
