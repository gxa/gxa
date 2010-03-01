/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://ostolop.github.com/gxa/
 */

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
