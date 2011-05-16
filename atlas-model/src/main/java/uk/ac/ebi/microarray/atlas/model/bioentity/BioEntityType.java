package uk.ac.ebi.microarray.atlas.model.bioentity;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public enum BioEntityType {
    ENSTRANSCRIPT("enstranscript"),
    ENSGENE("ensgene"),
    MICRORNA("microRNA"),
    OTHER("other"),

    //ToDo: should be removed when all db references are updated
    GENE_DM("gene_dm"),
    ENSGENE_ATL("ensgene_atl"),
    METABOLOM("metabolom"),
    UNIPRPOT("uniprot_acc"),
    DE_ATL("designelement_atl");

    private String value;

    BioEntityType(String name) {
        this.value = name;
    }

    public String value() {
        return value;
    }

    public static BioEntityType parse(String s) {
        try {
            return BioEntityType.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return OTHER;
        }
    }
}
