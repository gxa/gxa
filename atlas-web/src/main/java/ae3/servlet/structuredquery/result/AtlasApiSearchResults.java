package ae3.servlet.structuredquery.result;

import java.util.Iterator;

/**
 * @author pashky
 */
public interface AtlasApiSearchResults<ResultItem> {

    public long getTotalResults();

    public long getNumberOfResults();

    public long getStartingFrom();

    public Iterator<ResultItem> getResults();

}
