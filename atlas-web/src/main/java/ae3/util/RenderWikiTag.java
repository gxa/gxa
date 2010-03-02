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

package ae3.util;

import info.bliki.wiki.model.WikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

/**
 * JSP tag to render Wiki content stored in static MediaWiki-formatted files to HTML.
 *
 * Files are located in WEB-INF/help, images are located in WEB-INF/images/help. Wiki-formatted
 * files should have extension ".mwiki". Any custom template should also be in WEB-INF/help and
 * the file name should end in "Template.mwiki"
 *
 * Wiki pages are determined through context. Thus if the request comes in for /help/AtlasApis,
 * we search for WEB-INF/help/AtlasApis.mwiki.
 *
 * TODO: new wikiModel created for each request; possibly need to pass Wiki page to draw as parameter to tag
 *
 * @author ostolop
 */
public class RenderWikiTag extends TagSupport {
    final private Logger log = LoggerFactory.getLogger(getClass());

    final static public String DEFAULT_WIKI_HOME = "/AtlasHelp";
    final static public String DEFAULT_WIKI_404  = "/HelpNotFound";

    public int doStartTag() throws javax.servlet.jsp.JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        WikiModel wikiModel =
                new AtlasWikiModel(request.getContextPath() + "/images/help/${image}",
                                   request.getContextPath() + "/help/${title}");

        try {
            final String file = request.getRequestURI();
            String pageName = file.substring(file.lastIndexOf('/'));
            if ("/".equals(pageName) || "/help".equals(pageName) ) pageName = DEFAULT_WIKI_HOME;

            String pageContent;
            try {
                pageContent = getResourceAsString(pageContext.getServletContext(),
                                                  "/WEB-INF/help" + pageName + ".mwiki");
            } catch (NullPointerException npe) {
                // assume this npe was due to wiki page not available
                pageContent = getResourceAsString(pageContext.getServletContext(),
                                                  "/WEB-INF/help" + DEFAULT_WIKI_404 + ".mwiki");
            }

            pageContext.getOut().print(wikiModel.render(pageContent));
        } catch (IOException ioe) {
            throw new JspException("I/O Error : " + ioe.getMessage());
        }

        return Tag.SKIP_BODY;
    }

    private String getResourceAsString(ServletContext servletContext, final String res) throws IOException {
        final char[] buffer = new char[0x10000];

        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(servletContext.getResourceAsStream(res), "UTF-8");

        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);

        return out.toString();
    }

    private class AtlasWikiModel extends WikiModel {
        public AtlasWikiModel(String imageBase, String pageBase) {
            super(imageBase, pageBase);
        }

        @Override
        public String getRawWikiContent(String namespace, String templateName, Map templateParameters) {
            String result = super.getRawWikiContent(namespace, templateName, templateParameters);

            if (result != null) {
                return result;
            }

            try {
                if (namespace.equals("Template")) {
                    String name = encodeTitleToUrl(templateName, true);

                    String templateContent;
                    try {
                        templateContent = getResourceAsString(pageContext.getServletContext(), "/WEB-INF/help/" + name + "Template.mwiki");
                    } catch (NullPointerException npe) {
                        // template missing
                        templateContent = "Missing template " + name; 
                    }

                    return templateContent;
                }
            } catch (IOException e) {
                log.error("I/O exception while reading template" + templateName, e);
            }

            return null;
        }
    }
}
