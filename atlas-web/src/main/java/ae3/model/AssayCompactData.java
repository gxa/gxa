package ae3.model;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 21, 2010
 * Time: 10:13:15 AM
 * This class represents compacted
 * 1. experimental factor values in an assay
 * 2. samples corresponding to this assay,
 * passed via Atlas API as JSON to experiment.js and used in constructing large plots.
 */
public class AssayCompactData {

    // Set of indexes of experimental factor value data stored in efvIndexes in Assay class and populated in
    // ExperimentalSample.addAssay() method
    private Set<Integer> efvIndexesForPlot;
    // Set of indexes of Samples corresponding to this assay, populated via Assay.addSample() method
    private Set<Integer> sampleIndexesForPlot;


    public AssayCompactData(Set<Integer> efvIndexesForPlot, Set<Integer> sampleIndexesForPlot) {
        this.efvIndexesForPlot = efvIndexesForPlot;
        this.sampleIndexesForPlot = sampleIndexesForPlot;
    }

    public Set<Integer> getEfvIdxs() {
        return efvIndexesForPlot;
    }

    public Set<Integer> getSampleIdxs() {
        return sampleIndexesForPlot;
    }
}
