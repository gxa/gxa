package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Ontology;

import java.sql.ResultSet;
import java.sql.SQLException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class OntologyDAO extends AbstractDAO<Ontology> {
    public OntologyDAO(JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String sequence() {
        return "A2_ONTOLOGY_SEQ";
    }

    @Override
    protected void save(Ontology o) {
        int rows = template.update("insert into a2_ontology (" + OntologyMapper.FIELDS + ") " +
                "values (?, ?, ?, ?, ?)", o.getId(), o.getName(), o.getSourceUri(), o.getDescription(), o.getVersion());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + o + " - ontologies are supposed to be immutable");
    }

    @Override
    public Ontology getById(long id) {
        return template.queryForObject("select " + OntologyMapper.FIELDS + " from a2_ontology " +
                "where ontology = ?",
                new Object[]{id},
                new OntologyMapper());
    }

    private static class OntologyMapper implements RowMapper<Ontology> {
        private static final String FIELDS = "ONTOLOGYID, NAME, SOURCE_URI, DESCRIPTION, VERSION";

        public Ontology mapRow(ResultSet rs, int i) throws SQLException {
            return new Ontology(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
        }
    }
}
