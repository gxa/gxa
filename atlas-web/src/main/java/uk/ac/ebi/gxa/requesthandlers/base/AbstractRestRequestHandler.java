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

package uk.ac.ebi.gxa.requesthandlers.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestResultRenderException;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * REST API base servlet, implementing common functions as output format and style parameters handling,
 * GET/POST unification, exception handling etc.
 *
 * @author pashky
 */
public abstract class AbstractRestRequestHandler implements HttpRequestHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());
    private static final RestResultRenderer.ErrorWrapper ERROR_WRAPPER = new RestResultRenderer.ErrorWrapper() {
        public Object wrapError(Throwable e) {
            return new ErrorResult(e);
        }
    };
    private static final String XML_ROOTNAME = "atlasResponse";

    private static enum Format {
        JSON, XML;

        static Format parse(String s) {
            try {
                return Format.valueOf(s.toUpperCase());
            } catch (Exception e) {
                return JSON;
            }
        }
    }

    private Class profile = Object.class;

    /**
     * Use this function to set REST output formatter profile, to deal properly with the result of doRest() method
     *
     * @param profile profile class
     */
    protected void setRestProfile(Class profile) {
        this.profile = profile;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean indent = request.getParameter("indent") != null;
        Format format = Format.parse(request.getParameter("format"));
        try {
            Object o;
            try {
                o = process(request);
            } catch (final RuntimeException e) {
                log.error("Exception in servlet process()", e);
                o = new ErrorResult(e);
            }

            RestResultRenderer renderer;
            switch (format) {
                case XML: {
                    response.setContentType("text/xml");
                    response.setCharacterEncoding("utf-8");
                    renderer = new XmlRestResultRenderer(indent, 4, XML_ROOTNAME);
                }
                break;
                case JSON: {
                    if (indent)
                        response.setContentType("application/javascript");
                    else
                        response.setContentType("application/json");
                    response.setCharacterEncoding("utf-8");
                    String jsonCallback = request.getParameter("callback");
                    if (jsonCallback != null)
                        jsonCallback = jsonCallback.replaceAll("[^a-zA-Z0-9_]", "");
                    renderer = new JsonRestResultRenderer(indent, 4, jsonCallback);
                }
                break;
                default:
                    throw new IllegalStateException("Unknown format: " + format);
            }

            renderer.setErrorWrapper(ERROR_WRAPPER);
            log.debug("Beginning to render REST response in format: " + format);
            long timeStart = System.currentTimeMillis();
            renderer.render(o, response.getWriter(), profile);
            log.info("Finished rendering REST response in " + (System.currentTimeMillis() - timeStart) + " ms");
        } catch (IOException e) {
            // i/o exception is bad, we probably better give up writing anything
            log.error("I/O exception", e);
        } catch (RestResultRenderException e) {
            fatal(format, "Response render exception", e, response.getWriter());
        }
    }

    private void fatal(Format f, String text, Throwable e, PrintWriter out) {
        log.error(text, e);
        switch (f) {
            case JSON:
                out.println("{error:\"Fatal error\"}");
                break;
            case XML:
                out.println("<?xml version=\"1.0\"?><" + XML_ROOTNAME + "><error>Fatal error</error></" + XML_ROOTNAME + ">");
                break;
        }
    }

    /**
     * Implement this method to process REST API requests
     *
     * @param request HTTP request to handle
     * @return result object to be formatted with REST output formatter according to chosen by setRestProfile() mthod
     *         profile.
     */
    public abstract Object process(HttpServletRequest request);
}

