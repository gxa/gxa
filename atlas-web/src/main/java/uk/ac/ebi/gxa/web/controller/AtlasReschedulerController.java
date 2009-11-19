package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 14-Nov-2009
 */
public class AtlasReschedulerController extends AbstractController {
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public String getSuccessView() {
        return successView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) throws Exception {
        // parse accession parameter
        String accession = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "accession");

        // parse type parameter
        String type = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "type");

        log.info("Need to reschedule " + type + ": " + accession + " for indexing " +
                "(update load_monitor so searchindex=pending)");

        // todo - update load_monitor table.  Something like
//        AtlasDB.updateLoadMonitor(accession, type, "pending");

        return new ModelAndView(getSuccessView());
    }
}
