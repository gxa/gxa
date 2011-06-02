package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
@Entity
public class BioEntityType {
    public static final String ENSTRANSCRIPT = "enstranscript";
    public static final String ENSGENE = "ensgene";
    public static final String MICRORNA = "microRNA";
    public static final String OTHER = "other";
    public static final String GENE_DM = "gene_dm";
    public static final String ENSGENE_ATL = "ensgene_atl";
    public static final String METABOLOM = "metabolom";
    public static final String UNIPRPOT = "uniprot_acc";
    public static final String DE_ATL = "designelement_atl";

    @Id
    private Long bioentitytypeid;
    private String name;
    @Column(name = "ID_FOR_INDEX")
    private int useForIndex;

    BioEntityType() {
    }

    public BioEntityType(Long id, String name, int useForIndex) {
        this.bioentitytypeid = id;
        this.name = name;
        this.useForIndex = useForIndex;
    }

    public Long getId() {
        return bioentitytypeid;
    }

    public String getName() {
        return name;
    }

    public int isUseForIndex() {
        return useForIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntityType that = (BioEntityType) o;

        if (useForIndex != that.useForIndex) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + useForIndex;
        return result;
    }
}
