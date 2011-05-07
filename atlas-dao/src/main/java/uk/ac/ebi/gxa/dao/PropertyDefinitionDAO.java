package uk.ac.ebi.gxa.dao;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.PropertyDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class PropertyDefinitionDAO extends AbstractDAO<PropertyDefinition> {
    public PropertyDefinitionDAO(JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String sequence() {
        return "A2_PROPERTY_SEQ";
    }

    @Override
    protected void save(PropertyDefinition pd) {
        int rows = template.update("insert into a2_property (" + PropertyDefinitionMapper.FIELDS + ") " +
                "values (?, ?)", pd.getId(), pd.getName());
        if (rows != 1)
            throw createUnexpected("Cannot overwrite " + pd + " - properties are supposed to be immutable");
    }

    public List<PropertyDefinition> getAllPropertyDefinitions() {
        return template.query("SELECT " + PropertyDefinitionMapper.FIELDS + " FROM a2_property",
                new PropertyDefinitionMapper());
    }

    @Override
    protected PropertyDefinition loadById(long id) {
        return template.queryForObject("select " + PropertyDefinitionMapper.FIELDS + " " +
                "from a2_property " +
                "where propertyid = ?",
                new Object[]{id},
                new PropertyDefinitionMapper());
    }

    public PropertyDefinition getByName(String name) {
        return template.queryForObject("select " + PropertyDefinitionMapper.FIELDS + " " +
                "from a2_property " +
                "where name = ?",
                new Object[]{name},
                new PropertyDefinitionMapper());
    }

    public PropertyDefinition getOrCreate(String name) {
        try {
            return template.queryForObject("select " + PropertyDefinitionMapper.FIELDS + " " +
                    "from a2_property " +
                    "where name = ?",
                    new Object[]{name},
                    new PropertyDefinitionMapper());
        } catch (IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() != 0)
                throw createUnexpected("duplicated [" + e.getActualSize() + "]" +
                        " property for name ='" + name + "'", e);
            PropertyDefinition pd = new PropertyDefinition(nextId(), name);
            save(pd);
            return pd;
        }
    }

    private class PropertyDefinitionMapper implements RowMapper<PropertyDefinition> {
        private static final String FIELDS = "propertyid, name";

        public PropertyDefinition mapRow(ResultSet rs, int i) throws SQLException {
            PropertyDefinition propertyDefinition = new PropertyDefinition(rs.getLong(1), rs.getString(2));
            registerObject(propertyDefinition.getId(), propertyDefinition);
            return propertyDefinition;
        }
    }
}
