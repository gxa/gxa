package uk.ac.ebi.gxa.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 13-Nov-2009
 */
public class AtlasNetCDFController extends AbstractController {
    private NetCDFGenerator netCDFGenerator;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public NetCDFGenerator getNetCDFGenerator() {
        return netCDFGenerator;
    }

    public void setNetCDFGenerator(NetCDFGenerator netCDFGenerator) {
        this.netCDFGenerator = netCDFGenerator;
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
        log.info("Request to generate NetCDFs for " + accession);

        String type = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "type");
        if (type.equals("experiment")) {
            // and generate netCDFs
            netCDFGenerator.generateNetCDFsForExperiment(accession);

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
