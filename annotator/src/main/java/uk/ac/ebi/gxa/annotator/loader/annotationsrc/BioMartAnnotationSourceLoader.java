package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnectionFactory;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartArrayDesign;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import static com.google.common.collect.Sets.difference;
import static com.google.common.io.Closeables.closeQuietly;

public class BioMartAnnotationSourceLoader {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String organism_propName = "organism";
    private static final String software_name_propName = "software.name";
    private static final String software_version_propName = "software.version";
    private static final String types_propName = "types";
    private static final String url_propName = "url";
    private static final String mySqlDbName_propName = "mySqlDbName";
    private static final String mySqlDbUrl_propName = "mySqlDbUrl";
    private static final String datasetName_propName = "datasetName";
    private static final String databaseName_propName = "databaseName";
    private static final String biomartProperty_propName = "biomartProperty";
    private static final String arrayDesign_propName = "arrayDesign";

    private AnnotationSourceDAO annSrcDAO;

    public BioMartAnnotationSourceLoader(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

     public String getAnnSrcAsStringById(String id) {
        Long aLong = Long.parseLong(id);
        BioMartAnnotationSource annotationSource = (BioMartAnnotationSource) annSrcDAO.getById(aLong);
        Writer writer = new CharArrayWriter();
        writeSource(annotationSource, writer);
        return writer.toString();
    }

//    @Transactional
    public void saveAnnSrc(String text) throws AnnotationLoaderException {
        Reader reader = new StringReader(text);

            annSrcDAO.startSession();
            BioMartAnnotationSource annotationSource = readSource(reader);

            annSrcDAO.save(annotationSource);
            annSrcDAO.finishSession();

    }
    public BioMartAnnotationSource readSource(Reader input) throws AnnotationLoaderException {
        Properties properties = new Properties();
        BioMartAnnotationSource annotationSource = null;
        try {
            properties.load(input);
            Organism organism = annSrcDAO.findOrCreateOrganism(getProperty(organism_propName, properties));
            Software software = annSrcDAO.findOrCreateSoftware(getProperty(software_name_propName, properties), getProperty(software_version_propName, properties));

            annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
            if (annotationSource == null) {
                annotationSource = new BioMartAnnotationSource(null, software, organism);
            }

            updateTypes(properties, annotationSource);

            annotationSource.setUrl(getProperty(url_propName, properties));
            annotationSource.setDatabaseName(getProperty(databaseName_propName, properties));
            annotationSource.setDatasetName(getProperty(datasetName_propName, properties));
            annotationSource.setMySqlDbName(getProperty(mySqlDbName_propName, properties));
            annotationSource.setMySqlDbUrl(getProperty(mySqlDbUrl_propName, properties));

            updateBioMartProperties(properties, annotationSource);

            updateBioMartArrayDesigns(properties, annotationSource);

        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        }

        return annotationSource;
    }

    private void writeSource(BioMartAnnotationSource annSrc, Writer out) {
        PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.addProperty(organism_propName, annSrc.getOrganism().getName());
        properties.addProperty(software_name_propName, annSrc.getSoftware().getName());
        properties.addProperty(software_version_propName, annSrc.getSoftware().getVersion());
        properties.addProperty(url_propName, annSrc.getUrl());
        properties.addProperty(databaseName_propName, annSrc.getDatabaseName());
        properties.addProperty(datasetName_propName, annSrc.getDatasetName());
        properties.addProperty(mySqlDbName_propName, annSrc.getMySqlDbName());
        properties.addProperty(mySqlDbUrl_propName, annSrc.getMySqlDbUrl());

        //Write beioentity types
        StringBuffer types = new StringBuffer();
        int count = 1;
        for (BioEntityType type : annSrc.getTypes()) {
            types.append(type.getName());
            if (count++ < annSrc.getTypes().size()) {
                types.append(",");
            }
        }
        properties.addProperty(types_propName, types.toString());

        writeBioMartProperties(annSrc, properties);

        writeBioMartArrayDesign(annSrc, properties);

        try {
            properties.save(out);

        } catch (ConfigurationException e) {
            log.error("Cannot write Annotation Source " + annSrc.getAnnotationSrcId(), e);
        } finally {
            closeQuietly(out);
        }
    }

    public Collection<BioMartAnnotationSource> getCurrentAnnotationSources() {
        Collection<BioMartAnnotationSource> result = new HashSet<BioMartAnnotationSource>();
        annSrcDAO.startSession();
        Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            try {
                BioMartConnection connection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);
                String newVersion = connection.getOnlineMartVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    //ToDo: Too slow - find faster solution
//                    annSrc.setApplied(annSrcDAO.isAnnSrcApplied(annSrc));
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = annSrcDAO.findOrCreateSoftware(annSrc.getSoftware().getName(), newVersion);
                    BioMartAnnotationSource newAnnSrc = annSrcDAO.findAnnotationSource(newSoftware, annSrc.getOrganism(), BioMartAnnotationSource.class);
                    //create and Save new AnnotationSource
                    if (newAnnSrc == null) {
                        newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                        annSrcDAO.save(newAnnSrc);
                        annSrcDAO.remove(annSrc);
                    }
                    result.add(newAnnSrc);
                }

            } catch (BioMartAccessException e) {
                log.error("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        annSrcDAO.finishSession();
        return result;
    }

    private void updateBioMartProperties(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<BioMartProperty> bioMartProperties = new HashSet<BioMartProperty>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(biomartProperty_propName)) {
                BioEntityProperty beProperty = annSrcDAO.findOrCreateBEProperty(propName.substring(biomartProperty_propName.length() + 1));
                StringTokenizer tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                while (tokenizer.hasMoreElements()) {
                    bioMartProperties.add(new BioMartProperty(tokenizer.nextToken().trim(), beProperty, annotationSource));
                }
            }
        }

        Set<BioMartProperty> removedProperties = new HashSet<BioMartProperty>(difference(annotationSource.getBioMartProperties(), bioMartProperties));
        Set<BioMartProperty> addedProperties = new HashSet<BioMartProperty>(difference(bioMartProperties, annotationSource.getBioMartProperties()));

        for (BioMartProperty removedProperty : removedProperties) {
            annotationSource.removeBioMartProperty(removedProperty);
        }

        for (BioMartProperty addedProperty : addedProperties) {
            annotationSource.addBioMartProperty(addedProperty);
        }
    }

    private void updateBioMartArrayDesigns(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<BioMartArrayDesign> bioMartArrayDesigns = new HashSet<BioMartArrayDesign>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(arrayDesign_propName)) {
                ArrayDesign arrayDesign = annSrcDAO.getArrayDesignShallowByAccession(propName.substring(arrayDesign_propName.length() + 1));
                StringTokenizer tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                while (tokenizer.hasMoreElements()) {
                    bioMartArrayDesigns.add(new BioMartArrayDesign(null, tokenizer.nextToken().trim(), arrayDesign, annotationSource));
                }
            }
        }

        Set<BioMartArrayDesign> removedProperties = new HashSet<BioMartArrayDesign>(difference(annotationSource.getBioMartArrayDesigns(), bioMartArrayDesigns));
        Set<BioMartArrayDesign> addedProperties = new HashSet<BioMartArrayDesign>(difference(bioMartArrayDesigns, annotationSource.getBioMartArrayDesigns()));

        for (BioMartArrayDesign removedProperty : removedProperties) {
            annotationSource.removeBioMartArrayDesign(removedProperty);
        }

        for (BioMartArrayDesign addedProperty : addedProperties) {
            annotationSource.addBioMartArrayDesign(addedProperty);
        }
    }

    private void updateTypes(Properties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
        String typesString = getProperty(types_propName, properties);
        Set<BioEntityType> newTypes = new HashSet<BioEntityType>();

        StringTokenizer tokenizer = new StringTokenizer(typesString, ",");
        while (tokenizer.hasMoreElements()) {
            newTypes.add(annSrcDAO.findOrCreateBioEntityType(tokenizer.nextToken().trim()));
        }

        Set<BioEntityType> removedTypes = new HashSet<BioEntityType>(difference(annotationSource.getTypes(), newTypes));
        for (BioEntityType removedType : removedTypes) {
            annotationSource.removeBioentityType(removedType);
        }

        Set<BioEntityType> addedTypes = new HashSet<BioEntityType>(difference(newTypes, annotationSource.getTypes()));
        for (BioEntityType addedType : addedTypes) {
            annotationSource.addBioentityType(addedType);
        }
    }

    private void writeBioMartProperties(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();
        for (BioMartProperty bioMartProperty : annSrc.getBioMartProperties()) {
            bePropToBmProp.put(bioMartProperty.getBioEntityProperty().getName(), bioMartProperty.getName());
        }

        addCommaSeparatedProperties(biomartProperty_propName, properties, bePropToBmProp);

    }

    private void writeBioMartArrayDesign(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();

        for (BioMartArrayDesign bioMartArrayDesign : annSrc.getBioMartArrayDesigns()) {
            bePropToBmProp.put(bioMartArrayDesign.getArrayDesign().getAccession(), bioMartArrayDesign.getName());
        }

        addCommaSeparatedProperties(arrayDesign_propName, properties, bePropToBmProp);
    }

    private void addCommaSeparatedProperties(String propNamePrefix, PropertiesConfiguration properties, Multimap<String, String> bePropToBmProp) {
        int count;
        for (String beProp : bePropToBmProp.keySet()) {
            count = 1;
            StringBuffer bmProperties = new StringBuffer();
            Collection<String> bmPropCollection = bePropToBmProp.get(beProp);
            for (String bmProp : bmPropCollection) {
                bmProperties.append(bmProp);
                if (count++ < bmPropCollection.size()) {
                    bmProperties.append(",");
                }
            }
            properties.addProperty(propNamePrefix + "." + beProp, bmProperties.toString());
        }
    }


    private String getProperty(String name, Properties properties) throws AnnotationLoaderException {
        String property = properties.getProperty(name);
        if (property == null) {
            throw new AnnotationLoaderException("Required property " + name + " is missing");
        }
        return property;
    }


    private static final String input = "organism=gallus gallus\n" +
            "software.name=Ensembl\n" +
            "software.version=63\n" +
            "types=ensgene,enstranscript\n" +
            "url=http://www.ensembl.org/biomart/martservice?\n" +
            "datasetName=ggallus_gene_ensembl\n" +
            "databaseName=ensembl\n" +
            "biomartProperty.ensgene=ensembl_gene_id\n" +
            "biomartProperty.enstranscript=ensembl_transcript_id\n" +
            "biomartProperty.ensprotein=ensembl_peptide_id\n" +
            "biomartProperty.description=description\n" +
            "biomartProperty.symbol=external_gene_id\n" +
            "biomartProperty.goterm=name_1006\n" +
            "biomartProperty.go=go_id\n" +
            "biomartProperty.interpro=interpro\n" +
            "biomartProperty.interproterm=interpro_short_description\n" +
            "biomartProperty.hgnc_symbol=hgnc_symbol\n" +
            "biomartProperty.uniprot=uniprot_sptrembl, uniprot_swissprot_accession\n" +
            "biomartProperty.unigene=unigene\n" +
            "biomartProperty.refseq=refseq_dna, refseq_peptide\n" +
            "biomartProperty.embl=embl\n" +
            "biomartProperty.ensfamily_description=family_description\n" +
            "biomartProperty.ortholog=cow_ensembl_gene, ciona_intestinalis_ensembl_gene, zebrafish_ensembl_gene, drosophila_ensembl_gene, human_ensembl_gene, mouse_ensembl_gene, rat_ensembl_gene, yeast_ensembl_gene, xenopus_ensembl_gene\n" +
            "biomartProperty.entrezgene=entrezgene\n" +
            "biomartProperty.ensfamily=family\n" +
            "biomartProperty.mirbase_id=mirbase_id\n" +
            "biomartProperty.mirbase_accession=mirbase_accession";
}

