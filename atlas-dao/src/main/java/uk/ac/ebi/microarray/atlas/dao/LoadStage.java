package uk.ac.ebi.microarray.atlas.dao;

/**
 * An enumeration of possible stages that a process can occupy, indicating whether the current task is loading, creating
 * index entries, generating NetCDFs, and so on.
 *
 * @author Tony Burdett
 * @date 23-Nov-2009
 */
public enum LoadStage {
    LOAD,
    NETCDF,
    SIMILARITY,
    RANKING,
    SEARCHINDEX
}
