package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 07/02/2011
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
class AbstractAtlasDAO {
    protected JdbcTemplate template;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }
}
