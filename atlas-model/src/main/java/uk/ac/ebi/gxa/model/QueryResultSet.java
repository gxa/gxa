package  uk.ac.ebi.gxa.model;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Collection of result objects plust metadata - total number of records.
 * User: Andrey
 * Date: Oct 2, 2009
 * Time: 3:03:38 PM
 * To change this template use File | Settings | File Templates.
 */

public class QueryResultSet<T> implements java.io.Serializable {
    private Collection<T> items;
    private int totalResults;
    private int startingFrom;
    private boolean isForceIsMulty = false; //return isMulty = true even for single item

    public Iterable<T> getItems(){
        return items;
    }
    public T getItem(){
        if(null==items)
            return null;

        return (items.size() > 0 ? items.iterator().next() : null );
    }

    public void setItem(T item){
        this.items = new ArrayList<T>();
        this.items.add(item);
    }

    public void setItems(Iterable<T> items){
        this.items = new ArrayList<T>();

        for(T t : items)
            this.items.add(t);
    }

    public boolean isMulti() {
        if(isForceIsMulty)
            return true;

        if(null==items)
            return false;

            return items.size() > 1;
    }

    public void setIsMulti(boolean isMulty){
        if (isMulty)
            this.isForceIsMulty = true;
    }
        public boolean isFound() {
            if(null==items)
                return false;

            return items.size() > 0;
        }

    public int getTotalResults() {
        return totalResults;
    }
    public void setTotalResults(int totalResults){
        this.totalResults = totalResults;
    }

    public int getStartingFrom() {
        return startingFrom;
    }

    public void setStartingFrom(int startingFrom){
        this.startingFrom = startingFrom;
    }

    public int getNumberOfResults() {
        return items.size();
    }

}