package ae3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.mydas.configuration.DataSourceConfiguration;
import uk.ac.ebi.mydas.configuration.GlobalConfiguration;
import uk.ac.ebi.mydas.configuration.Mydasserver;
import uk.ac.ebi.mydas.configuration.ServerConfiguration;
import uk.ac.ebi.mydas.controller.DataSourceManager;
import uk.ac.ebi.mydas.controller.MydasServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * {@link MydasServlet} tailored for Atlas
 * <p/>
 * This class behaves like {@link MydasServlet}, except for slotting in its own {@link ServletContext} and listening for
 * {@link AtlasProperties} changes.
 * <p/>
 * For more information on the rationale see {@link MydasGxaServletContext}.
 *
 * @see MydasGxaServletContext
 */
public class MydasGxaServlet extends MydasServlet implements VetoableChangeListener {
    private static final Logger log = LoggerFactory.getLogger(MydasGxaServlet.class);

    @Override
    public void init() throws ServletException {
        getAtlasPropertiesBean(getServletContext()).registerListener(this);
        super.init();
    }

    @Override
    public ServletContext getServletContext() {
        ServletContext parentContext = super.getServletContext();
        return new MydasGxaServletContext(parentContext, getAtlasPropertiesBean(parentContext));
    }

    private AtlasProperties getAtlasPropertiesBean(ServletContext servletContext) {
        return getSpringApplicationContext(servletContext).getBean(AtlasProperties.class);
    }

    private static WebApplicationContext getSpringApplicationContext(ServletContext servletContext) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    /**
     * MydasServlet, used by Atlas to expose its data as a DAS source, is configured at start up via MydasServerConfig.xml.
     * Maven build replaces atlas.dasbase placeholder in MydasServerConfig.xml with a value set in atlas-web/pom.xml
     * atlas.dasbase property is also configurable via AtlasProperties, but since MydasServlet code does not currently
     * provide access to its internal fields using atlas.dasbase, the only current way to re-configure MydasServlet code after
     * an AtlasProperties change to atlas.dasbase is vai the reflection hack below.
     * TODO replace this method with direct calls to MydasServlet code once setter methods are provided by the DAS team
     * <p/>
     * It would be nice to re-init the MydasServlet using standard {@link javax.servlet.Servlet#destroy()} and
     * {@link javax.servlet.Servlet#init} methods, but unfortunately the settings are stored in a static field
     * which gets destroyed but not cleared. Hence, the reflection tricks are still necessary,
     * which in turn makes re-init cycle senseless.
     *
     * @param dasBaseURL base URL for DAS
     * @return true if all fields were updated via reflection successfully; false otherwise
     */
    public boolean updateDasBaseURL(String dasBaseURL) {
        boolean success = false;
        try {
            Field field = ReflectionUtils.findField(MydasServlet.class, "DATA_SOURCE_MANAGER");
            field.setAccessible(true);
            Object dataSourceManager = ReflectionUtils.getField(field, null);
            if (dataSourceManager != null) {
                // GxaS4DasDataSource has been accessed at least once since Atlas startup and MydasServerConfig.xml was already
                // read in by MydasServlet - need to update the relevant object fields via reflection
                // web.xml is now configured to load MydasServlet at Atlas startup - if it is not loaded by the time
                // this method runs
                ServerConfiguration serverConfiguration = ((DataSourceManager) dataSourceManager).getServerConfiguration();

                // Set baseUrl to dasBaseURL - c.f. <baseurl>${atlas.dasbase}/</baseurl> in MydasServerConfig.xml
                GlobalConfiguration globalConfiguration = serverConfiguration.getGlobalConfiguration();
                Field baseUrl = globalConfiguration.getClass().getDeclaredField("baseURL");
                baseUrl.setAccessible(true);
                baseUrl.set(globalConfiguration, dasBaseURL);
                log.debug("Setting <baseurl> MydasServerConfig.xml to: dasBaseURL");

                // Update all capability fields with the new dasBaseURL - c.f. (in MydasServerConfig.xml)
                //    <capability type="das1:sources" query_uri="${atlas.dasbase}/s4" />
                //    <capability type="das1:types" query_uri="${atlas.dasbase}/s4/types" />
                //    <capability type="das1:features" query_uri="${atlas.dasbase}/s4/features?segment=ENSG00000162552" />
                Map<String, DataSourceConfiguration> dataSourceConfigMap = serverConfiguration.getDataSourceConfigMap();
                for (DataSourceConfiguration dsConfig : dataSourceConfigMap.values()) {
                    List<Mydasserver.Datasources.Datasource.Version> versions = dsConfig.getConfig().getVersion();
                    for (Mydasserver.Datasources.Datasource.Version version : versions) {
                        List<Mydasserver.Datasources.Datasource.Version.Capability> capabilities = version.getCapability();
                        for (Mydasserver.Datasources.Datasource.Version.Capability capability : capabilities) {
                            String queryUri = capability.getQueryUri();
                            String newQueryUri = queryUri.replaceFirst(".*/", dasBaseURL + "/");
                            log.debug("Setting query_uri of capability type: " + capability.getType() + " in MydasServerConfig.xml to " + newQueryUri);
                            capability.setQueryUri(newQueryUri);
                        }
                    }
                }
                success = true;
            }
        } catch (Exception e) {
            log.error("Failed to update dasBaseUrl to : " + dasBaseURL, e);
        }
        return success;
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if ("atlas.dasbase".equals(evt.getPropertyName())) {
            final boolean success = updateDasBaseURL((String) evt.getNewValue());
            if (!success)
                throw new PropertyVetoException("Cannot update DAS base to " + evt.getNewValue(), evt);
        }
    }
}
