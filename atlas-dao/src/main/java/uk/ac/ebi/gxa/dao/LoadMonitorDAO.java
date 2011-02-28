package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.LoadDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LoadMonitorDAO {
    private JdbcTemplate template;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public LoadDetails getLoadDetailsForExperimentsByAccession(String accession) {
        return getLoadDetails("SELECT " + LoadDetailsMapper.FIELDS + " FROM load_monitor " +
                "WHERE load_type='experiment' " +
                "AND accession=?", accession);
    }

    public LoadDetails getLoadDetailsForArrayDesignsByAccession(String accession) {
        return getLoadDetails("SELECT " + LoadDetailsMapper.FIELDS + " FROM load_monitor " +
                "WHERE load_type='arraydesign' " +
                "AND accession=?", accession);
    }

    private LoadDetails getLoadDetails(String query, String accession) {
        List results = template.query(query,
                new Object[]{accession},
                new LoadDetailsMapper());
        return results.size() > 0 ? (LoadDetails) results.get(0) : null;
    }

    private static class LoadDetailsMapper implements RowMapper<LoadDetails> {
        private static final String FIELDS = "status";

        public LoadDetails mapRow(ResultSet resultSet, int i) throws SQLException {
            LoadDetails details = new LoadDetails();
            details.setStatus(resultSet.getString(1));
            return details;
        }
    }
}
