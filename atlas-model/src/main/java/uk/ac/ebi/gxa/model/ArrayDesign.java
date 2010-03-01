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
 * http://ostolop.github.com/gxa/
 */

package  uk.ac.ebi.gxa.model;

import java.io.Serializable;
import java.util.Collection;

/**
 * Model of gxa, like "Affymetrix Custom Array - Novartis Human [gnGNF1Ba]".
 */
public interface ArrayDesign extends Serializable, Accessible {


    /**
     * "Sanger Institute S pombe array 3 1 1 template 4."
     * @return String
     */
    public String getName();

    /**
     * Type of gxa: "aminosilane","glass","in_situ_oligo_features","insitu experiment","non-adsorptive","proteomics","spotted_ds_DNA_features", etc.
     * @return String
     */
    public String getType();

    /**
     * Microarray manufacturer: "Affymetrix, Inc.", "Biological Sciences", "Genomics Laboratory, UMC Utrecht", etc.
     * @return set of assays
     */
    public String getProvider();

    /**
     * Microarray spot detecting paticular gene expression; 
     */
    public interface DesignElement{

        /**
         * reporter, reporterSequence, compositeSequence.
         * @return string type of design element
         */
        public String getType();
        
        //a2_designelement.name      is always Id
        //a2_designelement.iscontorl is always null

        /**
         * gene accession
         * @return string
         */
        public String getGeneAccession();
    }

    /**
     * design elements (gxa spots) in array design
     * @return collection of design elements
     */
    public Collection<DesignElement> getDesignElements();

}