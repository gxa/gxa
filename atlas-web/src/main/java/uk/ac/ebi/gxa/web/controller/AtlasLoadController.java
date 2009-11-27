package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.ebi.gxa.loader.AtlasLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 13-Nov-2009
 */
public class AtlasLoadController extends AbstractController {
    private AtlasLoader loader;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasLoader getLoader() {
        return loader;
    }

    public void setLoader(AtlasLoader loader) {
        this.loader = loader;
    }

    public String getSuccessView() {
        return successView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    protected ModelAndView handleRequestInternal(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        // parse magetab_url para
        String magetabURL = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "magetab.url");
        log.info("Request to load: " + magetabURL);

        try {
            // load this document if the URL is valid
            loader.loadExperiment(new URL(magetabURL));

            return new ModelAndView(getSuccessView());
        }
        catch (MalformedURLException e) {
            String error = "the submitted URL (" + magetabURL + ") was not valid or inaccessible - " +
                    "check it exists and that you have permissions to access";

            // failure view
            Map<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("message", error);
            return new ModelAndView("load_fail.jsp", messageMap);
        }
    }
}
