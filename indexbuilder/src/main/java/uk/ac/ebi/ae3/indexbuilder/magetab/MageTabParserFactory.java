package uk.ac.ebi.ae3.indexbuilder.magetab;


/**
 * User: ostolop
 * Date: 18-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class MageTabParserFactory {
    public static MageTabParser getParser() {
        return new MageTabParser();
    }
}
