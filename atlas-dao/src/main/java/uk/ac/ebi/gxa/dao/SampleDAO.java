package uk.ac.ebi.gxa.dao;

import com.google.common.base.Strings;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import javax.annotation.Nonnull;
import java.util.List;

/**
 *
 * @author Robert Petryszak
 */
public class SampleDAO extends AbstractDAO<Sample> {

    public static final String NAME_COL = "accession";

    private static final String COMMON_HQL = "from Sample s left join s.properties p where p.propertyValue.property.name = ? ";

    public static final Logger log = LoggerFactory.getLogger(SampleDAO.class);

    public SampleDAO(SessionFactory sessionFactory) {
        super(sessionFactory, Sample.class);
    }

    long getTotalCount() {
        return (Long) template.find("select count(a) FROM Sample a").get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Sample> getSamplesByProperty(String propertyName) {
        return template.find("select s " + COMMON_HQL, propertyName);
    }

    @SuppressWarnings("unchecked")
    public List<Sample> getSamplesByPropertyValue(String propertyName, String propertyValue) {
        return template.find("select s " + COMMON_HQL + " and p.propertyValue.value = ?", propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    public List<SampleProperty> getSamplePropertiesByProperty(@Nonnull String propName, boolean exactValueMatch, boolean caseInsensitive) {
        if (Strings.isNullOrEmpty(propName))
            throw LogUtil.createUnexpected("PropertyName has not been passed as an argument");

        String propertyNameColumn = (caseInsensitive ? "upper(" : "") + "p.propertyValue.property.name" + (caseInsensitive ? ") " : "");
        String propertyName = caseInsensitive ? propName.toUpperCase() : propName;
        return template.find("select p from Sample t left join t.properties p " +
                "where " + propertyNameColumn + (exactValueMatch ? " = '" + propertyName + "' " : " like '%" + propertyName + "%' "));
    }


    @SuppressWarnings("unchecked")
    public List<SampleProperty> getSamplePropertiesByPropertyValue(String propertyName, @Nonnull String propertyValue, boolean exactMatch, boolean caseInsensitive) {

        findPropertiesQueryBuilder.setParentEntityName("Sample")
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

            return template.find(queryString, propertyValue, propertyName);

        }

        String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByValue();


        return template.find(queryString, propertyValue);

    }


    @SuppressWarnings("unchecked")
    public List<SampleProperty> getSamplePropertiesByOntologyTerm(@Nonnull String ontologyTerm) {
        if (Strings.isNullOrEmpty(ontologyTerm))
            throw LogUtil.createUnexpected("ontologyTerm has not been passed as an argument");

        return template.find("select p from Sample t left join t.properties p left join p.terms t where t.accession = ? ", ontologyTerm);
    }

    @Override
    public void save(Sample object) {
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
