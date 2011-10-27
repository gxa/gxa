/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.AnnotationSourceConnection;
import uk.ac.ebi.gxa.annotator.loader.filebased.FileBasedConnection;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;

/**
 * User: nsklyar
 * Date: 22/06/2011
 */
public class AnnotationSourceConnectionFactory {

    public static <T extends AnnotationSource> AnnotationSourceConnection createConnectionForAnnSrc(T annSrc) throws BioMartAccessException {
        if (annSrc instanceof BioMartAnnotationSource) {
            BioMartAnnotationSource bmAnnSrc = (BioMartAnnotationSource) annSrc;
            return new BioMartConnection(bmAnnSrc.getUrl(), bmAnnSrc.getDatabaseName(), bmAnnSrc.getDatasetName());
        }
        if (annSrc instanceof GeneSigAnnotationSource) {
            GeneSigAnnotationSource gsAnnSrc = (GeneSigAnnotationSource) annSrc;
            return new FileBasedConnection(gsAnnSrc.getUrl());
        }
        throw new IllegalArgumentException("Cannot create Connection for " + annSrc.getClass().getName());
    }

    public static BioMartConnection createConnectionForAnnSrc(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        return new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
    }

    public static FileBasedConnection createConnectionForAnnSrc(GeneSigAnnotationSource annSrc) throws BioMartAccessException {
        return new FileBasedConnection(annSrc.getUrl());
    }
}
