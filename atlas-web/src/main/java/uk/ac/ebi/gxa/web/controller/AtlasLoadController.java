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

package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.ebi.gxa.loader.AtlasLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 13-Nov-2009
 */
public class AtlasLoadController extends AbstractController {
    private AtlasLoader loader;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasLoader getLoader() {
        return loader;
    }

    public void setLoader(AtlasLoader loader) {
        this.loader = loader;
    }

    public String getSuccessView() {
        return successView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    protected ModelAndView handleRequestInternal(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        // parse magetab_url para
        String magetabURL = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "magetab.url");
        log.info("Request to load: " + magetabURL);

        try {
            // load this document if the URL is valid
            loader.loadExperiment(new URL(magetabURL));

            return new ModelAndView(getSuccessView());
        }
        catch (MalformedURLException e) {
            String error = "the submitted URL (" + magetabURL + ") was not valid or inaccessible - " +
                    "check it exists and that you have permissions to access";

            // failure view
            Map<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("message", error);
            return new ModelAndView("load_fail.jsp", messageMap);
        }
    }
}
