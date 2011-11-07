package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;

/**
 * User: nsklyar
 * Date: 07/11/2011
 */
public enum AnnotationSourceClass {
    BIOMART(BioMartAnnotationSource.class, "BioMart"),
    FILE(GeneSigAnnotationSource.class, "GeneSigDB");

    private final Class<? extends AnnotationSource> clazz;
    private final String name;

    <T extends AnnotationSource> AnnotationSourceClass(Class<T> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Class<? extends AnnotationSource> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public static AnnotationSourceClass getByName(String name) {
        for (AnnotationSourceClass annotationSourceClass : values()) {
            if (annotationSourceClass.getName().equalsIgnoreCase(name)) {
                return annotationSourceClass;
            }
        }
        throw new IllegalArgumentException("There is no AnnotationSourceClass with a name " + name);
    }

    public static <T extends AnnotationSource> AnnotationSourceClass getByClass(Class<T> clazz) {
        for (AnnotationSourceClass annotationSourceClass : values()) {
            if (annotationSourceClass.getClazz().equals(clazz)) {
                return annotationSourceClass;
            }
        }
        throw new IllegalArgumentException("There is no AnnotationSourceClass for class " + clazz);
    }
}
