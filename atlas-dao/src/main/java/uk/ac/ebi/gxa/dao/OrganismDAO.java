package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Organism;

import java.sql.ResultSet;
import java.sql.SQLException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class OrganismDAO extends AbstractDAO<Organism> {
    public OrganismDAO(JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String sequence() {
        return "A2_ORGANISM_SEQ";
    }

    @Override
    protected void save(Organism o) {
        int rows = template.update("insert into a2_organism (" + OrganismMapper.FIELDS + ") " +
                "values (?, ?)", o.getId(), o.getName());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + o + " - organisms are supposed to be immutable");
    }

    @Override
    public Organism getById(long id) {
        return template.queryForObject("select " + OrganismMapper.FIELDS + " from a2_organism " +
                "where organismid = ?",
                new Object[]{id},
                new OrganismMapper());
    }

    private static class OrganismMapper implements RowMapper<Organism> {
        private static final String FIELDS = "ONTOLOGYID, NAME, SOURCE_URI, DESCRIPTION, VERSION";

        public Organism mapRow(ResultSet rs, int i) throws SQLException {
            return new Organism(rs.getLong(1), rs.getString(2));
        }
    }
}
