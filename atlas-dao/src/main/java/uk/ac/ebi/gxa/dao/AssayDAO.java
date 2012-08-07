package uk.ac.ebi.gxa.dao;

import com.google.common.base.Strings;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

import javax.annotation.Nonnull;
import java.util.List;

public class AssayDAO extends AbstractDAO<Assay> {
    public static final String NAME_COL = "accession";

    private static String COMMON_HQL = "from Assay a left join a.properties p where p.propertyValue.property.name = ? ";

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
    public List<Assay> getAssaysByPropertyValue(String propertyName, String propertyValue) {
        return template.find("select a " + COMMON_HQL + " and p.propertyValue.value = ?", propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    public List<AssayProperty> getAssayPropertiesByProperty(@Nonnull String propertyName, boolean exactMatch, boolean caseInsensitive) {

        findPropertiesQueryBuilder.setPropertyEntityName("AssayProperty")
            .setCaseInsensitive(caseInsensitive)
            .setExactMatch(exactMatch);

        if (caseInsensitive) {
            propertyName = propertyName.toUpperCase();
        }

        if (!exactMatch) {
            propertyName = findPropertiesQueryBuilder.addHqlLikeSymbols(propertyName);
        }

        String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByName();

        return template.find(queryString, propertyName);

    }


    @SuppressWarnings("unchecked")
    public List<AssayProperty> getAssayPropertiesByPropertyValue(String propertyName, @Nonnull String propertyValue, boolean exactMatch, boolean caseInsensitive) {


        findPropertiesQueryBuilder.setPropertyEntityName("AssayProperty")
            .setCaseInsensitive(caseInsensitive)
            .setExactMatch(exactMatch);

        if (caseInsensitive) {
            propertyValue = propertyValue.toUpperCase();
        }

        if (!exactMatch) {
            propertyValue = findPropertiesQueryBuilder.addHqlLikeSymbols(propertyValue);
        }

        if (!Strings.isNullOrEmpty(propertyName)) {

            if (caseInsensitive) {
                propertyName = propertyName.toUpperCase();
            }

            String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByNameAndValue();

            return template.find(queryString, propertyName, propertyValue);

        }

        String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByValue();


        return template.find(queryString, propertyValue);

    }

    @SuppressWarnings("unchecked")
    public List<AssayProperty> getAssayPropertiesByOntologyTerm(@Nonnull String ontologyTerm) {
        if (Strings.isNullOrEmpty(ontologyTerm))
            throw LogUtil.createUnexpected("ontologyTerm has not been passed as an argument");

        return template.find("select p from Assay t left join t.properties p left join p.terms t where t.accession = ? ", ontologyTerm);
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
