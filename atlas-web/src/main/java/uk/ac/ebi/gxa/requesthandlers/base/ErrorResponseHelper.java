package uk.ac.ebi.gxa.requesthandlers.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Error redirect helpers
 * @author pashky
 */
public class ErrorResponseHelper {
    private static final String ERROR_JSP = "/WEB-INF/jsp/error.jsp";

    public static void errorUnavailable(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher(ERROR_JSP).forward(request,response);
    }

    public static void errorNotFound(HttpServletRequest request, HttpServletResponse response, String message) 
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher(ERROR_JSP).forward(request,response);
    }
}
