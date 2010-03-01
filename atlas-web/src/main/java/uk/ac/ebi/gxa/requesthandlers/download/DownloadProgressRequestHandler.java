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

package uk.ac.ebi.gxa.requesthandlers.download;

import ae3.service.AtlasDownloadService;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author pashky
 */
public class DownloadProgressRequestHandler extends AbstractRestRequestHandler {
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
            request.getRequestDispatcher("/WEB-INF/jsp/query/downloads.jsp").forward(request, response);
        } else {
            super.handleRequest(request, response);
        }
    }
}
