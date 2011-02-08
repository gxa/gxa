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
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.download;

import ae3.util.FileDownloadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.utils.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static uk.ac.ebi.gxa.utils.FileUtil.tempFile;

/**
 * @author ostolop
 */
public class DownloadFileRequestHandler implements HttpRequestHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        final String reqPI = httpServletRequest.getPathInfo();

        log.info("Request for list view export d/l {}", reqPI);

        File file = tempFile(reqPI);
        FileDownloadServer.processRequest(file, "text/plain", httpServletRequest, httpServletResponse);
    }
}
