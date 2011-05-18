package uk.ac.ebi.microarray.atlas.model.bioentity;

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
}
