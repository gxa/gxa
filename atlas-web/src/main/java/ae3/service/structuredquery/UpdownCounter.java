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

package ae3.service.structuredquery;

/**
 * @author pashky
*/
public class UpdownCounter {
    private int ups;
    private int downs;
    private double mpvup;
    private double mpvdn;

    public UpdownCounter(int ups, int downs, double mpvup, double mpvdn) {
        this.ups = ups;
        this.downs = downs;
        this.mpvup = mpvup;
        this.mpvdn = mpvdn;
    }

    public int getUps() {
        return ups;
    }

    public int getDowns() {
        return downs;
    }

    public double getMpvUp() {
        return mpvup;
    }

    public double getMpvDn() {
        return mpvdn;
    }

    public boolean isZero() {
        return getUps() + getDowns() == 0;
    }
}
