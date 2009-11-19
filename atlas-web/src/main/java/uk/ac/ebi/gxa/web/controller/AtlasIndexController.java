package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        // parse pending parameter
        boolean pending = Boolean.parseBoolean(ServletRequestUtils.getStringParameter(httpServletRequest, "pending"));

        log.info("Request to " + (pending ? "update" : "build") + "  index");
        if (pending) {
            // build index for pending items only
            indexBuilder.updateIndex();
            return new ModelAndView(getSuccessView());
        }
        else {
            // build index for pending items only
            indexBuilder.buildIndex();
            return new ModelAndView(getSuccessView());
        }
    }
}
