package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.BioEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Olga Melnichuk
 */
public class GeneDAO {
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<BioEntity> getGenes(String prefix, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT GENEID, IDENTIFIER, NAME, ORGANISMID FROM ( " +
                        "    SELECT ROW_NUMBER() OVER(ORDER BY name) LINENUM, GENEID, IDENTIFIER, NAME, ORGANISMID " +
                        "    FROM A2_GENE " +
                        "    WHERE LOWER(NAME) LIKE ? " +
                        "    ORDER BY name " +
                        ") " +
                        "WHERE LINENUM BETWEEN ? AND ?",
                new Object[]{prefix.toLowerCase() + "%", offset, offset + limit - 1},
                new RowMapper<BioEntity>() {
                    public BioEntity mapRow(ResultSet resultSet, int i) throws SQLException {
                        BioEntity gene = new BioEntity(resultSet.getString(2));

                        gene.setId(resultSet.getLong(1));
                        gene.setName(resultSet.getString(3));
                        gene.setSpecies(resultSet.getString(4));

                        return gene;
                    }
                });
    }
}
