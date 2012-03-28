package uk.ac.ebi.gxa.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.servlet.*;

/**
 * In London Data Centres external services direct their load balancing by issuing a 'health' web request to each of four
 * London Atlas data servers every 3 seconds. If a given server does not respond 3 times in a row (i.e. over a period of 9 seconds)
 * the load-balancer re-directs traffic to the remaining Atlas servers.
 * <p/>
 * Given this ES 'health' requests will be very frequent, we don't want them to pollute access logs. Atlas servers in London therefore
 * have a Valve defined in server.xml that prevents writing to access log requests for which attribute "health" has been set (c.f. doFilter()
 * method below).
 * <p/>
 * C.f. web.xml for which type of request this filter applies to.
 *
 * @author Robert Petryszak
 */
public class HealthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HealthFilter.class);

    public HealthFilter() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("health", Boolean.valueOf(true));
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterconfig)
            throws ServletException {
        log.debug("init");
    }

    public void destroy() {
        log.debug("destroy");
    }
}
