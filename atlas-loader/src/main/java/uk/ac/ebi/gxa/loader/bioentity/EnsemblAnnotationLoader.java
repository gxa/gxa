package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.EnsemblAnnotationDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class EnsemblAnnotationLoader extends AtlasBioentityAnnotationLoader {

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    static final String[] properties = {"human_ensembl_gene", "ensembl_peptide_id", "description",
            "external_gene_id", "name_1006", "go_biological_process_id",
            "go_cellular_component__dm_name_1006", "go_cellular_component_id",
            "go_molecular_function__dm_name_1006", "go_molecular_function_id",
            "interpro", "interpro_short_description",
            "uniprot_sptrembl", "uniprot_swissprot_accession",
            "refseq_peptide",
            "embl", "entrezgene",
            "unigene", "refseq_dna", "mirbase_accession", "mirbase_id", "hgnc_symbol", "mim_morbid_description"};
    //            "unigene", "refseq_dna","mirbase_accession", "mirbase_id", "mgi_id","mgi_symbol", "mgi_description"};
//            "family", "family_description", "unigene", "mirbase_accession", "mirbase_id"};


    private BioMartDAO bioMartDAO = new BioMartDAO();
    private SoftwareDAO swDao;


    public void updateAnnotaions(List<String> organisms) throws AtlasLoaderException {
        for (String organism : organisms) {
            updateAnnotationsForOrganism(organism);
        }
    }

    private void updateAnnotationsForOrganism(String organism) throws AtlasLoaderException {
        //check latest Ens version from registry
        String ensemblVersion = bioMartDAO.getEnsemblVersion();

        //ToDo: check version for a particular organism
        String dbVersion = swDao.getLatestVersionOfSoftware(SoftwareDAO.ENSEMBL);

        if (StringUtils.isNotEmpty(ensemblVersion) && StringUtils.isNotEmpty(dbVersion) && ensemblVersion.equals(dbVersion)) {
            log.info("Ensembl versions in BioMart and in Atlas are the same!");

        }

        Map<String, String> organismToEnsOrgName = bioEntityDAO.getOrganismToEnsOrgName();
        Multimap<String, String> propertyToEnsPropNames = bioEntityDAO.getPropertyToEnsPropNames();
        CSVReader csvReader = null;
        boolean beExist = false;
        try {
            if (organismToEnsOrgName.containsKey(organism)) {
                for (String atlasProperty : propertyToEnsPropNames.keySet()) {
                    for (String ensProperty : propertyToEnsPropNames.get(atlasProperty)) {
                        URL url = bioMartDAO.getPropertyForOrganismURL(organismToEnsOrgName.get(organism), ensProperty);
                        if (url != null) {
                            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                            readProperty(csvReader, organism, atlasProperty, beExist);
                            csvReader.close();
                            beExist = true;
                        }
                    }
                }
            } else {
                //ToDo: maybe throw an exception
                log.info("Organism " + organism + " is not mapped to Ensembl data set.");
            }

            writeBioentitiesAndAnnotations(true);

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for organism " + organism, e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    private void readProperty(CSVReader csvReader, String organism, String propertyName, boolean beExist) throws IOException {

        String[] line;

        while ((line = csvReader.readNext()) != null) {
            if (line.length < 1 || line[0].contains("Exception")) {
                log.info("Cannot get property " + propertyName + " for organism " + organism);
                break;
            }
            System.out.println("line = " + line);

            String beIdentifier = line[1];
            String geneName = line[0];
            String value = line[2];


            addPropertyValue(beIdentifier, geneName, propertyName, value);

            if (!beExist) {
                addTranscriptGeneMapping(beIdentifier, geneName);
                addGene(organism, getGeneField(), geneName);
                addTransctipt(organism, getTranscriptField(), beIdentifier);
            }


        }
    }

    private void readAnnotations(String organism) {
        for (int i = 0; i < properties.length; i++) {
            String property = properties[i];
            CSVReader csvReader = null;
            URL url = null;
            boolean beExist = false;
            try {
                url = bioMartDAO.getPropertyForOrganismURL(organism, property);

                InputStream inputStream = url.openStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String s = bufferedReader.readLine();
                System.out.println("s = " + s);
                bufferedReader.close();

                csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                readProperty(csvReader, organism, property, beExist);

            } catch (MalformedURLException e) {
                log.error("Problem when reading bioentity annotations file " + url, e);
            } catch (IOException e) {
                log.error("Problem when reading bioentity annotations file " + url, e);
            } finally {
                log.info("Finished reading from " + url + ", closing");
                closeQuietly(csvReader);
            }

        }
    }

    public static void main(String[] args) {
        EnsemblAnnotationLoader loader = new EnsemblAnnotationLoader();
//        loader.readAnnotations("xtropicalis_gene_ensembl");
        loader.readAnnotations("hsapiens_gene_ensembl");


//        try {
//            URL u = new URL(urlString + propertyQuery.replace(DATA_SET, "xtropicalis_gene_ensembl").replace(PROP_NAME, properties[2]));
//            InputStream in = u.openStream();
//
//            int b;
//            while ((b = in.read()) != -1) {
//                System.out.write(b);
//            }
//        } catch (MalformedURLException e) {
//            System.err.println(e);
//        } catch (IOException e) {
//            System.err.println(e);
//        }
    }

    @Override
    public void process(LoadBioentityCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSwDao(SoftwareDAO swDao) {
        this.swDao = swDao;
    }
}
