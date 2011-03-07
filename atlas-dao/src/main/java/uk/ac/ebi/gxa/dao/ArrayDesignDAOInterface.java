package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 07/03/2011
 * Time: 13:09
 * To change this template use File | Settings | File Templates.
 */
public interface ArrayDesignDAOInterface {
    List<ArrayDesign> getAllArrayDesigns();

    ArrayDesign getArrayDesignByAccession(String accession);

    ArrayDesign getArrayDesignShallowByAccession(String accession);

    void setJdbcTemplate(JdbcTemplate template);
}
