package uk.ac.ebi.gxa.requesthandlers.genepage;

import ae3.anatomogram.Anatomogram;
import ae3.anatomogram.Annotator;
import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class AnatomogramRequestHandler implements HttpRequestHandler {
    private AtlasSolrDAO atlasSolrDAO;
    private Efo efo;
    private Annotator annotator;
    private String organism;
    private Annotator.AnatomogramType anatomogramType = Annotator.AnatomogramType.Das;

    public void setAtlasSolrDAO(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
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

    public static class Annotation {
        public Annotation(String id, String caption, int up, int dn) {
            this.id = id;
            this.caption = caption;
            this.up = up;
            this.dn = dn;
        }

        public String id;
        public String caption;
        public int up;
        public int dn;
    }

    public static Annotation newAnnotation(String id, String caption, int up, int dn) {
        return new Annotation(id, caption, up, dn);
    }

    public Anatomogram getAnatomogram(String geneId, Annotator.AnatomogramType anatomogramType) {
        AtlasSolrDAO.AtlasGeneResult geneResult = atlasSolrDAO.getGeneByIdentifier(geneId);
        if (!geneResult.isFound()) {//not found
            return annotator.getEmptyAnatomogram();
        }

        return annotator.getAnatomogram(anatomogramType, geneResult.getGene());
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String geneId = request.getParameter("gid");
        this.anatomogramType = Annotator.AnatomogramType.Das;

        if (null != request.getParameter("type"))
            if ("web".equalsIgnoreCase(request.getParameter("type"))) {
                this.anatomogramType = Annotator.AnatomogramType.Web;
            }

        if (Strings.isNullOrEmpty(geneId)) {
            ErrorResponseHelper.errorNotFound(request, response, "Cannot process anatomogram request without a gene identifier!");
            return;
        }

        try {
            Anatomogram an = getAnatomogram(geneId, this.anatomogramType);
            if (response != null) {
                response.setContentType("image/png");
                an.writePngToStream(response.getOutputStream());
            }
        } catch (IllegalArgumentException e) {
            log.info("Failed to process anatomogram: " + e.getMessage());
        } catch (Exception ex) {
            log.error("Error!", ex);
        }
    }
}
