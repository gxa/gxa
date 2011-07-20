/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.tags.resourcebundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.tags.resourcebundle.wro4j.Wro4jResourceBundleConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Olga Melnichuk
 */
public class WebResourceBundleTag extends TagSupport {

    private static final long serialVersionUID = 6184065535320355872L;

    private final static Logger log = LoggerFactory.getLogger(WebResourceBundleTag.class);

    private static final WebResourceBundleProperties properties = new WebResourceBundleProperties();

    static {
        String resourceBundleProperties = "resourcebundle.properties";
        try {
            InputStream in = WebResourceBundleTag.class.getClassLoader().getResourceAsStream(resourceBundleProperties);
            if (in == null) {
                log.error(resourceBundleProperties + " not found in the classpath");
            } else {
                properties.load(in);
            }
        } catch (IOException e) {
            log.error(resourceBundleProperties + " not loaded", e);
        }
    }

    private String name;
    private final List<WebResourceType> resourceTypes = new ArrayList<WebResourceType>();

    public WebResourceBundleTag() {
        this(WebResourceType.values());
    }

    WebResourceBundleTag(WebResourceType... types) {
        resourceTypes.addAll(Arrays.asList(types));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int doStartTag() throws JspException {
        try {
            pageContext.getOut().write(html());
        } catch (IOException e) {
            throw jspTagException(e);
        } catch (WebResourceBundleConfigException e) {
            throw jspTagException(e);
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    private JspTagException jspTagException(Exception e) {
        return new JspTagException("Web resource aggregation tag throw an exception", e);
    }

    private String html() throws WebResourceBundleConfigException {
        WebResourceBundleConfig config = loadBundleConfig();
        String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
        return properties.isDebugOn() ? resourcesHtml(config, contextPath) : compressedResourcesHtml(config, contextPath);
    }

    private String compressedResourcesHtml(WebResourceBundleConfig config, String contextPath) throws WebResourceBundleConfigException {
        config.assertConfigured(name);

        StringBuilder sb = new StringBuilder();
        for (WebResourceType type : resourceTypes) {
            sb.append(
                    type.toHtml(WebResourcePath.joinPaths(contextPath, properties.getBundlePath(type)), name)
            ).append("\n");
        }
        return sb.toString();
    }

    private String resourcesHtml(WebResourceBundleConfig config, String contextPath) throws WebResourceBundleConfigException {
        Collection<WebResource> resources = config.getResources(name, resourceTypes);

        StringBuilder sb = new StringBuilder();
        for (WebResource res : resources) {
            sb.append(res.asHtml(contextPath)).append("\n");
        }
        return sb.toString();
    }

    private WebResourceBundleConfig loadBundleConfig() throws WebResourceBundleConfigException {
        File webInf = new File(pageContext.getServletContext().getRealPath("/"), "WEB-INF");
        File wro4jConfigPath = new File(webInf, "wro.xml");

        WebResourceBundleConfig config = new Wro4jResourceBundleConfig();
        config.load(wro4jConfigPath);
        return config;
    }
}
