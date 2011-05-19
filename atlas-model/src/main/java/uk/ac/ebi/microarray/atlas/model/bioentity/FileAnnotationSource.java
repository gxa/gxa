package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class FileAnnotationSource extends AnnotationSource{

    private String fileName;

    public FileAnnotationSource(String name, String version, Organism organism, String fileName) {
        super(name, version, organism);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean isUpdatable() {
        return false;  
    }

    @Override
    protected CurrentAnnotationSource<? extends AnnotationSource> createCurrAnnSrc(BioEntityType bioEntityType) {
        return new CurrentAnnotationSource<FileAnnotationSource>(this, bioEntityType);
    }
}
