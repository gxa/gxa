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

package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

/**
 * User: nsklyar
 * Date: 04/01/2012
 */
@MappedSuperclass
public abstract class FileBasedAnnotationSource extends AnnotationSource {
    protected FileBasedAnnotationSource() {
    }

    protected FileBasedAnnotationSource(Software software) {
        super(software);
        this.name = createName();
    }

    @Override
    protected final String createName() {
        return getSoftware().getFullName();
    }

    public abstract char getSeparator();
}