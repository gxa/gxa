package ae3.service.structuredquery;

/**
 * @author pashky
*/
public interface ColumnInfo extends Comparable<ColumnInfo> {
    int getPosition();

    boolean isQualified(UpdownCounter ud);
}
