package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.*;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "beTypeSeq")
    @SequenceGenerator(name = "beTypeSeq", sequenceName = "A2_BIOENTITYTYPE_SEQ", allocationSize = 1)
    private Long bioentitytypeid;
    private String name;
    @Column(name = "ID_FOR_INDEX")
    private int useForIndex;

    @OneToOne(cascade = CascadeType.ALL)
    private BioEntityProperty identifierProperty;

    @ManyToOne(cascade = CascadeType.ALL)
    private BioEntityProperty nameProperty;

    BioEntityType() {
    }

    public BioEntityType(Long id, String name,  int useForIndex) {
        this(id, name, useForIndex, new BioEntityProperty(null, name), new BioEntityProperty(null, name));
    }

    public BioEntityType(Long bioentitytypeid, String name, int useForIndex, BioEntityProperty identifierProperty, BioEntityProperty nameProperty) {
        this.bioentitytypeid = bioentitytypeid;
        this.name = name.toLowerCase();
        this.useForIndex = useForIndex;
        this.identifierProperty = identifierProperty;
        this.nameProperty = nameProperty;
    }

    public Long getId() {
        return bioentitytypeid;
    }

    public String getName() {
        return name;
    }

    public BioEntityProperty getIdentifierProperty() {
        return identifierProperty;
    }

    public BioEntityProperty getNameProperty() {
        return nameProperty;
    }

    public int isUseForIndex() {
        return useForIndex;
    }

    @Override
    public String toString() {
        return name;
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
