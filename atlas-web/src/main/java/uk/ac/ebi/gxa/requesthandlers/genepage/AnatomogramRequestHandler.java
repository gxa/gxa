package uk.ac.ebi.gxa.requesthandlers.genepage;

import ae3.anatomogram.Annotator;
import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.QueryExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Mar 25, 2010
 * Time: 1:37:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnatomogramRequestHandler implements HttpRequestHandler {
    private AtlasSolrDAO atlasSolrDAO;
    private Efo efo;
    private Annotator annotator;
    private String organism;
    private Annotator.AnatomogramType anatomogramType = Annotator.AnatomogramType.Das;

    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public AtlasSolrDAO getAtlasSolrDAO() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDAO(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public Efo getEfo() {
        return efo;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public Annotator getAnnotator() {
        return annotator;
    }

    public void setAnnotator(Annotator annotator) {
        this.annotator = annotator;
    }

    public class Annotation {
        public Annotation(String id, String caption, int up, int dn, int nonde) {
            this.id = id;
            this.caption = caption;
            this.up = up;
            this.dn = dn;
            this.nonde = nonde;
        }

        public String id;
        public String caption;
        public int up;
        public int dn;
        public int nonde;
    }

    public List<Annotation> getAnnotations(String geneIdentifier) {
        List<Annotation> result = new ArrayList<Annotation>();

        AtlasSolrDAO.AtlasGeneResult geneResult = getAtlasSolrDAO().getGeneByIdentifier(geneIdentifier);
        if (geneResult.isFound()) {
            AtlasGene gene = geneResult.getGene();

            /*Arrays.asList("EFO_0000302","EFO_0000792","EFO_0000800","EFO_0000943","EFO_0000110"
                ,"EFO_0000265","EFO_0000815","EFO_0000803","EFO_0000793","EFO_0000827"
                ,"EFO_0000889","EFO_0000934","EFO_0000935","EFO_0000968","EFO_0001385","EFO_0001412"
                ,"EFO_0001413","EFO_0001937")*/
            this.organism = gene.getGeneSpecies();
            Long geneId = Long.parseLong(gene.getGeneId());
            for (String acc : annotator.getKnownEfo(this.anatomogramType, this.organism)) {

                EfoTerm term = getEfo().getTermById(acc);
                boolean isEfo = AtlasStatisticsQueryService.EFO_QUERY;

                int dn = atlasStatisticsQueryService.getExperimentCountsForGene(acc, StatisticsType.DOWN, isEfo, geneId, null);
                int up = atlasStatisticsQueryService.getExperimentCountsForGene(acc, StatisticsType.UP, isEfo, geneId, null);
                int nonde = atlasStatisticsQueryService.getExperimentCountsForGene(acc, StatisticsType.NON_D_E, isEfo, geneId, null);


                if (dn > 0 || up > 0 || nonde > 0)
                    result.add(new Annotation(acc, term.getTerm(), up, dn, nonde));
            }
        } else {//not found
            this.organism = "unknown";
            throw new IllegalArgumentException(String.format("gene not found : %1$s", geneIdentifier));
        }

        return result;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String geneId = request.getParameter("gid");
        this.anatomogramType = Annotator.AnatomogramType.Das;

        if (null != request.getParameter("type"))
            if (0 == request.getParameter("type").compareToIgnoreCase("web")) {
                this.anatomogramType = Annotator.AnatomogramType.Web;
            }

        if (!"".equals(geneId)) {
            try {
                List<Annotation> annotations = getAnnotations(geneId);

                if (null == annotations) {
                    ErrorResponseHelper.errorNotFound(request, response, "There are no records for gene " + geneId);
                    return;
                }

                if (null == response) {
                    annotator.process(this.organism, annotations, Annotator.Encoding.Png /*Png,Jpeg*/, null, this.anatomogramType);
                } else {
                    response.setContentType("image/png");
                    annotator.process(this.organism, annotations, Annotator.Encoding.Png /*Png,Jpeg*/, response.getOutputStream(), this.anatomogramType);
                }
            } catch (IllegalArgumentException e) {
                log.info("Failed to process anatomogram: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Error!", ex);
            }
        } else {
            ErrorResponseHelper.errorNotFound(request, response, "Cannot process anatomogram request without a gene identifier!");
        }
    }
}
