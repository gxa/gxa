package uk.ac.ebi.gxa.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 12/04/2011
 * Time: 15:01
 * To change this template use File | Settings | File Templates.
 */
public class EnsemblAnnotationDAO {

    private static Logger log = LoggerFactory.getLogger(EnsemblAnnotationDAO.class);

    protected JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public Map<String, String> getOrganismToEnsOrgName() {
        final Map<String, String> answer = new HashMap<String, String>();

        template.query("SELECT name, ensname FROM a2_organism WHERE ensname is not NULL", new RowMapper<Object>() {
            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                answer.put(rs.getString(1), rs.getString(2));
                return rs.getString(1);
            }
        });

        return answer;
    }

    public Map<String, String> getPropertyToEnsPropName() {
        final Map<String, String> answer = new HashMap<String, String>();

        template.query("SELECT name, ensname FROM a2_bioentityproperty ", new RowMapper<Object>() {
            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                answer.put(rs.getString(1), rs.getString(2));
                return rs.getString(1);
            }
        });

        return answer;
    }
}
