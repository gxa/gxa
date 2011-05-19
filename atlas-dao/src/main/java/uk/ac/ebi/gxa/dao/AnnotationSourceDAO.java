package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.CurrentAnnotationSource;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO {
    private JdbcTemplate template;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public AnnotationSource getById(long id) {
        return null;
    }

    public <T extends AnnotationSource> T save(T annotationSource) {
        return null;
    }

    public void saveCurrentAnnotationSource(CurrentAnnotationSource currentAnnotationSource) {

    }

    public Collection<CurrentAnnotationSource> getAllCurrentAnnotationSources() {
        return null;
    }

    public <T extends AnnotationSource> Collection<CurrentAnnotationSource<T>> getCurrentAnnotationSourcesOfType(Class<T> type) {
        Collection<CurrentAnnotationSource<T>> result = new ArrayList<CurrentAnnotationSource<T>>();

        return result;
    }

    public <T extends AnnotationSource> T findAnnotationSource(String name, String version, Organism organism, Class<T> type){
        return null;
    }
}
