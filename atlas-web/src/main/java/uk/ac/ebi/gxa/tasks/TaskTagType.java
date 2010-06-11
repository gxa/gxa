package uk.ac.ebi.gxa.tasks;

/**
 * Task log messages have a conception of "tags" and "task groups". Each scheduled task may trigger a whole
 * cascade of dependent task automatically scheduled upon original task completion. All those task executions
 * are considered related to each other and each of them may be realted to handling of some object like particular
 * experiment or array design accession or load URL. So, the whole group gets a bunch of tags like
 * "experiment=E-AFMX-5", "url=http://ae.uk/e-afmx-5.idf.txt" and "arraydesign=A-AFFY-123" so later one can search
 * for all the log messages somehow related (may be indirectly as arraydesign relates to experiment load, that's the
 * whole point) to particular object.
 *
 * This class represents all existing types of tagged objects
 *
 * @author pashky
 */
public enum TaskTagType {
    EXPERIMENT,
    ARRAYDESIGN,
    URL,
    OTHER
}
