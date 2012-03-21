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

import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.*;


/**
 * User: nsklyar
 * Date: 07/11/2011
 */
class BioMartAnnotationSourceConverter extends AnnotationSourceConverter<BioMartAnnotationSource> {

    @Override
    protected BioMartAnnotationSource initAnnotationSource(AnnotationSourceProperties properties){
        Organism organism = organismDAO.getOrCreateOrganism(properties.getProperty(ORGANISM_PROPNAME));
        Software software = softwareDAO.findOrCreate(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));

        return new BioMartAnnotationSource(software, organism);
    }

    @Override
    protected boolean annSrcExists(BioMartAnnotationSource annSrc) {
        return annSrcDAO.findBioMartAnnotationSource(annSrc.getSoftware(), annSrc.getOrganism())!= null;
    }

    @Override
    protected void updateExtraProperties(AnnotationSourceProperties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
        annotationSource.setUrl(properties.getProperty(URL_PROPNAME));
        annotationSource.setDatabaseName(properties.getProperty(DATABASE_NAME_PROPNAME));
        annotationSource.setDatasetName(properties.getProperty(DATASET_NAME_PROPNAME));
        annotationSource.setMySqlDbName(properties.getProperty(MYSQLDBNAME_PROPNAME));
        annotationSource.setMySqlDbUrl(properties.getProperty(MYSQLDBURL_PROPNAME));
    }

    @Override
    protected void writeExtraProperties(BioMartAnnotationSource annSrc, AnnotationSourceProperties properties) {
        properties.addProperty(ORGANISM_PROPNAME, annSrc.getOrganism().getName());
        properties.addProperty(DATABASE_NAME_PROPNAME, annSrc.getDatabaseName());
        properties.addProperty(DATASET_NAME_PROPNAME, annSrc.getDatasetName());
        properties.addProperty(MYSQLDBNAME_PROPNAME, annSrc.getMySqlDbName());
        properties.addProperty(MYSQLDBURL_PROPNAME, annSrc.getMySqlDbUrl());
    }

}
