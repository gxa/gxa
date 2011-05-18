package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.CurrentAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Organism;

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

    public AnnotationSource findOrCreate(String name, String version, String organism, Collection<BioEntityType> types) {
        return null;
    }

    public void saveCurrentAnnotationSource(CurrentAnnotationSource currentAnnotationSource) {

    }

    public Collection<CurrentAnnotationSource> getAllCurrentAnnotationSources() {
        return null;
    }

}
