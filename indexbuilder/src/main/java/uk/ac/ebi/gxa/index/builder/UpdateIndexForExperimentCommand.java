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

package uk.ac.ebi.gxa.index.builder;

/**
 * Update index for experiment by accession index builder command
 * @author pashky
 */
public class UpdateIndexForExperimentCommand implements IndexBuilderCommand {
    private String accession;

    /**
     * Creates command for specific experiment by accession
     * @param accession experiment accession
     */
    public UpdateIndexForExperimentCommand(String accession) {
        this.accession = accession;
    }

    /**
     * Returns experiment accession
     * @return acession string
     */
    public String getAccession() {
        return accession;
    }

    public void visit(IndexBuilderCommandVisitor visitor) throws IndexBuilderException {
        visitor.process(this);
    }

    @Override
    public String toString() {
        return "Reindex for experiment " + getAccession();
    }
}
