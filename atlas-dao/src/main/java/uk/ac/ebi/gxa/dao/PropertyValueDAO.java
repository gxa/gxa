package uk.ac.ebi.gxa.dao;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.PropertyDefinition;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class PropertyValueDAO extends AbstractDAO<PropertyValue> {
    private PropertyDAO pdao;

    public PropertyValueDAO(JdbcTemplate template, PropertyDAO pdao) {
        super(template);
        this.pdao = pdao;
    }

    public Collection<PropertyValue> getAllPropertyValues() {
        return template.query("SELECT " + PropertyValueMapper.FIELDS + " FROM a2_propertyvalue",
                new PropertyValueMapper());
    }

    public PropertyValue getOrCreate(String name, String value) {
        PropertyDefinition pd = pdao.getOrCreate(name);
        try {
            return template.queryForObject("select " + PropertyValueMapper.FIELDS + " from a2_propertyvalue " +
                    "where propertyid = ? and name = ?", new Object[]{pd.getId(), value},
                    new PropertyValueMapper());
        } catch (IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() != 0)
                throw LogUtil.createUnexpected("duplicated [" + e.getActualSize() + "]" +
                        " property value for " + pd + ", value='" + value + "'", e);
            PropertyValue pv = new PropertyValue(nextId(), pd, value);
            save(pv);
            return pv;
        }
    }

    @Override
    public PropertyValue getById(long id) {
        return template.queryForObject("select " + PropertyValueMapper.FIELDS + " from a2_propertyvalue " +
                "where propertyvalueid = ?", new Object[]{id},
                new PropertyValueMapper());
    }

    @Override
    protected String sequence() {
        return "A2_PROPERTYVALUE_SEQ";
    }

    @Override
    protected void save(PropertyValue pv) {
        template.update("insert into a2_propertyvalue (" + PropertyValueMapper.FIELDS + ") " +
                "values (?,?,?)", pv.getId(), pv.getDefinition().getId(), pv.getValue());
    }

    private class PropertyValueMapper implements RowMapper<PropertyValue> {
        private static final String FIELDS = "propertyvalueid, propertyid, name";

        public PropertyValue mapRow(ResultSet rs, int i) throws SQLException {
            return new PropertyValue(rs.getLong(1),
                    pdao.getById(rs.getLong(2)), rs.getString(3));
        }
    }
}
