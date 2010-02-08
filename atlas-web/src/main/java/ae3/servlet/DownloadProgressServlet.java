package ae3.servlet;

import ae3.service.AtlasDownloadService;
import ae3.servlet.structuredquery.RestServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author pashky
 */
public class DownloadProgressServlet extends RestServlet {
    private AtlasDownloadService downloadService;

    public void setDownloadService(AtlasDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public Object process(HttpServletRequest request) {
        Object downloads = downloadService.getDownloads(request.getSession().getId());
        return downloads != null ? downloads : new HashMap();
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if(request.getParameter("progress") == null) {
            request.setAttribute("downloads",  downloadService.getDownloads(request.getSession().getId()));
            request.getRequestDispatcher("downloads.jsp").forward(request, response);
        } else {
            super.handleRequest(request, response);
        }
    }
}
