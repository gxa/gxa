package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.RowCallbackHandler;
import uk.ac.ebi.microarray.atlas.model.ObjectWithProperties;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class ObjectPropertyMapper implements RowCallbackHandler {
    private Map<Long, ? extends ObjectWithProperties> objectsById;
    private PropertyValueDAO propertyValueDAO;

    public ObjectPropertyMapper(Map<Long, ? extends ObjectWithProperties> objectsById, PropertyValueDAO propertyValueDAO) {
        this.objectsById = objectsById;
        this.propertyValueDAO = propertyValueDAO;
    }

    public void processRow(ResultSet rs) throws SQLException {
        final ObjectWithProperties owner = objectsById.get(rs.getLong(2));
        PropertyValue pv = propertyValueDAO.getById(rs.getLong(3));
        Property property = new Property(rs.getLong(1), owner, pv, OntologyTerm.parseTerms(rs.getString(4)));
        owner.addProperty(property);
    }
}
