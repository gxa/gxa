package uk.ac.ebi.gxa.web.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter setting no cache headers
 * @author pashky
 */
public class NoCacheHeaderFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(servletResponse instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.setHeader("Cache-Control","no-cache");
            response.setHeader("Pragma","no-cache");
            response.setDateHeader ("Expires", 0);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
