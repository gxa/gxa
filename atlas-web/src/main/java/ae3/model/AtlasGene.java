package ae3.model;

import org.apache.solr.common.SolrDocument;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class AtlasGene {
    private String geneId;
    private String geneName;
    private String geneIdentifier;
    private String geneSpecies;

    private SolrDocument geneSolrDocument;

    private Map<String, List<String>> geneHighlights;

    private AtlasGene() {};

    public AtlasGene(SolrDocument geneDoc) {
        this.setGeneId((String) geneDoc.getFieldValue("gene_id"));
        this.setGeneName((String) geneDoc.getFieldValue("gene_name"));
        this.setGeneIdentifier((String) geneDoc.getFieldValue("gene_identifier"));
        this.setGeneSpecies(geneDoc.getFieldValues("gene_species"));

        this.setGeneSolrDocument(geneDoc);
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
        this.geneSpecies = this.geneSpecies.substring(0, 1)+this.geneSpecies.substring(1, this.geneSpecies.length()).toLowerCase();
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

    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    public void setGeneSolrDocument(SolrDocument geneSolrDocument) {
        this.geneSolrDocument = geneSolrDocument;
    }

    public HashMap serializeForWebServices() {
        HashMap h = new HashMap();
        Map m = this.getGeneSolrDocument().getFieldValuesMap();
        for (Object key : m.keySet()) {
            h.put(key, m.get(key));
        }

        return h;
    }
}
