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

    public <T extends AnnotationSource> Collection<T> getCurrentAnnotationSourcesOfType(Class<T> type) {
        List<T> result = template.find("from " + type.getSimpleName());
        return result;

//        //ToDo: this is just for test, write real method!
//        Software software = new Software(null, "plants", "8");
//        Organism organism = organismDAO.getByName("arabidopsis thaliana");
//
//        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
//        annotationSource.setDatabaseName("plant");
//        annotationSource.setDatasetName("athaliana_eg_gene");
//        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");
//
//        BioEntityProperty goterm = bioEntityPropertyDAO.getByName("goterm");
//        annotationSource.addBioMartProperty("name_1006", goterm);
//
//        BioEntityType transcType = (BioEntityType) getFirst(template.find("from BioEntityType where name = ?", BioEntityType.ENSTRANSCRIPT), null);
//        BioEntityType geneType = (BioEntityType) getFirst(template.find("from BioEntityType where name = ?", BioEntityType.ENSGENE), null);
//
//        annotationSource.addBioentityType(transcType);
//        annotationSource.addBioentityType(geneType);
//
//        return (Collection<T>) Arrays.asList(annotationSource);
    }

    public <T extends AnnotationSource> T findAnnotationSource(Software software, Organism organism, Class<T> type) {
        String queryString = "from " + type.getSimpleName() + " where software = ? and organism = ?";
        final List results = template.find(queryString, software, organism);
        return results.isEmpty() ? null : (T) results.get(0);
    }
}
