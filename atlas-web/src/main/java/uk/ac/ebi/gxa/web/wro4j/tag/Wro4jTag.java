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

package uk.ac.ebi.gxa.web.wro4j.tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.wro4j.tag.config.Wro4jConfig;
import uk.ac.ebi.gxa.web.wro4j.tag.config.Wro4jConfigException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Olga Melnichuk
 */
public class Wro4jTag extends TagSupport {

    private static final long serialVersionUID = 6184065535320355872L;

    private final static Logger log = LoggerFactory.getLogger(Wro4jTag.class);

    private static final Wro4jTagProperties properties = new Wro4jTagProperties();

    static {
        String tagProperties = "wro4j-tag.properties";
        try {
            InputStream in = Wro4jTag.class.getClassLoader().getResourceAsStream(tagProperties);
            if (in == null) {
                log.error(tagProperties + " not found in the classpath");
            } else {
                properties.load(in);
            }
        } catch (IOException e) {
            log.error("Wro4jTag error: " + tagProperties + " not loaded", e);
        }
    }

    private String name;
    private final List<WebResourceType> resourceTypes = new ArrayList<WebResourceType>();

    public Wro4jTag() {
        this(WebResourceType.values());
    }

    Wro4jTag(WebResourceType... types) {
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
        } catch (Wro4jConfigException e) {
            throw jspTagException(e);
        } catch (Wro4jTagException e) {
            throw jspTagException(e);
        }
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    private JspTagException jspTagException(Exception e) {
        log.error("Wro4jTag error: " + e.getMessage(), e);
        return new JspTagException("Wro4jTag threw an exception; see logs for details");
    }

    private String html() throws Wro4jConfigException, Wro4jTagException {
        Wro4jConfig config = loadWro4jConfig();
        String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
        return properties.isDebugOn() ? notAggregatedVersion(config, contextPath) : aggregatedVersion(config, contextPath);
    }

    private String aggregatedVersion(Wro4jConfig config, String contextPath) throws Wro4jConfigException, Wro4jTagException {
        StringBuilder sb = new StringBuilder();
        for (WebResourceType type : resourceTypes) {
            if (config.hasResources(name, type)) {
                String fullPath = WebResourcePath.joinPaths(contextPath, aggregationFullName(name, type));
                sb.append(type.toHtml(fullPath)).append("\n");
            }
        }
        return sb.toString();
    }

    private String notAggregatedVersion(Wro4jConfig config, String contextPath) throws Wro4jConfigException {
        Collection<WebResource> resources = config.getResources(name, resourceTypes);
        StringBuilder sb = new StringBuilder();
        for (WebResource res : resources) {
            sb.append(res.asHtml(contextPath)).append("\n");
        }
        return sb.toString();
    }

    private Wro4jConfig loadWro4jConfig() throws Wro4jConfigException {
        File webInf = new File(pageContext.getServletContext().getRealPath("/"), "WEB-INF");
        File wro4jConfigPath = new File(webInf, "wro.xml");

        Wro4jConfig config = new Wro4jConfig();
        config.load(wro4jConfigPath);
        return config;
    }

    private String aggregationFullName(String groupName, WebResourceType type) throws Wro4jTagException {
        String aggregationPath = properties.getAggregationPath(type);
        final Pattern namePattern = properties.getAggregationNamePattern(groupName, type);

        File folder = new File(pageContext.getServletContext().getRealPath("/"), aggregationPath);
        String[] names = folder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return namePattern.matcher(name).matches();
            }
        });

        if (names.length == 1) {
            return WebResourcePath.joinPaths(aggregationPath, names[0]);
        } else if (names.length > 1) {
            throw new Wro4jTagException("More than one file matches the pattern '" + namePattern + "': " + Arrays.toString(names));
        }
        throw new Wro4jTagException("No file matching the pattern: '" + namePattern + "' found in the path: " + folder.getAbsolutePath());
    }
}
