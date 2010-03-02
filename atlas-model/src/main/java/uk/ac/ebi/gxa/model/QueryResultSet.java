/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package  uk.ac.ebi.gxa.model;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of result objects plust metadata - total number of records.
 * User: Andrey
 * Date: Oct 2, 2009
 * Time: 3:03:38 PM
 * To change this template use File | Settings | File Templates.
 */

public class QueryResultSet<T> implements java.io.Serializable {
    private List<T> items;
    private int totalResults;
    private int startingFrom;
    private boolean isForceIsMulty = false; //return isMulty = true even for single item

    public List<T> getItems(){
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

    public void setItems(Collection<T> items){
        this.items = new ArrayList<T>(items);
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