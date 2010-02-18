package uk.ac.ebi.gxa.requesthandlers.helper;

import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.AtlasPlotter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author pashky
 */
public class PlotterRequestHandler extends AbstractRestRequestHandler {
    final Logger log = LoggerFactory.getLogger(getClass());

    AtlasPlotter plotter;

    public void setPlotter(AtlasPlotter plotter) {
        this.plotter = plotter;
    }

    public Object process(HttpServletRequest request) {

        String gid = request.getParameter("gid");
        String eid = request.getParameter("eid");
        String plotType = "bar";
        String ef = "default";
        String efv = "";

        if(request.getParameter("plot") != null)
            plotType = request.getParameter("plot");

        if(request.getParameter("ef") != null && !request.getParameter("ef").equals(""))
            ef = request.getParameter("ef");

        if(request.getParameter("efv") != null)
            efv = request.getParameter("efv");

        return plotter.getGeneInExpPlotData(gid, eid, ef, efv, plotType);
    }
}
