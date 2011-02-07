package uk.ac.ebi.gxa.requesthandlers.dump;

import ae3.util.FileDownloadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.tempFile;

/**
 * Class to dump a plan text file with rows containing the following tab-separate values:
 * <ol>
 * <li>experiment accession</li>
 * <li>sample/assay property</li>
 * <li>sample/assay property value</li>
 * <li>corresponding ontology term if exists; empty String otherwise</li>
 * </ol>
 */
public class ExperimentPropertiesDumpRequestHandler implements HttpRequestHandler, InitializingBean {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String TAB = "\t";

    private File dumpExperimentPropertiesFile;
    private AtlasProperties atlasProperties;
    private AtlasDAO atlasDAO;

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }


    public void setAtlasProperties(uk.ac.ebi.gxa.properties.AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void afterPropertiesSet() throws Exception {
        if (dumpExperimentPropertiesFile == null)
            dumpExperimentPropertiesFile = tempFile(atlasProperties.getExperimentsToPropertiesDumpFilename());
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene identifiers dump download request");
        if (!dumpExperimentPropertiesFile.exists() && dumpExperimentPropertiesFile.length() == 0)
            dumpExperimentProperties();
        FileDownloadServer.processRequest(dumpExperimentPropertiesFile, "text/plain", httpServletRequest, httpServletResponse);
    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     */
    void dumpExperimentProperties() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(dumpExperimentPropertiesFile));

            log.info("Writing experiment to properties mappings file from to " + dumpExperimentPropertiesFile + " ...");
            List<OntologyMapping> ontologyMappings = atlasDAO.getExperimentsToAllProperties();
            for (OntologyMapping ontologyMapping : ontologyMappings) {
                StringBuilder row = new StringBuilder();
                row.append(ontologyMapping.getExperimentAccession()).append(TAB);
                row.append(ontologyMapping.getProperty()).append(TAB);
                row.append(ontologyMapping.getPropertyValue()).append(TAB);
                if (ontologyMapping.getOntologyTerm() != null) {
                    row.append(ontologyMapping.getOntologyTerm());
                }
                out.write(row.toString());
                out.newLine();
            }
            log.info("Done writing experiment to properties mappings file from to " + dumpExperimentPropertiesFile);
        } catch (IOException e) {
            log.error("Failed to dump experiment to properties mappings to file: " + dumpExperimentPropertiesFile, e);
        } finally {
            closeQuietly(out);
        }
    }
}
