package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO {
    private JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    
}
