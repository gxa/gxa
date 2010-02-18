package uk.ac.ebi.gxa.requesthandlers.api.result;

import java.util.Iterator;

/**
 * @author pashky
 */
public interface ApiQueryResults<ResultItem> {

    public long getTotalResults();

    public long getNumberOfResults();

    public long getStartingFrom();

    public Iterator<ResultItem> getResults();

}
