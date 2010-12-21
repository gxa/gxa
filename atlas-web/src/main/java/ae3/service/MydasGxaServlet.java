package ae3.service;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.mydas.controller.MydasServlet;

import javax.servlet.ServletContext;

/**
 * This class behaves like {@link MydasServlet}, except for slotting in its own {@link ServletContext}&mdash;
 * for more information on the rationale see {@link MydasGxaServletContext}.
 *
 * @see MydasGxaServletContext
 */
public class MydasGxaServlet extends MydasServlet {
    @Override
    public ServletContext getServletContext() {
        ServletContext parentContext = super.getServletContext();
        return new MydasGxaServletContext(parentContext, getAtlasPropertiesBean(parentContext));
    }

    private AtlasProperties getAtlasPropertiesBean(ServletContext servletContext) {
        return (AtlasProperties) getSpringApplicationContext(servletContext).getBean("atlasProperties");
    }

    private static WebApplicationContext getSpringApplicationContext(ServletContext servletContext) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }
}
