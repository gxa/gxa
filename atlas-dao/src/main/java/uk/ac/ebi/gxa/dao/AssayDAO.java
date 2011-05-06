package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.sql.ResultSet;
import java.sql.SQLException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class AssayDAO extends AbstractDAO<Assay> {
    private ExperimentDAO edao;
    private ArrayDesignDAO addao;

    public AssayDAO(JdbcTemplate template, ExperimentDAO edao, ArrayDesignDAO addao) {
        super(template);
        this.edao = edao;
        this.addao = addao;
    }

    @Override
    protected String sequence() {
        return "A2_ONTOLOGY_SEQ";
    }

    @Override
    protected void save(Assay o) {
        int rows = template.update("insert into a2_assay (" + AssayMapper.FIELDS + ") " +
                "values (?, ?, ?, ?)", o.getAssayID(), o.getAccession(),
                o.getExperiment().getId(), o.getArrayDesign().getArrayDesignID());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + o + " - assays are supposed to be immutable");
    }

    @Override
    public Assay getById(long id) {
        return template.queryForObject("select " + AssayMapper.FIELDS + " from a2_assay " +
                "where assayid = ?",
                new Object[]{id},
                new AssayMapper());
    }

    private  class AssayMapper implements RowMapper<Assay> {
        private static final String FIELDS = "ASSAYID, ACCESSION, EXPERIMENTID, ARRAYDESIGNID";

        public Assay mapRow(ResultSet rs, int i) throws SQLException {
            return new Assay(rs.getLong(1), rs.getString(2), edao.getById(rs.getLong(3)), addao.getById(rs.getLong(4)));
        }
    }
}
