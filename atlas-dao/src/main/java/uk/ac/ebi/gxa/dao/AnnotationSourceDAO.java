package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class AnnotationSourceDAO extends AbstractDAO<AnnotationSource> {

    private SoftwareDAO softwareDAO;
    private OrganismDAO organismDAO;
    private BioEntityPropertyDAO bioEntityPropertyDAO;

    AnnotationSourceDAO(SessionFactory sessionFactory, SoftwareDAO softwareDAO) {
        super(sessionFactory, AnnotationSource.class);
        this.softwareDAO = softwareDAO;
    }

    public AnnotationSourceDAO(SessionFactory sessionFactory,  SoftwareDAO softwareDAO, OrganismDAO organismDAO, BioEntityPropertyDAO bioEntityPropertyDAO) {
        super(sessionFactory, AnnotationSource.class);
        this.softwareDAO = softwareDAO;
        this.organismDAO = organismDAO;
        this.bioEntityPropertyDAO = bioEntityPropertyDAO;
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

        //ToDo: this is just for test, write real method!
        Software software = new Software(null, "plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityProperty goterm = bioEntityPropertyDAO.getByName("goterm");
        annotationSource.addBioMartProperty("name_1006", goterm);

        return (Collection<T>) Arrays.asList(annotationSource);
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }
}
