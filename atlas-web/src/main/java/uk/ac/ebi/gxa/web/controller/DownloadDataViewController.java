/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.controller;

import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.download.DownloadTaskResult;
import uk.ac.ebi.gxa.download.TaskExecutionException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.service.DownloadDataService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static ae3.util.FileDownloadServer.processRequest;

/**
 * @author Olga Melnichuk
 */
@Controller
public class DownloadDataViewController extends AtlasViewController {

    private AtlasProperties atlasProperties;
    private DownloadDataService downloadService;

    @Autowired
    public DownloadDataViewController(AtlasProperties atlasProperties,
                                      DownloadDataService downloadService) {
        this.atlasProperties = atlasProperties;
        this.downloadService = downloadService;
    }

    @RequestMapping(value = "/download/geneSearch", method = RequestMethod.GET)
    public String downloadGeneSearchResults(
            @CookieValue("JSESSIONID") String cookie,
            HttpServletRequest request,
            Model model) {
        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request, atlasProperties);
        return taskToken(downloadService.addGeneSearchTask(atlasQuery, cookie), model);
    }

    @RequestMapping(value = "/download/experimentAnalytics", method = RequestMethod.GET)
    public String downloadExperimentAnalytics(
            @RequestParam("eacc") String expAcc,
            @CookieValue("JSESSIONID") String cookie,
            Model model) throws RecordNotFoundException {
        return taskToken(downloadService.addExperimentAnalyticsTask(expAcc, cookie), model);
    }

    @RequestMapping(value = "/download/experimentExpressions", method = RequestMethod.GET)
    public String downloadExperimentExpressions(
            @RequestParam("eacc") String expAcc,
            @CookieValue("JSESSIONID") String cookie,
            Model model) throws RecordNotFoundException {
        return taskToken(downloadService.addExperimentExpressionsTask(expAcc, cookie), model);
    }

    @RequestMapping(value = "/download/experimentDesign", method = RequestMethod.GET)
    public String downloadExperimentDesign(
            @RequestParam("eacc") String expAcc,
            @CookieValue("JSESSIONID") String cookie,
            Model model) {
        return taskToken(downloadService.addExperimentDesignTask(expAcc, cookie), model);
    }

    @RequestMapping(value = "/download/progress", method = RequestMethod.GET)
    public String getDownloadStatus(
            @RequestParam("token") String token,
            Model model) {
        model.addAttribute("token", token);
        try {
            model.addAttribute("progress", downloadService.getProgress(token));
        } catch (TaskExecutionException e) {
            Throwable cause = e.getCause();
            model.addAttribute("error", cause.getClass().getName() + ": " + cause.getMessage());
        }
        return JSON_ONLY_VIEW;
    }

    @RequestMapping(value = "/download/result", method = RequestMethod.GET)
    public void getDownloadResult(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response) throws ResourceNotFoundException, IOException {
        DownloadTaskResult result = downloadService.getResult(token);
        if (result == null || result.hasErrors()) {
            throw new ResourceNotFoundException("No download result found; token=" + token);
        }
        processRequest(result.getFile(), result.getContentType(), request, response);
    }

    @RequestMapping(value = "/download/cancel", method = RequestMethod.GET)
    public String cancelDownload(
            @RequestParam("token") String token,
            Model model) {
        // TODO add model attributes
        downloadService.cancelTask(token);
        return JSON_ONLY_VIEW;
    }

    private String taskToken(String token, Model model) {
        model.addAttribute("token", token);
        return JSON_ONLY_VIEW;
    }
}
