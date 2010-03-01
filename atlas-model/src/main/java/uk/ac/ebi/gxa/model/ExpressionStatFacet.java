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

package uk.ac.ebi.gxa.model;

/**
 * @author pashky
 */
public class ExpressionStatFacet extends FacetQueryResultSet.FacetField<String,ExpressionStatFacet.UpDown> {
    public static class UpDown implements Comparable<UpDown> {
        private int up;
        private int down;

        public UpDown() {
            up = down = 0;
        }

        public void add(int v, boolean doUp)
        {
            if(doUp)
                up += v;
            else
                down += v;
        }

        public int getUp() {
            return up;
        }

        public int getDown() {
            return down;
        }

        public int compareTo(UpDown o) {
            // descending order
            return - Integer.valueOf(getDown() + getUp()).compareTo(o.getUp() + o.getDown());
        }
    }

    private String factor;

    public ExpressionStatFacet(String factor) {
        this.factor = factor;
    }

    public String getFactor() {
        return factor;
    }

    public UpDown createValue() {
        return new UpDown();
    }
}
