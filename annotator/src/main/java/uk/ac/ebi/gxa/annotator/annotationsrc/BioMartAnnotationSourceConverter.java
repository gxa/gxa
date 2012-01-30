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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 07/11/2011
 */
class BioMartAnnotationSourceConverter extends AnnotationSourceConverter<BioMartAnnotationSource> {

    private static final String ORGANISM_PROPNAME = "organism";
    private static final String MYSQLDBNAME_PROPNAME = "mySqlDbName";
    private static final String MYSQLDBURL_PROPNAME = "mySqlDbUrl";
    private static final String DATASET_NAME_PROPNAME = "datasetName";
    private static final String DATABASE_NAME_PROPNAME = "databaseName";

    @Override
    protected Class<BioMartAnnotationSource> getClazz() {
        return BioMartAnnotationSource.class;
    }

    @Override
    protected BioMartAnnotationSource initAnnotationSource(BioMartAnnotationSource annSrc, Properties properties) throws AnnotationLoaderException {
        Organism organism = organismDAO.getOrCreateOrganism(getProperty(ORGANISM_PROPNAME, properties));
        Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

        if (annSrc == null) {
            return new BioMartAnnotationSource(software, organism);
        }

        if (!annSrc.getSoftware().equals(software)) {
             throw new AnnotationLoaderException("Software should not be changed when editing Annotation Source!");
        }

        if (!annSrc.getOrganism().equals(organism)) {
            throw new AnnotationLoaderException("Organism should not be changed when editing Annotation Source!");
        }
        return annSrc;
    }

    @Override
    protected void updateExtraProperties(Properties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
        annotationSource.setUrl(getProperty(URL_PROPNAME, properties));
        annotationSource.setDatabaseName(getProperty(DATABASE_NAME_PROPNAME, properties));
        annotationSource.setDatasetName(getProperty(DATASET_NAME_PROPNAME, properties));
        annotationSource.setMySqlDbName(getProperty(MYSQLDBNAME_PROPNAME, properties));
        annotationSource.setMySqlDbUrl(getProperty(MYSQLDBURL_PROPNAME, properties));
    }

    @Override
    protected void writeExtraProperties(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        properties.addProperty(ORGANISM_PROPNAME, annSrc.getOrganism().getName());
        properties.addProperty(DATABASE_NAME_PROPNAME, annSrc.getDatabaseName());
        properties.addProperty(DATASET_NAME_PROPNAME, annSrc.getDatasetName());
        properties.addProperty(MYSQLDBNAME_PROPNAME, annSrc.getMySqlDbName());
        properties.addProperty(MYSQLDBURL_PROPNAME, annSrc.getMySqlDbUrl());
    }

}
