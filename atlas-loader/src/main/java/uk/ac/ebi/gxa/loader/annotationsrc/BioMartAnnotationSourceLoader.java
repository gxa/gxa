package uk.ac.ebi.gxa.loader.annotationsrc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cache.HashtableCacheProvider;
import org.hibernate.dialect.Oracle10gDialect;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.gxa.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.BioEntityTypeDAO;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.hibernate.AtlasNamingStrategy;
import uk.ac.ebi.gxa.dao.hibernate.SchemaValidatingAnnotationSessionFactoryBean;
import uk.ac.ebi.gxa.loader.bioentity.BioMartAccessException;
import uk.ac.ebi.gxa.loader.bioentity.BioMartConnection;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Ontology;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.annotation.FileAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import static com.google.common.io.Closeables.closeQuietly;

public class BioMartAnnotationSourceLoader {

    private static final String organism_propName = "organism";
    private static final String software_name_propName = "software.name";
    private static final String software_version_propName = "software.version";
    private static final String types_propName = "types";
    private static final String url_propName = "url";
    private static final String datasetName_propName = "datasetName";
    private static final String databaseName_propName = "databaseName";
    private static final String biomartProperty_propName = "biomartProperty";

    private AnnotationSourceDAO annSrcDAO;

    public BioMartAnnotationSourceLoader(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public BioMartAnnotationSource readSource(Reader input) throws AnnotationLoaderException {
        Properties properties = new Properties();
        BioMartAnnotationSource annotationSource = null;
        try {
            properties.load(input);
            Organism organism = annSrcDAO.findOrCreateOrganism(getProperty(organism_propName, properties));
            Software software = annSrcDAO.findOrCreateSoftware(getProperty(software_name_propName, properties), getProperty(software_version_propName, properties));

            annotationSource = new BioMartAnnotationSource(null, software, organism);

            String types = getProperty(types_propName, properties);
            StringTokenizer tokenizer = new StringTokenizer(types, ",");
            while (tokenizer.hasMoreElements()) {
                annotationSource.addBioentityType(annSrcDAO.findOrCreateBioEntityType(tokenizer.nextToken().trim()));
            }

            annotationSource.setUrl(getProperty(url_propName, properties));
            annotationSource.setDatabaseName(getProperty(databaseName_propName, properties));
            annotationSource.setDatasetName(getProperty(datasetName_propName, properties));

            for (String propName : properties.stringPropertyNames()) {
                if (propName.startsWith(biomartProperty_propName)) {
                    BioEntityProperty beProperty = annSrcDAO.findBEProperty(propName.substring(biomartProperty_propName.length() + 1));
                    tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                    while (tokenizer.hasMoreElements()) {
                        annotationSource.addBioMartProperty(tokenizer.nextToken().trim(), beProperty);
                    }

                }
            }

        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        }


        System.out.println(properties.getProperty("databaseName"));
        return annotationSource;
    }

    public void saveAnnotationSource(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        BioMartConnection connection = new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
        boolean validDataSetName = connection.isValidDataSetName();
        System.out.println("connection.isValidDataSetName() = " + validDataSetName);
        Collection<String> invalidAttributes = connection.validateAttributeNames(annSrc.getBioMartPropertyNames());
        System.out.println("strings = " + invalidAttributes);
        if (validDataSetName && invalidAttributes.isEmpty()) {
            //ToDo: check if one already exists, and update in this case
            annSrcDAO.save(annSrc);
        } else {
            throw new BioMartAccessException("Annotation source is not valid: " + invalidAttributes);
        }
    }

    public void writeSource(BioMartAnnotationSource annSrc, OutputStream out) {
        Properties properties = new Properties();
        properties.put(organism_propName, annSrc.getOrganism().getName());
        properties.put(software_name_propName, annSrc.getSoftware().getName());
        properties.put(software_version_propName, annSrc.getSoftware().getVersion());
        properties.put(url_propName, annSrc.getUrl());
        properties.put(databaseName_propName, annSrc.getDatabaseName());
        properties.put(datasetName_propName, annSrc.getDatasetName());
        StringBuffer types = new StringBuffer();
        int count = 1;
        for (BioEntityType type : annSrc.getTypes()) {
            types.append(type.getName());
            if (count++ < annSrc.getTypes().size()) {
                types.append(",");
            }
        }
        properties.put(types_propName, types.toString());

        Multimap<String, String> bePropToBmProp = HashMultimap.create();
        for (BioMartProperty bioMartProperty : annSrc.getBioMartProperties()) {
            bePropToBmProp.put(bioMartProperty.getBioEntityProperty().getName(), bioMartProperty.getName());
        }

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
            properties.put(biomartProperty_propName + "." + beProp, bmProperties.toString());
        }

        try {
            properties.store(out, "AnnSrcID:" + annSrc.getAnnotationSrcId());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            closeQuietly(out);
        }
    }

    private String getProperty(String name, Properties properties) throws AnnotationLoaderException {
        String property = properties.getProperty(name);
        if (property == null) {
            throw new AnnotationLoaderException("Required property " + name + " is missing");
        }
        return property;
    }

    public static void main(String[] args) throws Exception {

        SessionFactory sessionFactory = createSessionFactory();

        AnnotationSourceDAO annSrcDAO = new AnnotationSourceDAO(sessionFactory);

        BioMartAnnotationSourceLoader loader = new BioMartAnnotationSourceLoader(annSrcDAO);
        Reader reader = new StringReader(input);

//        softwareDAO.startSession();
        org.hibernate.Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        Transaction transaction = session.beginTransaction();
        BioMartAnnotationSource annotationSource = loader.readSource(reader);

        loader.saveAnnotationSource(annotationSource);

        transaction.commit();

//        softwareDAO.finishSession();

    }

    private static SessionFactory createSessionFactory() throws Exception {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:oracle:thin:@barney.ebi.ac.uk:1521:ATLASDEV", "nsklyar", "nsklyar", true);

        SchemaValidatingAnnotationSessionFactoryBean factory = new SchemaValidatingAnnotationSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setAnnotatedClasses(new Class[]{
                Experiment.class,
                Assay.class,
                Sample.class,
                AssayProperty.class,
                SampleProperty.class,
                ArrayDesign.class,
                Organism.class,
                Property.class,
                PropertyValue.class,
                Ontology.class,
                OntologyTerm.class,
                Asset.class,
                BioEntityProperty.class,
                BioMartAnnotationSource.class,
                BioMartProperty.class,
                BioEntityType.class,
                FileAnnotationSource.class,
                AnnotationSource.class,
                Software.class
        });
        factory.setHibernateProperties(new Properties() {
            {
                put("hibernate.show_sql", Boolean.TRUE);
                put("hibernate.dialect", Oracle10gDialect.class.getName());
            }
        });
        factory.setNamingStrategy(new AtlasNamingStrategy());
        factory.setCacheProvider(new HashtableCacheProvider());
        factory.afterPropertiesSet();
//        factory.validateDatabaseSchema();

        return factory.getObject();
    }

//    private static final String input = "organism=Homo Sapiens\n" +
//            "software.name=Ensembl\n" +
//            "software.version=63\n" +
//            "types=ensgene,enstranscript\n" +
//            "url=http://www.ensembl.org/biomart/martservice?\n" +
//            "datasetName=hsapiens_gene_ensembl\n" +
//            "databaseName=ensembl\n" +
//            "biomartProperty.ensgene=ensembl_gene_id\n" +
//            "biomartProperty.enstranscript=ensembl_transcript_id\n" +
//            "biomartProperty.ensprotein=ensembl_peptide_id\n" +
//            "biomartProperty.description=description\n" +
//            "biomartProperty.symbol=external_gene_id\n" +
//            "biomartProperty.goterm=name_1006\n" +
//            "biomartProperty.go=go_id\n" +
//            "biomartProperty.interpro=interpro\n" +
//            "biomartProperty.interproterm=interpro_short_description\n" +
//            "biomartProperty.hgnc_symbol=hgnc_symbol\n" +
//            "biomartProperty.uniprot=uniprot_sptrembl, uniprot_swissprot_accession\n" +
//            "biomartProperty.unigene=unigene\n" +
//            "biomartProperty.refseq=refseq_dna, refseq_peptide\n" +
//            "biomartProperty.embl=embl\n" +
//            "biomartProperty.disease=mim_morbid_description\n" +
//            "biomartProperty.ensfamily_description=family_description\n" +
//            "biomartProperty.ortholog=cow_ensembl_gene, ciona_intestinalis_ensembl_gene, zebrafish_ensembl_gene, drosophila_ensembl_gene, chicken_ensembl_gene, mouse_ensembl_gene, rat_ensembl_gene, yeast_ensembl_gene, xenopus_ensembl_gene\n" +
//            "biomartProperty.entrezgene=entrezgene\n" +
//            "biomartProperty.ensfamily=family\n" +
//            "biomartProperty.mirbase_id=mirbase_id\n" +
//            "biomartProperty.mirbase_accession=mirbase_accession";

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

