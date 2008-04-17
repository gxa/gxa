package ae3.model;

import org.apache.solr.common.SolrDocument;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Apr 17, 2008
 * Time: 9:31:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasGene {
    private String geneId;
    private String geneName;
    private String geneIdentifier;
    private String geneSpecies;
    private Map<String, List<String>> geneHighlights;

    public AtlasGene(SolrDocument geneDoc) {
        this.setGeneId((String) geneDoc.getFieldValue("gene_id"));
        this.setGeneName((String) geneDoc.getFieldValue("gene_name"));
        this.setGeneIdentifier((String) geneDoc.getFieldValue("gene_identifier"));
        this.setGeneSpecies(geneDoc.getFieldValues("gene_species"));
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public void setGeneIdentifier(String geneIdentifier) {
        this.geneIdentifier = geneIdentifier;
    }

    public void setGeneSpecies(Collection geneSpecies) {
        this.geneSpecies = (String) geneSpecies.toArray()[0];
    }

    public String getGeneSpecies() {
        return geneSpecies;
    }

    public String getGeneId() {
        return geneId;
    }

    public String getGeneName() {
        return geneName;
    }

    public String getGeneIdentifier() {
        return geneIdentifier;
    }

    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    public String getGeneHighlightStringForHtml() {
        Map<String, List<String>> hilites = this.getGeneHighlights();

        if(hilites == null)
            return "";

        Set<String> hls = new HashSet<String>();
        for (String hlf : hilites.keySet()) {
            hls.add(hlf + ": " + StringUtils.join(hilites.get(hlf), ";"));
        }

        if(hls.size() > 0)
            return StringUtils.join(hls,"<br/>");

        return "";
    }

    public Map<String, List<String>> getGeneHighlights() {
        return geneHighlights;
    }
}
