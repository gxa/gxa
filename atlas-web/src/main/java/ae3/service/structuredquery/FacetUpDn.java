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

package ae3.service.structuredquery;

/**
 * @author pashky
*/
public class FacetUpDn implements Comparable<FacetUpDn> {
    private int up;
    private int down;

    public FacetUpDn() {
        up = down = 0;
    }

    void add(int v, boolean doUp)
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

    public int compareTo(FacetUpDn o) {
        // descending order
        return - Integer.valueOf(getDown() + getUp()).compareTo(o.getUp() + o.getDown());
    }

}
