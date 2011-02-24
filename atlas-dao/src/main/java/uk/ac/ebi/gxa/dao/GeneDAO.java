package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Olga Melnichuk
 *         Date: 23/02/2011
 */
public class GeneDAO {

    private static final String SELECT_GENE_BY_PREFIX_AND_OFFSET = "SELECT GENEID, IDENTIFIER, NAME, ORGANISMID FROM ( " +
            "    SELECT ROW_NUMBER() OVER(ORDER BY name) LINENUM, GENEID, IDENTIFIER, NAME, ORGANISMID " +
            "    FROM A2_GENE " +
            "    WHERE LOWER(NAME) LIKE ? " +
            "    ORDER BY name " +
            ") " +
            "WHERE LINENUM BETWEEN ? AND ?";

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<Gene> getGenes(String prefix, int offset, int limit) {
        return jdbcTemplate.query(
                SELECT_GENE_BY_PREFIX_AND_OFFSET,
                new Object[]{prefix.toLowerCase() + "%", offset, offset + limit - 1},
                new RowMapper<Gene>() {
                    public Gene mapRow(ResultSet resultSet, int i) throws SQLException {
                        Gene gene = new Gene();

                        gene.setGeneID(resultSet.getLong(1));
                        gene.setIdentifier(resultSet.getString(2));
                        gene.setName(resultSet.getString(3));
                        gene.setSpecies(resultSet.getString(4));

                        return gene;
                    }
                });
    }
}
