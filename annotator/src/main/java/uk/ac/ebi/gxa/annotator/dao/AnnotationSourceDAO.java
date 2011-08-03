package uk.ac.ebi.gxa.annotator.dao;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AbstractDAO;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.gxa.dao.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.BioEntityTypeDAO;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO extends AbstractDAO<AnnotationSource> {

    private SessionFactory sessionFactory;
    private JdbcTemplate atlasJdbcTemplate;

    @Autowired
    private OrganismDAO organismDAO;
    @Autowired
    private SoftwareDAO softwareDAO;
    @Autowired
    private BioEntityTypeDAO typeDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private ArrayDesignDAO arrayDesignDAO;

    public AnnotationSourceDAO(SessionFactory sessionFactory, JdbcTemplate atlasJdbcTemplate) {
        super(sessionFactory, AnnotationSource.class);

        this.sessionFactory = sessionFactory;
        this.atlasJdbcTemplate = atlasJdbcTemplate;
    }

    @Override
    public void save(AnnotationSource object) {
        object.setLoadDate(new Date());
        template.save(object);
        template.flush();
    }

    public <T extends AnnotationSource> Collection<T> getAnnotationSourcesOfType(Class<T> type) {
        List<T> result = template.find("from " + type.getSimpleName());
//        List<T> result = template.find("from " + type.getSimpleName() + " where software.isActive = ?", true);
        return result;
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }

    public void remove(BioMartAnnotationSource annSrc) {
        template.delete(annSrc);
    }

    public boolean isAnnSrcApplied(AnnotationSource annSrc) {
        //ToDo: this is very slow query - try to find faster solution
        String query = "SELECT bepv.bioentityid\n" +
                "  FROM A2_BIOENTITYBEPV BEPV\n" +
                "  JOIN A2_BIOENTITY BE ON BE.BIOENTITYID = BEPV.BIOENTITYID\n" +
                "  where BEPV.SOFTWAREID=? and BE.ORGANISMID=? and rownum=1";
        List list = atlasJdbcTemplate.queryForList(query, annSrc.getSoftware().getSoftwareid(), annSrc.getOrganism().getId());

        return list.size() > 0;
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
        sessionFactory.getCurrentSession().setFlushMode(FlushMode.COMMIT);
    }

    public void finishSession() {
        SessionFactoryUtils.processDeferredClose(sessionFactory);
    }

}
