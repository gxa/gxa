package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.utils.LazyList;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class AssayDAO extends AbstractDAO<Assay> {
    private final ExperimentDAO edao;
    private final ArrayDesignDAO addao;
    private final SampleDAO sdao;

    public AssayDAO(JdbcTemplate template, ExperimentDAO edao, ArrayDesignDAO addao, SampleDAO sdao) {
        super(template);
        this.edao = edao;
        this.addao = addao;
        this.sdao = sdao;
    }

    @Override
    protected String sequence() {
        return "A2_ASSAY_SEQ";
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

    private class AssayMapper implements RowMapper<Assay> {
        private static final String FIELDS = "ASSAYID, ACCESSION, EXPERIMENTID, ARRAYDESIGNID";

        public Assay mapRow(ResultSet rs, int i) throws SQLException {
            final Assay assay = new Assay(rs.getLong(1), rs.getString(2), edao.getById(rs.getLong(3)), addao.getById(rs.getLong(4)));
            assay.setSamples(new LazyList<Sample>(new Callable<List<Sample>>() {
                @Override
                public List<Sample> call() throws Exception {
                    return sdao.getByAssay(assay);
                }
            }));
            return assay;
        }
    }
}
