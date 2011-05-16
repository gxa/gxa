package uk.ac.ebi.gxa.dao;

import com.google.common.collect.ArrayListMultimap;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;

import java.util.List;

/**
 * TODO: Rename me to BioEntityDAO
 */
public interface BioEntityDAOInterface {
    List<BioEntity> getAllGenesFast();

    void getPropertiesForGenes(List<BioEntity> bioEntities);

    public int getGeneCount();

    public List<String> getSpeciesForExperiment(long experimentId);

    void setJdbcTemplate(JdbcTemplate template);

    ArrayListMultimap<Long, DesignElement> getAllDesignElementsForGene();
}
