package uk.ac.ebi.microarray.atlas.dao;

/**
 * An enumeration of possible states a process can occupy, indicating whether a task is pending, working, done, or
 * failed.
 *
 * @author Tony Burdett
 * @date 23-Nov-2009
 */
public enum LoadStatus {
    PENDING,
    WORKING,
    DONE,
    FAILED
}
