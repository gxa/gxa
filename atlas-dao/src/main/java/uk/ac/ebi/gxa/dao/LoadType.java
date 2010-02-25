package uk.ac.ebi.gxa.dao;

/**
 * An enumeration of the possible types a load process can have.  This indicates whether a load process pertains to an
 * experiment or a gene.
 *
 * @author Tony Burdett
 * @date 07-Dec-2009
 */
public enum LoadType {
    EXPERIMENT,
    ARRAYDESIGN,
    GENE
}
