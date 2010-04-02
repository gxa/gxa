package uk.ac.ebi.gxa.netcdf.migrator;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Special migrator class for reading data from the old db
 */
public class AewDAO {
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
