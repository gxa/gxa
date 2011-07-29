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

package uk.ac.ebi.gxa.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Ignoring ETag header in tomcat (http://developer.yahoo.com/blogs/ydn/posts/2007/07/high_performanc_11/).
 *
 * @author Olga Melnichuk
 */
public class NoEtagHeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(NoEtagHeaderFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, new HttpServletResponseWrapper((HttpServletResponse) response) {
            public void setHeader(String name, String value) {
                if (!"etag".equalsIgnoreCase(name)) {
                    super.setHeader(name, value);
                } else {
                    log.debug("Ignoring Etag header: " + name + " " + value);
                }
            }
        });
    }
}

