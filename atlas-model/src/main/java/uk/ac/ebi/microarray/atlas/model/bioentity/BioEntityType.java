package uk.ac.ebi.microarray.atlas.model.bioentity;

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
    private Long id;
    private String name;
    private boolean useForIndex;

    public BioEntityType(Long id, String name, boolean useForIndex) {
        this.id = id;
        this.name = name;
        this.useForIndex = useForIndex;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isUseForIndex() {
        return useForIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntityType that = (BioEntityType) o;

        if (useForIndex != that.useForIndex) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + (useForIndex ? 1 : 0);
        return result;
    }
}
