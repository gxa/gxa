package uk.ac.ebi.gxa.annotator.loader.genesig;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.loader.URLContentLoader;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.utils.FileUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/16/12 3:26 PM
 */
public class GeneSigAnnotationLoader {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private BioEntityAnnotationData.Builder builder;

    private final FileBasedAnnotationSource annotSource;

    private final HttpClient httpClient;

    public GeneSigAnnotationLoader(HttpClient httpClient, FileBasedAnnotationSource annotSource) {
        this.httpClient  = httpClient;
        this.annotSource = annotSource;
    }

    public void loadPropertyValues() throws AnnotationException, IOException, InvalidCSVColumnException {
        builder = new BioEntityAnnotationData.Builder();

        File contentAsFile = FileUtil.tempFile("genesig.tmp");
        try {
            new URLContentLoader(httpClient).getContentAsFile(annotSource.getUrl(), contentAsFile);
            parse(new FileInputStream(contentAsFile));
        } finally {
            if (!contentAsFile.delete()) {
                log.error("Couldn't delete temp file " + contentAsFile.getAbsolutePath());
            }
        }
    }

    public BioEntityAnnotationData getPropertyValuesData() throws InvalidAnnotationDataException {
        return builder.build(annotSource.getTypes());
    }

    private void parse(InputStream in) throws AnnotationException, IOException, InvalidCSVColumnException {
        List<ExternalBioEntityProperty> properties = annotSource.getNonIdentifierExternalProperties();
        Map<String, BioEntityType> name2Type = annotSource.getExternalName2TypeMap();

        CSVBasedReader reader = null;
        try {
            reader = new CSVBasedReader(in, annotSource.getSeparator(), '"');

            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                for (ExternalBioEntityProperty property : properties) {
                    BEPropertyValue propertyValue = new BEPropertyValue(property.getBioEntityProperty(), row.get(property.getName()));
                    for (Map.Entry<String, BioEntityType> entry : name2Type.entrySet()) {
                        builder.addPropertyValue(row.get(entry.getKey()), entry.getValue(), propertyValue);
                    }
                }
            }
        } finally {
            closeQuietly(reader);
        }
    }
}
