package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Ontology;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import java.sql.ResultSet;
import java.sql.SQLException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class OntologyTermDAO extends AbstractDAO<OntologyTerm> {
    private OntologyDAO odao;

    public OntologyTermDAO(JdbcTemplate template, OntologyDAO odao) {
        super(template);
        this.odao = odao;
    }

    @Override
    protected String sequence() {
        return "A2_ONTOLOGYTERM_SEQ";
    }

    @Override
    protected void save(OntologyTerm term) {
        int rows = template.update("insert into a2_ontologyterm (" + OntologyTermMapper.FIELDS + ") " +
                "values (?, ?, ?, ?, ?)", term.getId(), term.getOntology().getId(), term.getTerm(), term.getAccession(), term.getDescription());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + term + " - properties are supposed to be immutable");
    }

    @Override
    public OntologyTerm getById(long id) {
        return template.queryForObject("select " + OntologyTermMapper.FIELDS + " " +
                "from a2_ontologyterm " +
                "where ontologytermid = ?",
                new Object[]{id},
                new OntologyTermMapper());
    }

    private class OntologyTermMapper implements RowMapper<OntologyTerm> {
        private static final String FIELDS = "ONTOLOGYTERMID, ONTOLOGYID, TERM, ACCESSION, DESCRIPTION";

        public OntologyTerm mapRow(ResultSet rs, int i) throws SQLException {
            Ontology ontology = odao.getById(rs.getLong(2));
            return new OntologyTerm(rs.getLong(1), ontology, rs.getString(3), rs.getString(4), rs.getString(5));
        }
    }
}
