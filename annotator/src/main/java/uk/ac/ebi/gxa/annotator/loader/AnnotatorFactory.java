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

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

/**
 * User: nsklyar
 * Date: 03/01/2012
 */

public class AnnotatorFactory {

    @Autowired
    private AtlasBioEntityDataWriter beDataWriter;
    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private HttpClient httpClient;

    public BioMartAnnotator createBioMartAnnotator(BioMartAnnotationSource annSrc) {
        return new BioMartAnnotator(annSrc, annSrcDAO, propertyDAO, beDataWriter, httpClient);
    }

    public <T extends FileBasedAnnotationSource> FileBasedAnnotator createFileBasedAnnotator(T annSrc) {
        return new FileBasedAnnotator(annSrc, beDataWriter, httpClient);
    }
}
