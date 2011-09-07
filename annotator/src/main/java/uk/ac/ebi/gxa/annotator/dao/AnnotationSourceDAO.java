package uk.ac.ebi.gxa.annotator.dao;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
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
public class AnnotationSourceDAO {

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

    private final HibernateTemplate template;

    public AnnotationSourceDAO(SessionFactory sessionFactory, JdbcTemplate atlasJdbcTemplate) {
        this.template = new HibernateTemplate(sessionFactory);

        this.atlasJdbcTemplate = atlasJdbcTemplate;
    }

    public AnnotationSource getById(long id) {
         return template.get(AnnotationSource.class, id);
     }

    public void save(AnnotationSource object) {
        object.setLoadDate(new Date());
        template.save(object);
        template.flush();
    }

    public <T extends AnnotationSource> Collection<T> getAnnotationSourcesOfType(Class<T> type) {
        List<T> result = template.find("from " + type.getSimpleName());
        return result;
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }

    public void remove(BioMartAnnotationSource annSrc) {
       
        template.delete(annSrc);
        template.flush();
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
        return organismDAO.getOrCreateOrganism(organismName);
    }

    public Software findOrCreateSoftware(String swName, String swVersion) {
        return softwareDAO.findOrCreate(swName, swVersion);
    }

    public BioEntityType findOrCreateBioEntityType(String typeName) {
       return typeDAO.findOrCreate(typeName);
    }

    public BioEntityProperty findOrCreateBEProperty(String propertyName) {
        return propertyDAO.findOrCreate(propertyName);
    }

    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }
}
