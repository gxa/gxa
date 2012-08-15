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
    public List<SampleProperty> getSamplePropertiesByProperty(@Nonnull String propertyName, boolean exactMatch) {

        findPropertiesQueryBuilder.setPropertyEntityName("SampleProperty")
                .setExactMatch(exactMatch);


        propertyName = propertyName.toUpperCase();


        if (!exactMatch) {
            propertyName = findPropertiesQueryBuilder.addHqlLikeSymbols(propertyName);
        }

        String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByName();

        return template.find(queryString, propertyName);

    }


    @SuppressWarnings("unchecked")
    public List<SampleProperty> getSamplePropertiesByPropertyValue(String propertyName, @Nonnull String propertyValue, boolean exactMatch) {

        findPropertiesQueryBuilder.setPropertyEntityName("SampleProperty")
                .setExactMatch(exactMatch);


        propertyValue = propertyValue.toUpperCase();


        if (!exactMatch) {
            propertyValue = findPropertiesQueryBuilder.addHqlLikeSymbols(propertyValue);
        }

        if (!Strings.isNullOrEmpty(propertyName)) {


            propertyName = propertyName.toUpperCase();


            String queryString = findPropertiesQueryBuilder.getQueryThatSelectsPropertiesByNameAndValue();

            return template.find(queryString, propertyName, propertyValue);

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

    public void saveSampleProperty(SampleProperty sampleProperty) {
        template.saveOrUpdate(sampleProperty);
        template.flush();

    }
}
