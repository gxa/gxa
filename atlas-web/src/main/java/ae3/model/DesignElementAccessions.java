package ae3.model;

/**
 * Design element accessions interface
 * Is used only in NetCDFReader and should be replaced with newer model classes.
 * @author ostolop
 */
public interface DesignElementAccessions {
    public String getDesignElementAccession(final int designElementIndex);
}