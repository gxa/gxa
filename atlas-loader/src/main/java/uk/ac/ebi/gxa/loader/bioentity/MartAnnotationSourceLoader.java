package uk.ac.ebi.gxa.loader.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * User: nsklyar
 * Date: 20/06/2011
 */
public class MartAnnotationSourceLoader {

    public BioMartAnnotationSource parseAnnotationSource(String annSrcString) {
        BioMartAnnotationSource annSrc = new BioMartAnnotationSource(null, null);

        return annSrc;
    }

    public String annSrcToString(BioMartAnnotationSource annSrc) {
        OutputStream os = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(os);
        encoder.writeObject(annSrc);
        encoder.close();

        return os.toString();
    }

    public BioMartAnnotationSource copyAnnotationSource(BioMartAnnotationSource annSrc, Software software) {
        BioMartAnnotationSource newAnnSrc = new BioMartAnnotationSource(software, annSrc.getOrganism());

        return newAnnSrc;
    }

//    public static void main(String[] args) {
//        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(new Software("Ensembl", "61"), new Organism(null, "mus musculus"));
//        annotationSource.setDatasetName("mmusculus_gene_ensembl");
//        annotationSource.setUrl("http://www.ensembl.org/biomart/martservice?");
//        annotationSource.addBioMartProperty("external_gene_id", new BioEntityProperty(null, "symbol"));
//        annotationSource.addBioMartProperty("interpro", new BioEntityProperty(null, "interpro"));
//        annotationSource.addBioMartProperty("interpro_short_description", new BioEntityProperty(null, "interproterm"));
//
//        Person person = new Person("www", "bla bla");
//        OutputStream os = new ByteArrayOutputStream();
//        XMLEncoder encoder = new XMLEncoder(os);
//        encoder.writeObject(person);
//        encoder.close();
//
//        System.out.println("os.toString() = " + os.toString());
//    }

//    public static class Person implements Serializable{
//        private String name;
//        private String address;
////        private Set properties = new HashSet();
//
//
//        public Person() {
//        }
//
//        private Person(String name, String address) {
//            this.name = name;
//            this.address = address;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public void setName(String name) {
//            this.name = name;
//        }
//
//        public String getAddress() {
//            return address;
//        }
//
//        public void setAddress(String address) {
//            this.address = address;
//        }
//
//    }
}
