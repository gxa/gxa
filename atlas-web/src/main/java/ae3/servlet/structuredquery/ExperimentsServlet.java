package ae3.servlet.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import ae3.dao.AtlasObjectNotFoundException;
import ae3.dao.AtlasDao;
import ae3.service.structuredquery.ExperimentsService;
import ae3.service.structuredquery.ExperimentList;
import ae3.service.structuredquery.ExperimentRow;
import ae3.model.AtlasGene;
import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.Constants;

/**
 * @author pashky
 */
public class ExperimentsServlet extends HttpServlet {
    private Logger log = LoggerFactory.getLogger(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String geneIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if(geneIdKey != null && factor != null && factorValue != null)
        {
            boolean isEfo = Constants.EFO_FACTOR_NAME.equals(factor);

            request.setAttribute("ef", factor);
            request.setAttribute("efv", factorValue);

            if(isEfo) {
                Efo efo = Efo.getEfo();
                Efo.Term term = efo.getTermById(factorValue);
                if(term != null) 
                    request.setAttribute("efv", term.getTerm());
            }

            try {
                AtlasGene gene = AtlasDao.getGene(geneIdKey);
                ExperimentList experiments = isEfo ?
                        ExperimentsService.getExperiments(gene, factorValue, ExperimentList.ORDER_EXPID_PVALUE) :
                        ExperimentsService.getExperiments(gene, factor, factorValue, ExperimentList.ORDER_PVALUE);

                Set<Long> ups = new HashSet<Long>();
                Set<Long> dns = new HashSet<Long>();

                for(ExperimentRow er : experiments) {
                    (er.getUpdn().isUp() ? ups : dns).add(er.getExperimentId());
                }

                request.setAttribute("numup", ups.size());
                request.setAttribute("numdn", dns.size());

                request.setAttribute("gene", gene);
                request.setAttribute("exps", experiments);
                request.setAttribute("expsi", experiments.iterator());
                request.getRequestDispatcher("experiments.jsp").forward(request, response);
            } catch(AtlasObjectNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "Experiments not found for gene " + String.valueOf(geneIdKey));
                request.getRequestDispatcher("error.jsp").forward(request,response);
            }
        }
    }

}
