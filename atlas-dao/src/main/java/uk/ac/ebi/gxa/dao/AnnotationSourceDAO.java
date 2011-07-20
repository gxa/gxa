package uk.ac.ebi.gxa.dao;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;

import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO extends AbstractDAO<AnnotationSource> {

    private SessionFactory sessionFactory;

    private OrganismDAO organismDAO;
    private SoftwareDAO softwareDAO;
    private BioEntityTypeDAO typeDAO;
    private BioEntityPropertyDAO propertyDAO;
    private ArrayDesignDAO arrayDesignDAO;

    public AnnotationSourceDAO(SessionFactory sessionFactory) {
        super(sessionFactory, AnnotationSource.class);

        this.sessionFactory = sessionFactory;

        organismDAO = new OrganismDAO(sessionFactory);
        softwareDAO = new SoftwareDAO(sessionFactory);
        typeDAO = new BioEntityTypeDAO(sessionFactory);
        propertyDAO = new BioEntityPropertyDAO(sessionFactory);
        arrayDesignDAO = new ArrayDesignDAO(null, sessionFactory);
    }

    @Override
    public void save(AnnotationSource object) {
        object.setLoadDate(new Date());
//        template.merge(object);
//        template.save(object);
//        template.flush();
        sessionFactory.getCurrentSession().merge(object);
        sessionFactory.getCurrentSession().flush();
    }

    public <T extends AnnotationSource> Collection<T> getCurrentAnnotationSourcesOfType(Class<T> type) {
        List<T> result = template.find("from " + type.getSimpleName());
        return result;
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }

    public Organism findOrCreateOrganism(String organismName) {
        Organism organism = organismDAO.getByName(organismName);
        if (organism == null) {
            organism = new Organism(null, organismName);
            organismDAO.save(organism);
        }
        return organism;
    }

    public Software findOrCreateSoftware(String swName, String swVersion) {
        return softwareDAO.findOrCreate(swName, swVersion);
    }

    public BioEntityType findOrCreateBioEntityType(String typeName) {
        BioEntityType type = typeDAO.find(typeName);
        if (type == null) {
            BioEntityProperty beProperty = findOrCreateBEProperty(typeName);
            type = new BioEntityType(null, typeName, 0, beProperty, beProperty);
        }

        return type;
    }

    public BioEntityProperty findBEProperty(String propertyName) {
        return propertyDAO.getByName(propertyName);
    }

    public BioEntityProperty findOrCreateBEProperty(String propertyName) {
        BioEntityProperty property = propertyDAO.getByName(propertyName);
        if (property == null) {
            property = new BioEntityProperty(null, propertyName);
            propertyDAO.save(property);
        }
        return property;
    }

    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }

    public void startSession() {
        SessionFactoryUtils.initDeferredClose(sessionFactory);
        FlushMode flushMode = sessionFactory.getCurrentSession().getFlushMode();
        System.out.println("flushMode = " + flushMode);
        sessionFactory.getCurrentSession().setFlushMode(FlushMode.COMMIT);
    }

    public void finishSession() {
        SessionFactoryUtils.processDeferredClose(sessionFactory);
    }
}
