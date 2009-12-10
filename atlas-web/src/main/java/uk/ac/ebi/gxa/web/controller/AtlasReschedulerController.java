package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.dao.LoadType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 14-Nov-2009
 */
public class AtlasReschedulerController extends AbstractController {
    private AtlasDAO atlasDAO;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

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
        log.info("Request to schedule " + type + ": " + accession + " for re-indexing.");

        if (type.equals("experiment") || type.equals("gene")) {
            atlasDAO.writeLoadDetails(accession, LoadStage.SEARCHINDEX, LoadStatus.PENDING,
                                      (type.equals("experiment") ? LoadType.EXPERIMENT : LoadType.GENE));
            return new ModelAndView(getSuccessView());
        }
        else {
            String error = "the load type specified (" + type + ") is not permitted";

            // failure view
            Map<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("message", error);
            return new ModelAndView("load_fail.jsp", messageMap);
        }
    }
}
