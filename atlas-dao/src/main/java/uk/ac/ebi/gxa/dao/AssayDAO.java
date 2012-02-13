package uk.ac.ebi.gxa.dao;

import com.google.common.base.Strings;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

import java.util.List;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final String NAME_COL = "accession";

    private static String COMMON_HQL = "from Experiment e left join e.assays a left join a.properties p where p.propertyValue.property.name = ? ";

    public static final Logger log = LoggerFactory.getLogger(AssayDAO.class);

    public AssayDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Assay.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Assay a").get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Assay> getAssaysByProperty(String propertyName) {
        return template.find("select a " + COMMON_HQL, propertyName);
    }

    @SuppressWarnings("unchecked")
    public List<AssayProperty> getAssayPropertiesByProperty(String propertyName) {
        return template.find("select p " + COMMON_HQL, propertyName);
    }

    @SuppressWarnings("unchecked")
    public List<Assay> getAssaysByPropertyValue(String propertyName, String propertyValue) {
        return template.find("select a " + COMMON_HQL + " and p.propertyValue.value = ?", propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    public List<AssayProperty> getAssayPropertiesByPropertyValue(String propName, String propValue, boolean exactValueMatch, boolean caseInsensitive) {
        if (Strings.isNullOrEmpty(propValue)) {
            throw LogUtil.createUnexpected("propertyValue has not been passed as an argument");
        }

        String propertyNameColumn = (caseInsensitive ? "upper(" : "") + "p.propertyValue.property.name" + (caseInsensitive ? ") " : "");
        String propertyValueColumn = (caseInsensitive ? "upper(" : "") + "p.propertyValue.value" + (caseInsensitive ? ") " : "");
        String propertyValue = caseInsensitive ? propValue.toUpperCase() : propValue;

        if (!Strings.isNullOrEmpty(propName)) {
            String propertyName = caseInsensitive ? propName.toUpperCase() : propName;
            return template.find("select p from Assay t left join t.properties p " +
                    "where " + propertyNameColumn + " = '" + propertyName + "' " +
                    "and " + propertyValueColumn + (exactValueMatch ? " = '" + propertyValue + "' " : " like '%" + propertyValue + "%' "));
        }
        return template.find("select p from Assay t  left join t.properties p " +
                "where " + propertyValueColumn + (exactValueMatch ? " = '" + propertyValue + "' " : " like '%" + propertyValue + "%' "));
    }

    @Override
    public void save(Assay object) {
        super.save(object);
        template.flush();
    }

    /**
     * @return Name of the column for hibernate to match searched objects against - c.f. super.getByName()
     */
    @Override
    public String getNameColumn() {
        return NAME_COL;
    }
}
