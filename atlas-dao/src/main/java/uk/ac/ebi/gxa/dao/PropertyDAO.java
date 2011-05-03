package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;

public class PropertyDAO {
    private JdbcTemplate template;

    public PropertyDAO(JdbcTemplate template) {
        this.template = template;
    }
}
