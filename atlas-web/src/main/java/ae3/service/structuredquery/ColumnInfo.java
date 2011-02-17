package ae3.service.structuredquery;

/**
 * Column information class to be used as paylod in result EFV tree.
 * @author pashky
*/
public interface ColumnInfo extends Comparable<ColumnInfo> {
    /**
     * Returns position of column in result counters array
     * @return
     */
    int getPosition();

    /**
     * Checks if provided experiment counts are qualified for this particular EFV
     * @param ud counter
     * @return true or false
     */
    boolean isQualified(UpdownCounter ud);

    /**
     *
     * @return true if position has been set to a valid value
     */
    boolean isPositionSet();
}
