/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model;

import uk.ac.ebi.gxa.Experiment;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class ExperimentImpl extends uk.ac.ebi.gxa.impl.ExperimentImpl {
    private boolean isprivate;
    private boolean curated;


    public static Experiment create(String accession) {
        return new ExperimentImpl(accession, 0);
    }

    public static Experiment create(String accession, long id) {
        return new ExperimentImpl(accession, id);
    }

    ExperimentImpl(String accession, long id) {
        super(accession, id);
    }

    public boolean isPrivate() {
        return isprivate;
    }

    public void setPrivate(boolean isprivate) {
        this.isprivate = isprivate;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }
}
