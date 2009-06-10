package ae3.servlet.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;

import ae3.dao.AtlasDao;
import ae3.model.AtlasGene;
import ae3.model.AtlasExperiment;
import ae3.util.CuratedTexts;
import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.Experiment;

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
        JSONObject jsResult = new JSONObject();

        String geneIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if(geneIdKey != null && factor != null && factorValue != null)
        {
            boolean isEfo = Constants.EFO_FACTOR_NAME.equals(factor);

            try {
                jsResult.put("ef", factor);
                jsResult.put("eftext", CuratedTexts.get("head.ef." + factor));
                jsResult.put("efv", factorValue);

                if(isEfo) {
                    Efo efo = Efo.getEfo();
                    Efo.Term term = efo.getTermById(factorValue);
                    if(term != null)
                        jsResult.put("efv", term.getTerm());
                }

                AtlasGene gene = AtlasDao.getGene(geneIdKey);

                JSONObject jsGene = new JSONObject();

                jsGene.put("id", geneIdKey);
                jsGene.put("identifier", gene.getGeneIdentifier());
                jsGene.put("name", gene.getGeneName());
                jsResult.put("gene", jsGene);

                Map<Long,Map<String, List<Experiment>>> exmap = new HashMap<Long,Map<String,List<Experiment>>>();
                for(Experiment exp : isEfo ?
                        gene.getExpermientsTable().findByEfoSet(Efo.getEfo().getTermAndAllChildrenIds(factorValue)) :
                        gene.getExpermientsTable().findByEfEfv(factor, factorValue)) {
                    Map<String,List<Experiment>> efmap = exmap.get(exp.getId());
                    if(efmap == null)
                        exmap.put(exp.getId(), efmap = new HashMap<String, List<Experiment>>());
                    List<Experiment> list = efmap.get(exp.getEf());
                    if(list == null)
                        efmap.put(exp.getEf(), list = new ArrayList<Experiment>());

                    list.add(exp);

                }

                for(Map<String,List<Experiment>> ef : exmap.values())
                    for(List<Experiment> e : ef.values())
                    {
                        Collections.sort(e, new Comparator<Experiment>() {
                            public int compare(Experiment o1, Experiment o2) {
                                return o1.getPvalue() - o2.getPvalue() < 0 ? -1 : 1;
                            }
                        });
                    }

                @SuppressWarnings("unchecked")

                List<Map.Entry<Long,Map<String, List<Experiment>>>> exps = new ArrayList<Map.Entry<Long,Map<String, List<Experiment>>>>(exmap.entrySet());
                Collections.sort(exps, new Comparator<Map.Entry<Long,Map<String, List<Experiment>>>>() {
                    public int compare(Map.Entry<Long, Map<String, List<Experiment>>> o1, Map.Entry<Long, Map<String, List<Experiment>>> o2) {
                        double minp1 = 1;
                        for(Map.Entry<String,List<Experiment>> ef : o1.getValue().entrySet())
                            minp1 = Math.min(minp1, ef.getValue().get(0).getPvalue());
                        double minp2 = 1;
                        for(Map.Entry<String,List<Experiment>> ef : o2.getValue().entrySet())
                            minp2 = Math.min(minp2, ef.getValue().get(0).getPvalue());
                        return minp1 < minp2 ? -1 : 1;
                    }
                });

                int numUp = 0, numDn = 0;

                JSONArray jsExps = new JSONArray();
                for(Map.Entry<Long,Map<String, List<Experiment>>> e : exps) {
                    AtlasExperiment aexp = AtlasDao.getExperimentByIdDw(String.valueOf(e.getKey()));
                    if(aexp != null) {
                        JSONObject jsExp = new JSONObject();
                        jsExp.put("accession", aexp.getDwExpAccession());
                        jsExp.put("name", aexp.getDwExpDescription());
                        jsExp.put("id", e.getKey());

                        boolean wasup = false;
                        boolean wasdn = false;
                        JSONArray jsEfs = new JSONArray();
                        for(Map.Entry<String, List<Experiment>> ef : e.getValue().entrySet()) {
                            JSONObject jsEf = new JSONObject();
                            jsEf.put("ef", ef.getKey());
                            jsEf.put("eftext", CuratedTexts.get("head.ef." + ef.getKey()));

                            JSONArray jsEfvs = new JSONArray();
                            for(Experiment exp : ef.getValue()) {
                                JSONObject jsEfv = new JSONObject();
                                jsEfv.put("efv", exp.getEfv());
                                jsEfv.put("isup", exp.getExpression().isUp());
                                jsEfv.put("pvalue", exp.getPvalue());
                                jsEfvs.put(jsEfvs.length(), jsEfv);

                                if(exp.getExpression().isUp()) wasup = true;else wasdn = true;
                            }
                            jsEf.put("efvs", jsEfvs);                            
                            jsEfs.put(jsEfs.length(), jsEf);
                        }
                        jsExp.put("efs", jsEfs);

                        if(wasup) ++numUp;
                        if(wasdn) ++numDn;
                        jsExps.put(jsExps.length(), jsExp);
                    }
                }

                jsResult.put("experiments", jsExps);

                jsResult.put("numUp", numUp);
                jsResult.put("numDn", numDn);

            } catch(Exception e) {
                try {
                    jsResult.put("error", "Experiments processing error for gene " + String.valueOf(geneIdKey));

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw, true);
                    e.printStackTrace(pw);
                    pw.flush();
                    sw.flush();
                    jsResult.put("exception", sw.toString());
                } catch(JSONException jse) {
                    // nothing
                }
                log.error("Error", e);
            }

        }

        try {
            response.setContentType("text/plain");
            response.setCharacterEncoding("utf-8");
            jsResult.write(response.getWriter());
        }  catch(JSONException jse) {
            // do nothing
        }
    }

}
