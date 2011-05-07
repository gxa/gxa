package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.ObjectWithProperties;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.base.Joiner.on;

public class ObjectPropertyDAO extends AbstractDAO<Property> {
    private AbstractDAO<? extends ObjectWithProperties> ownerDAO;
    private final PropertyValueDAO propertyValueDAO;

    public ObjectPropertyDAO(JdbcTemplate template, PropertyValueDAO propertyValueDAO) {
        super(template);
        this.propertyValueDAO = propertyValueDAO;
    }

    void setOwnerDAO(AbstractDAO<? extends ObjectWithProperties> ownerDAO) {
        this.ownerDAO = ownerDAO;
    }

    @Override
    protected Property loadById(long id) {
        return template.queryForObject("select " + fields() + " from " + table() +
                " where " + pk() + " = ?", new ObjectPropertyMapper());
    }

    public List<Property> getByOwner(ObjectWithProperties owner) {
        return template.query("select " + fields() + " from " + table() +
                " where " + ownerid() + " = ?",
                new Object[]{owner.getId()},
                new ObjectPropertyMapper());
    }

    @Override
    protected String sequence() {
        return "A2_" + type() + "PV_SEQ";
    }

    @Override
    protected void save(Property object) {

    }

    private String type() {
        String daoname = ownerDAO.getClass().getSimpleName();
        return daoname.substring(0, daoname.indexOf("DAO"));
    }


    private String pk() {
        return type() + "PVID";
    }

    private String table() {
        return "A2_" + type() + "PV";
    }

    private String ownerid() {
        return type() + "ID";
    }

    private String fields() {
        return on(",").join(pk(), ownerid(), "PROPERTYVALUEID");
    }

    private class ObjectPropertyMapper implements RowMapper<Property> {
        public Property mapRow(ResultSet rs, int rowNum) throws SQLException {
            ObjectWithProperties owner = ownerDAO.getById(rs.getLong(2));
            PropertyValue pv = propertyValueDAO.getById(rs.getLong(3));
            Property property = new Property(rs.getLong(1), owner, pv, OntologyTerm.parseTerms(""));
            registerObject(property.getId(), property);
            // TODO: 4alf: Ontology mapping should go here
            return property;
        }
    }
}
