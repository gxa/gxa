package ae3.model;

import org.apache.solr.common.SolrDocument;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.*;

import ae3.util.HtmlHelper;
import ae3.util.QueryHelper;

public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;

    public AtlasGene(SolrDocument geneDoc) {
        this.geneSolrDocument = geneDoc;
    }

    public String getGeneSpecies() {
        String species = (String) geneSolrDocument.getFieldValues("gene_species").toArray()[0];
        return species.substring(0, 1).toUpperCase() + species.substring(1, species.length()).toLowerCase();
    }

    private String getValue(String name)
    {
        return StringUtils.join(geneSolrDocument.getFieldValues(name), ", ");
    }

    public String getGeneId() {
        return getValue("gene_id");
    }

    public String getGeneName() {
        return getValue("gene_name");
    }

    public String getGeneIdentifier() {
        return getValue("gene_identifier");
    }

    public String getGoTerm() {
        return getValue("gene_goterm");
    }

    public String getInterProTerm() {
        return getValue("gene_interproterm");
    }

    private String getHilitValue(String name) {
        List<String> val = geneHighlights.get(name);
        if(val == null || val.size() == 0)
            return StringEscapeUtils.escapeHtml(getValue(name));
        return StringUtils.join(val, ", ");
    }

    public String getHilitInterProTerm() {
        return getHilitValue("gene_interproterm");
    }

    public String getHilitGoTerm() {
        return getHilitValue("gene_goterm");
    }

    public String getHilitGeneName() {
        return getHilitValue("gene_name");
    }

    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    public String getGeneHighlightStringForHtml() {

        if(geneHighlights == null)
            return "";

        Set<String> hls = new HashSet<String>();
        for (String hlf : geneHighlights.keySet()) {
            hls.add(hlf + ": " + StringUtils.join(geneHighlights.get(hlf), ";"));
        }

        if(hls.size() > 0)
            return StringUtils.join(hls,"<br/>");

        return "";
    }

    public HashMap serializeForWebServices() {
        HashMap h = new HashMap();
        SolrDocument gene = this.getGeneSolrDocument();

        if(gene != null) {
            Map m = gene.getFieldValuesMap();
            for (Object key : m.keySet()) {
                Collection<String> s = (Collection<String>) m.get(key);
                h.put(key, StringUtils.join(s, "\t"));
            }
        }

        return h;
    }
}
