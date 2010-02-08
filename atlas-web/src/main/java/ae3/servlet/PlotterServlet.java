package ae3.servlet;

import ae3.servlet.structuredquery.RestServlet;
import ae3.servlet.structuredquery.result.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.AtlasPlotter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author pashky
 */
public class PlotterServlet extends RestServlet {
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
