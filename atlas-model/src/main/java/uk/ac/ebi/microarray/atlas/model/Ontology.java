package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
public class Ontology {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ontologySeq")
    @SequenceGenerator(name = "ontologySeq", sequenceName = "A2_ONTOLOGY_SEQ")
    private Long ontologyid;
    private String name;
    @Column(name = "SOURCE_URI")
    private String sourceUri;
    private String description;
    private String version;

    Ontology() {
    }

    public Ontology(Long id, String name, String sourceUri, String description, String version) {
        this.ontologyid = id;
        this.name = name;
        this.sourceUri = sourceUri;
        this.description = description;
        this.version = version;
    }

    public Long getId() {
        return ontologyid;
    }

    public String getName() {
        return name;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }
}