package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class SampleDAO extends AbstractDAO<Sample> {
    private final OrganismDAO odao;

    public SampleDAO(JdbcTemplate template, OrganismDAO odao) {
        super(template);
        this.odao = odao;
    }

    @Override
    protected String sequence() {
        return "A2_SAMPLE_SEQ";
    }

    @Override
    protected void save(Sample sample) {
        int rows = template.update("insert into a2_sample (" + SampleMapper.CLEAN_FIELDS + ") " +
                "values (?, ?, ?, ?)", sample.getSampleID(), sample.getAccession(), sample.getOrganism().getId(), sample.getChannel());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + sample + " - organisms are supposed to be immutable");
    }

    @Override
    public Sample getById(long id) {
        return template.queryForObject("select " + SampleMapper.FIELDS + " from a2_sample s " +
                "where s.sampleid = ?",
                new Object[]{id},
                new SampleMapper());
    }

    public List<Sample> getByAssay(Assay assay) {
        return template.query("select " + SampleMapper.FIELDS + " from a2_sample s " +
                "join a2_assaysample ass on s.sampleid = ass.sampleid " +
                "where ass.assayid = ?",
                new Object[]{assay.getAssayID()},
                new SampleMapper());
    }

    private class SampleMapper implements RowMapper<Sample> {
        private static final String CLEAN_FIELDS = "SAMPLEID, ACCESSION, ORGANISMID, CHANNEL";
        private static final String FIELDS = "s.SAMPLEID, s.ACCESSION, s.ORGANISMID, s.CHANNEL";

        public Sample mapRow(ResultSet rs, int i) throws SQLException {
            return new Sample(rs.getLong(1), rs.getString(2), odao.getById(rs.getLong(3)), rs.getString(4));
        }
    }
}
