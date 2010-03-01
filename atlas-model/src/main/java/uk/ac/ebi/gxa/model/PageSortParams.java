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
 * http://ostolop.github.com/gxa/
 */

package  uk.ac.ebi.gxa.model;

/**
 * Parameters for paging-sorting: Start, Rows, SortOrder.
 * User: Andrey
 * Date: Oct 15, 2009
 * Time: 2:08:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageSortParams implements java.io.Serializable {

    public static final PageSortParams ALL = new PageSortParams(0, Integer.MAX_VALUE, "");

    protected int start = 0;

    public PageSortParams() {
    }

    public PageSortParams(int start, int rows, String sortOrder) {
        this.start = start;
        this.rows = rows;
        this.sortOrder = sortOrder;
    }

    public int getStart(){
        return start;
    }
    public PageSortParams setStart(int start){
        this.start = start;
        return this;
    }

    protected int rows = 100;
    
    public int getRows(){
        return rows;
    }
    public PageSortParams setRows(int rows){
        this.rows = rows;
        return this;
    }

    protected String sortOrder;
                             
    public PageSortParams orderBy(String sortOrder){  //rank_by_expression_algorithm_23
        this.sortOrder = sortOrder;
        return this;
    }
    public String getOrderBy(){
        return this.sortOrder;
    }

}