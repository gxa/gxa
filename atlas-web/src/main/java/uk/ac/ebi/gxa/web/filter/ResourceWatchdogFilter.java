package uk.ac.ebi.gxa.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

public class ResourceWatchdogFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ResourceWatchdogFilter.class);

    private static final ThreadLocal<List<Closeable>> resources = new ThreadLocal<List<Closeable>>() {
        @Override
        protected List<Closeable> initialValue() {
            return new ArrayList<Closeable>();
        }
    };

    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("init");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            cleanup();
        }
    }

    public static void cleanup() {
        for (Closeable resource : resources.get()) {
            closeQuietly(resource);
        }
        resources.remove();
    }

    public void destroy() {
        log.debug("destroy");
    }

    public static void register(Closeable resource) {
        resources.get().add(resource);
    }
}
