package uk.ac.ebi.gxa.controller;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.ServletRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.gxa.index.builder.IndexBuilder;

import java.util.Map;
import java.util.HashMap;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 13-Nov-2009
 */
public class AtlasIndexController extends AbstractController {
    private IndexBuilder indexBuilder;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public IndexBuilder getIndexBuilder() {
        return indexBuilder;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
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
        log.info("Request to build index for " + accession);

        String type = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "type");
        if (type.equals("experiment")) {
            // and build index
//            indexBuilder.buildIndex();

            return new ModelAndView(getSuccessView());
        }
        else {
            String error = "the type '" + type + "' was not recognised";

            // failure view
            Map<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("message", error);
            return new ModelAndView("load_fail.jsp", messageMap);
        }
    }
}
