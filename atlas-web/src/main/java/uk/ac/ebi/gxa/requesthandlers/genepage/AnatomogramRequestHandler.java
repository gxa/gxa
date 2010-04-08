package uk.ac.ebi.gxa.requesthandlers.genepage;

import ae3.anatomogram.Annotator;
import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public List<Annotation> getAnnotations(String geneIdentifier) {
        List<Annotation> result = new ArrayList<Annotation>();

        AtlasSolrDAO.AtlasGeneResult geneResult = getAtlasSolrDAO().getGeneByIdentifier(geneIdentifier);
        if (geneResult.isFound()) {
            AtlasGene gene = geneResult.getGene();

            for (String acc : Arrays.asList("EFO_0000302","EFO_0000792","EFO_0000800","EFO_0000943","EFO_0000110"
                            ,"EFO_0000265","EFO_0000815","EFO_0000803","EFO_0000793","EFO_0000827"
                ,"EFO_0000889","EFO_0000934","EFO_0000935","EFO_0000968","EFO_0001385","EFO_0001412"
                ,"EFO_0001413","EFO_0001937")) {
                
                EfoTerm term = getEfo().getTermById(acc);

                int dn = gene.getCount_dn(acc);
                int up = gene.getCount_up(acc);

                if((dn>0)||(up>0))
                    result.add(new Annotation(acc, term.getTerm(), up, dn));
            }
        }
        return result;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String geneId = request.getParameter("gid");

        if (geneId != null || !"".equals(geneId)) {
            //Annotator annotator = new Annotator();
            //InputStream stream = getClass().getResourceAsStream("/Human_Male.svg");

            try {
                //annotator.setTemplate(stream);
                //annotator.setAnnotations(getAnnotations(geneId));

                response.setContentType("image/png");

                annotator.process(getAnnotations(geneId), Annotator.Encoding.Png /*Png,Jpeg*/, response.getOutputStream());
            }
            catch(Exception ex){
                log.error("Cannot process anatomogram",ex);
            } finally{
                //if(null != stream)
                //    stream.close();
            }
        } else {
            ErrorResponseHelper.errorNotFound(request, response, "There are no records for gene " + String.valueOf(geneId));
        }
    }
}
