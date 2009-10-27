package  uk.ac.ebi.gxa.model;

import java.io.Serializable;
import java.util.Collection;

/**
 * Model of microarray, like "Affymetrix Custom Array - Novartis Human [gnGNF1Ba]".
 */
public interface ArrayDesign extends Serializable, Accessible {


    /**
     * "Sanger Institute S pombe array 3 1 1 template 4."
     * @return String
     */
    public String getName();

    /**
     * Type of microarray: "aminosilane","glass","in_situ_oligo_features","insitu experiment","non-adsorptive","proteomics","spotted_ds_DNA_features", etc.
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
     * design elements (microarray spots) in array design 
     * @return collection of design elements
     */
    public Collection<DesignElement> getDesignElements();

}