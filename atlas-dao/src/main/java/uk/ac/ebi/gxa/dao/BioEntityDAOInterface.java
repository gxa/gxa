package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.List;

/**
 * TODO: Rename me to BioEntityDAO
 */
public interface BioEntityDAOInterface {
    List<Gene> getAllGenesFast();

//    Gene getGeneById(Long id);

    List<Gene> getGenesByExperimentAccession(String exptAccession);

    void getPropertiesForGenes(List<Gene> genes);

    //ToDo: remove when gene indexer is tested on a bigger DE set
    List<DesignElement> getDesignElementsByGeneID(long geneID);

    public int getGeneCount();

    public List<String> getSpeciesForExperiment(long experimentId);

    void setJdbcTemplate(JdbcTemplate template);
}
