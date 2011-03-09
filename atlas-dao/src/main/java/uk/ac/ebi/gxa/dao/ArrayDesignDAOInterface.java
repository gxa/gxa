package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.util.List;

/**
 * TODO: rename me to ArrayDesignDAO
 *
 * @author Nataliya Sklyar
 */
public interface ArrayDesignDAOInterface {
    List<ArrayDesign> getAllArrayDesigns();

    ArrayDesign getArrayDesignByAccession(String accession);

    ArrayDesign getArrayDesignShallowByAccession(String accession);

    void setJdbcTemplate(JdbcTemplate template);
}
