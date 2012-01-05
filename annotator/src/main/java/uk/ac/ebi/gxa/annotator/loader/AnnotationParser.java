/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.annotator.loader;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityDataBuilder;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class AnnotationParser<T extends BioEntityData> {

    final private List<BioEntityType> bioEntityTypes;
    private BioEntityDataBuilder<T> builder;

    final private Logger log = LoggerFactory.getLogger(this.getClass());
    private char separator = '\t';

    public static <T extends BioEntityData> AnnotationParser<T> initParser(List<BioEntityType> types, BioEntityDataBuilder<T> builder) {
        AnnotationParser<T> parser = new AnnotationParser<T>(types);
        parser.setBuilder(builder);
        parser.createNewBioEntityData();
        return parser;
    }

    private AnnotationParser(List<BioEntityType> bioEntityTypes) {
        this.bioEntityTypes = bioEntityTypes;
    }

    public void createNewBioEntityData() {
        builder.createNewData(bioEntityTypes);
    }

    public void parseBioEntities(URL url, Organism organism) throws AnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), separator, '"');
            String[] line;

            while ((line = csvReader.readNext()) != null) {
                if (line.length < bioEntityTypes.size() || line[0].contains("Exception")) {
                    throw new AnnotationException("Cannot update Bioentities for Organism.Problem when connecting to biomart: " + organism.getName());
                }

                int columnCount = 0;
                for (BioEntityType type : bioEntityTypes) {
                    String beIdentifier = line[columnCount++];
                    builder.addBioEntity(beIdentifier, type, organism);
                }
            }
        } catch (IOException e) {
            throw new AnnotationException("Cannot update Bioentities for Organism.Problem when connecting to biomart: " + organism.getName(), e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    public void parsePropertyValues(BioEntityProperty property, URL url) throws AnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), separator, '"');

            String[] line;
            int lineCount = 0;

            while ((line = csvReader.readNext()) != null) {
                validateLine(line, "Cannot get property " + property.getName() + "from " + url);

                BEPropertyValue propertyValue = new BEPropertyValue(property, line[bioEntityTypes.size()]);
                int count = 0;
                for (BioEntityType type : bioEntityTypes) {
                    builder.addPropertyValue(line[count++], type, propertyValue);
                }

                if (lineCount++ % 2000 == 0) {
                    log.debug("Parsed " + lineCount + " properties values");
                }

            }
        } catch (IOException e) {
            throw new AnnotationException("Cannot get property " + property.getName() + "from " + url, e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    public void parsePropertyValues(Collection<BioEntityProperty> properties, InputStream input, boolean isFirstLineHeader) throws AnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(input), separator, '"');

            String[] line;
            int lineCount = 0;

            if (isFirstLineHeader && (line = csvReader.readNext()) != null) {
                //ToDo: match column names and property names
                validateLine(line, "Annotation data are not valid");
            }

            while ((line = csvReader.readNext()) != null) {

                validateLine(line, "Annotation data are not valid");

                int propertyCount = 0;
                for (BioEntityProperty property : properties) {
                    BEPropertyValue propertyValue = new BEPropertyValue(property, line[bioEntityTypes.size() + propertyCount++].trim());
                    int typeCount = 0;
                    for (BioEntityType type : bioEntityTypes) {
                        builder.addPropertyValue(line[typeCount++].trim(), type, propertyValue);
                    }

                    if (lineCount % 2000 == 0) {
                        log.debug("Parsed " + lineCount + " properties values");
                    }
                }

            }
        } catch (IOException e) {
            throw new AnnotationException("Cannot get access annotation data", e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    public void parseDesignElementMappings(URL url) throws AnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), separator, '"');

            String[] line;
            int lineCount = 0;

            while ((line = csvReader.readNext()) != null) {
                validateLine(line, "Cannot update design element mappings from " + url);
                String deAcc = line[bioEntityTypes.size()];
                int count = 0;
                for (BioEntityType type : bioEntityTypes) {
                    builder.addBEDesignElementMapping(line[count++], type, deAcc);
                }

                if (lineCount++ % 2000 == 0) {
                    log.debug("Parsed " + lineCount + " design element mappings");
                }

            }
        } catch (IOException e) {
            throw new AnnotationException("Cannot update design element mappings", e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    private void validateLine(String[] line, String exceptionMsg) throws AnnotationException {

        if (line.length < bioEntityTypes.size() + 1 || line[0].contains("Exception")) {
            log.debug("{} line: {}", exceptionMsg, Arrays.toString(line));
            throw new AnnotationException(exceptionMsg);
        }
    }

    void setBuilder(BioEntityDataBuilder<T> builder) {
        this.builder = builder;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public T getData() throws AnnotationException {
        return builder.getBioEntityData();
    }

    private Reader getReader(URL url) throws IOException {
        return new InputStreamReader(url.openStream());
    }
}
