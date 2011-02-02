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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ostolop
 */
public class DataDownloadRequestHandler implements HttpRequestHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected File atlasDataRepo;

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        final String reqPI = httpServletRequest.getPathInfo();

        log.info("Request for data download d/l {}", reqPI);

        Pattern regexPattern = Pattern.compile("^/?(.+).zip$");
        Matcher regexMatcher = regexPattern.matcher(reqPI);
        String Accession = null;
        if (regexMatcher.matches()) {
            Accession = regexMatcher.group(1);
        }
        File file = new File(new File(atlasDataRepo, "export"), Accession + ".zip");
        FileDownloadServer.processRequest(file, "application/zip", httpServletRequest, httpServletResponse);
    }
}
