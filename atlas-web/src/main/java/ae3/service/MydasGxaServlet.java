package ae3.service;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.mydas.controller.MydasServlet;

import javax.servlet.ServletContext;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 7, 2010
 * Time: 3:20:33 PM
 * This class behaves like MydasServlet, except for slotting in its own ServletContext - for more information
 * on rationale see MydasGxaServletContext.
 *
 */
public class MydasGxaServlet extends MydasServlet {

    // Atlas Spring ApplicationContext
    private static WebApplicationContext wac = null;

    public ServletContext getServletContext() {
        ServletContext parentSC = super.getServletContext();
        return new MydasGxaServletContext(parentSC, getAtlasPropertiesBean(parentSC));
    }

    /**
     *
     * @param servletContext
     * @return Atlas Spring ApplicationContext
     */
    private AtlasProperties getAtlasPropertiesBean(ServletContext servletContext) {
        if (wac == null) {
            wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        }
        return (AtlasProperties) wac.getBean("atlasProperties");
    }
}
