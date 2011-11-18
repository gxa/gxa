package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public List<Assay> getAssaysByOntologyTerm(String ontologyTerm) {
        return template.find("select a from Experiment e left join e.assays a left join a.properties p left join p.terms t where t.accession = ? ", ontologyTerm);
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
