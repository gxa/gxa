package ae3.service.structuredquery;

import java.util.List;
import java.util.ArrayList;

/**
 * @author pashky
 */
public class GeneProperties {
    public static enum PropType {
        NAME(5), ID(3), DESC(-1);

        public int limit;
        private PropType(int limit) {
            this.limit = limit;
        }
    }

    public static class Prop {
        public Prop(String id, String facetField, String searchField, PropType type, boolean drilldown) {
            this.id = id;
            this.facetField = facetField;
            this.searchField = searchField;
            this.type = type;
            this.drilldown = drilldown;
        }

        public String id;
        public String facetField;
        public String searchField;
        public PropType type;
        public boolean drilldown;
    }

    private static final Prop[] GENE_PROPS = {
        new Prop("dbxref", "gene_dbxref", "gene_dbxref", PropType.ID, false),
        new Prop("embl", "gene_embl", "gene_embl", PropType.ID, false),
        new Prop("identifier", "gene_identifier", "gene_identifier", PropType.ID, false),
        new Prop("ensfamily", "gene_ensfamily", "gene_ensfamily", PropType.ID, false),
        new Prop("ensgene", "gene_ensgene", "gene_ensgene", PropType.ID, false),
        new Prop("ensprotein", "gene_ensprotein", "gene_ensprotein", PropType.ID, false),
        new Prop("enstranscript", "gene_enstranscript", "gene_enstranscript", PropType.ID, false),
        new Prop("goid", "gene_goid", "gene_goid", PropType.ID, false),
        new Prop("image", "gene_image", "gene_image", PropType.ID, false),
        new Prop("interproid", "gene_interproid", "gene_interproid", PropType.ID, false),
        new Prop("locuslink", "gene_locuslink", "gene_locuslink", PropType.ID, false),
        new Prop("omimid", "gene_omimid", "gene_omimid", PropType.ID, false),
        new Prop("orf", "gene_orf", "gene_orf", PropType.ID, false),
        new Prop("ortholog", "gene_ortholog", "gene_ortholog", PropType.ID, false),
        new Prop("refseq", "gene_refseq", "gene_refseq", PropType.ID, false),
        new Prop("unigene", "gene_unigene", "gene_unigene", PropType.ID, false),
        new Prop("uniprot", "gene_uniprot", "gene_uniprot", PropType.ID, false),
        new Prop("hmdb", "gene_hmdb", "gene_hmdb", PropType.ID, false),
        new Prop("chebi", "gene_chebi", "gene_chebi", PropType.ID, false),
        new Prop("cas", "gene_cas", "gene_cas", PropType.ID, false),
        new Prop("uniprotmetenz", "gene_uniprotmetenz", "gene_uniprotmetenz", PropType.ID, false),

        new Prop("name", "gene_name_exact", "gene_name", PropType.NAME, false),
        new Prop("synonym", "gene_synonym", "gene_synonym", PropType.NAME, false),

        new Prop("disease", "gene_disease_exact", "gene_disease", PropType.DESC, true),
        new Prop("goterm", "gene_goterm_exact", "gene_goterm", PropType.DESC, true),
        new Prop("interproterm", "gene_interproterm_exact", "gene_interproterm", PropType.DESC, true),
        new Prop("keyword", "gene_keyword_exact", "gene_keyword", PropType.DESC, true),
        new Prop("protein", "gene_protein_exact", "gene_protein", PropType.DESC, true),
    };

    public static String convertPropertyToFacetField(String id)
    {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.facetField;
        return null;
    }

    public static String convertPropertyToSearchField(String id)
    {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.searchField;
        return null;
    }

    public static Prop[] allProperties()
    {
        return GENE_PROPS;
    }

    public static Iterable<Prop> allDrillDowns()
    {
        List<Prop> s = new ArrayList<Prop>();
        for(Prop p : GENE_PROPS)
            if(p.drilldown)
                s.add(p);
        return s;
    }

    public static Iterable<String> allPropertyIds()
    {
        List<String> s = new ArrayList<String>();
        for(Prop p : GENE_PROPS)
            s.add(p.id);
        return s;
    }

    public static Prop findPropByFacetField(String field)
    {
        for(Prop p : GeneProperties.allProperties())
            if(p.facetField.equals(field))
                return p;
        return null;
    }

    public static boolean isNameProperty(String id) {
        for(Prop p : GENE_PROPS)
            if(p.id.equals(id))
                return p.type == PropType.NAME;
        return false;
    }
}
