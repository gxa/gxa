package uk.ac.ebi.gxa.dao;

import com.google.common.collect.ArrayListMultimap;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;

import java.util.List;

/**
 * TODO: Rename me to BioEntityDAO
 */
public interface BioEntityDAOInterface {
    List<BioEntity> getAllGenesFast();

//    Gene getGeneById(Long id);

    List<BioEntity> getGenesByExperimentAccession(String exptAccession);

    void getPropertiesForGenes(List<BioEntity> bioEntities);

    //ToDo: remove when gene indexer is tested on a bigger DE set
    List<DesignElement> getDesignElementsByGeneID(long geneID);

    public int getGeneCount();

    public List<String> getSpeciesForExperiment(long experimentId);

    void setJdbcTemplate(JdbcTemplate template);

    ArrayListMultimap<Long, DesignElement> getAllDesignElementsForGene();
}
