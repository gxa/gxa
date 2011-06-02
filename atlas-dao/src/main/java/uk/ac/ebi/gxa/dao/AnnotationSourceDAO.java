package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.CurrentAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO extends AbstractDAO<AnnotationSource>{

    private SoftwareDAO softwareDAO;

    AnnotationSourceDAO(SessionFactory sessionFactory, SoftwareDAO softwareDAO) {
        super(sessionFactory, AnnotationSource.class);
        this.softwareDAO = softwareDAO;
    }

    @Override
    public void save(AnnotationSource object) {
        super.save(object);
        template.flush();
    }

    public void saveAsCurrentAnnotationSource(AnnotationSource currentAnnotationSource) {

    }

//    public Collection<CurrentAnnotationSource> getAllCurrentAnnotationSources() {
//        Collection<CurrentAnnotationSource> annotationSources = new ArrayList<CurrentAnnotationSource>();
//
//        return annotationSources;
//    }

    public Collection<AnnotationSource> getAllCurrentAnnotationSources() {
        Collection<AnnotationSource> annotationSources = new ArrayList<AnnotationSource>();

        return annotationSources;
    }

    public <T extends AnnotationSource> Collection<T> getCurrentAnnotationSourcesOfType(Class<T> type) {
        return null;
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type){
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }
}
