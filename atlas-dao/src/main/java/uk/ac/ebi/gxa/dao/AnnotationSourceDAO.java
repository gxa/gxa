package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;

import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO extends AbstractDAO<AnnotationSource> {

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
        Software software = currentAnnotationSource.getSoftware();
        software.setActive(true);
        softwareDAO.save(software);
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
}
