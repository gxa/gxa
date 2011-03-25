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

import java.util.ArrayList;
import java.util.List;

/**
 * Heat map cell class
 *
 * @author pashky
 */
public class UpdownCounter implements Comparable<UpdownCounter> {
    private int ups;
    private int downs;
    private int nones;
    private float mpvup;
    private float mpvdn;
    public List<Long> experiments;

    public UpdownCounter() {
        this(0, 0, 0, 1, 1);
    }

    public UpdownCounter(int ups, int downs, int nones, float mpvup, float mpvdn) {
        this.ups = ups;
        this.downs = downs;
        this.mpvup = mpvup;
        this.mpvdn = mpvdn;
        this.nones = nones;
    }

    public int getUps() {
        return ups;
    }

    public int getDowns() {
        return downs;
    }

    /**
     * Method used to aggregate counts for over a whole column in a heatmap
     *
     * @param downs
     */
    public void addDowns(int downs) {
        this.downs += downs;
    }

    /**
     * Method used to aggregate counts for over a whole column in a heatmap
     *
     * @param ups
     */
    public void addUps(int ups) {
        this.ups += ups;
    }

    /**
     * Method used to aggregate counts for over a whole column in a heatmap
     *
     * @param nones
     */
    public void addNones(int nones) {
        this.nones += nones;
    }

    public int setNones(int nones) {
        return this.nones = nones;
    }

    public int getNones() {
        return nones;
    }

    public float getMpvUp() {
        return mpvup;
    }

    public float getMpvDn() {
        return mpvdn;
    }

    public void add(boolean isUp, float pvalue) {
        if (isUp) {
            ++ups;
            mpvup = Math.min(mpvup, pvalue);
        } else {
            ++downs;
            mpvdn = Math.min(mpvdn, pvalue);
        }
    }

    public void addNo() {
        ++nones;
    }

    public boolean isZero() {
        return getUps() + getDowns() + getNones() == 0;
    }

    public int compareTo(UpdownCounter o) {
        if (getUpDownDiffNoStudies() == o.getUpDownDiffNoStudies() && getNoStudies() != 0)
            // The if-else statement below ensures that in cases such as up:5,dn:2 and up:7, the latter always comes first.
            // Otherwise if up:5,dn:2's pVal was better than up:7's, the former would have shown up first
            if (getUps() != o.getUps())
                return -(getUps() - o.getUps());
            else if (getDowns() != o.getDowns())
                return -(getDowns() - o.getDowns());
            else
                return -Float.valueOf(getMpvUp() + getMpvDn()).compareTo(o.getMpvUp() + o.getMpvDn());
        else if (getNoStudies() == o.getNoStudies() && getNoStudies() == 0)
            return -(getNones() - o.getNones());
        else
            return -(getUpDownDiffNoStudies() - o.getUpDownDiffNoStudies());

    }

    public int getNoStudies() {
        return getUps() + getDowns();
    }

    /**
     * A mechanism to achieve sorting: up, then up/down, then down - in Collections of UpdownCounters (e.g. on heatmap
     * page and in efv tables on the gene page)
     *
     * @return number of up experiment counts - number of down experiment counts
     */
    private int getUpDownDiffNoStudies() {
        return getUps() - getDowns();
    }

    public void setExperiments(List<Long> experiments) {
        this.experiments = experiments;
    }

    public List<Long> getExperiments() {
        return this.experiments;
    }

    public void addExperiment(Long experimentID) {
        if (null == this.experiments)
            this.experiments = new ArrayList<Long>();

        this.experiments.add(experimentID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpdownCounter that = (UpdownCounter) o;

        if (downs != that.downs) return false;
        if (Float.compare(that.mpvdn, mpvdn) != 0) return false;
        if (Float.compare(that.mpvup, mpvup) != 0) return false;
        if (nones != that.nones) return false;
        if (ups != that.ups) return false;
        if (experiments != null ? !experiments.equals(that.experiments) : that.experiments != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ups;
        result = 31 * result + downs;
        result = 31 * result + nones;
        result = 31 * result + (mpvup != +0.0f ? Float.floatToIntBits(mpvup) : 0);
        result = 31 * result + (mpvdn != +0.0f ? Float.floatToIntBits(mpvdn) : 0);
        result = 31 * result + (experiments != null ? experiments.hashCode() : 0);
        return result;
    }
}
