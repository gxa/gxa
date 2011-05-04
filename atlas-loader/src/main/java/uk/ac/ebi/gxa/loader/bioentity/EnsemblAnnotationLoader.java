package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
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

    private BioMartDAO bioMartDAO = new BioMartDAO();
    private SoftwareDAO swDao;

    public static final String ENS_TRANSCRIPT = "enstranscript";
    public static final String ENS_GENE = "ensgene";

    public void updateAnnotaions(List<String> organisms) throws AtlasLoaderException {
        for (String organism : organisms) {
            updateAnnotationsForOrganism(organism);
        }
    }

    private void updateAnnotationsForOrganism(String ensOrganism) throws AtlasLoaderException {
        //check latest Ens version from registry
        String ensemblVersion = bioMartDAO.getEnsemblVersion();

        //ToDo: check version for a particular ensOrganism
        String dbVersion = swDao.getLatestVersionOfSoftware(SoftwareDAO.ENSEMBL);

        if (StringUtils.isNotEmpty(ensemblVersion) && StringUtils.isNotEmpty(dbVersion) && ensemblVersion.equals(dbVersion)) {
            log.info("Ensembl versions in BioMart and in Atlas are the same!");

        }

        setSource(SoftwareDAO.ENSEMBL);
        setVersion(ensemblVersion);

        Map<String, String> organismToEnsOrgName = bioEntityDAO.getOrganismToEnsOrgName();
        Multimap<String, String> propertyToEnsPropNames = bioEntityDAO.getPropertyToEnsPropNames();
        CSVReader csvReader = null;
        boolean beExist = false;
        try {
            if (organismToEnsOrgName.containsKey(ensOrganism)) {
                setOrganism(organismToEnsOrgName.get(ensOrganism));
                reportProgress("Reading Ensembl annotations for organism " + getOrganism());
                for (String atlasProperty : propertyToEnsPropNames.keySet()) {
                    for (String ensProperty : propertyToEnsPropNames.get(atlasProperty)) {
                        URL url = bioMartDAO.getPropertyForOrganismURL(ensOrganism, ensProperty);
                        if (url != null) {
                            reportProgress("Reading " + atlasProperty + " for " + getOrganism());
                            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                            readProperty(csvReader, ensOrganism, atlasProperty, beExist);
                            csvReader.close();
                            beExist = true;
                        }
                    }
                }
            } else {
                //ToDo: maybe throw an exception
                log.info("Organism " + ensOrganism + " is not mapped to Ensembl data set.");
            }

            writeBioentitiesAndAnnotations(ENS_TRANSCRIPT, ENS_GENE);

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for ensOrganism " + ensOrganism, e);
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

            String beIdentifier = line[1];
            String geneName = line[0];
            String value = line[2];


            addPropertyValue(beIdentifier, geneName, propertyName, value);

            if (!beExist) {
                addTranscriptGeneMapping(beIdentifier, geneName);
                addGene(organism, ENS_GENE, geneName);
                addTransctipt(organism, ENS_TRANSCRIPT, beIdentifier);
            }


        }
    }

    public void process(UpdateAnnotationCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);
        updateAnnotationsForOrganism(command.getAccession());
    }

    public void setSwDao(SoftwareDAO swDao) {
        this.swDao = swDao;
    }
}
