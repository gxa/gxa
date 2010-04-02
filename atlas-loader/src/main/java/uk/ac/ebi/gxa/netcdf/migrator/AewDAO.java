package uk.ac.ebi.gxa.netcdf.migrator;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * Special migrator class for reading data from the old db
 */
public class AewDAO {
    private JdbcTemplate jdbcTemplate;

    private final static String EXPRESSIONVALUES_BY_EXPERIMENT_AND_ARRAYDESIGN =
            "SELECT ev.assay_id, ev.designelement_identifier, nvl(ev.absolute, ev.ratio) " +
            "FROM ae2__expressionvalue__main ev " +
            "WHERE ev.experiment_id=? AND ev.arraydesign_id=? " +
            "ORDER BY ev.designelement_identifier, ev.assay_id";

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Executes a ResultSetExtractor on all expression values retrieved for experiment/arraydesign
     * pair, returning nothing.
     * @param experimentId experiment id
     * @param arraydesignId arraydesign id
     * @param rse ResultSetExtractor object
     */
    public void processExpressionValues(long experimentId, long arraydesignId, ResultSetExtractor rse) {
        jdbcTemplate.query(EXPRESSIONVALUES_BY_EXPERIMENT_AND_ARRAYDESIGN,
                new Object[] {experimentId, arraydesignId},
                rse);
    }
}
