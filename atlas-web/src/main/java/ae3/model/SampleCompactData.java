package ae3.model;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 21, 2010
 * Time: 10:15:09 AM
 * This class represents compacted characteristic values for a sample, passed via Atlas API as JSON to experiment.js and
 * used in constructing large plots.
 */
public class SampleCompactData {

    // Set of indexes of sample characteristic value data stored in scvIndexes in Sample class and populated in
    // ExperimentalData.addSample() method
    private Set<Integer> scvIndexes;


    public SampleCompactData(Set<Integer> scvIndexes) {
        this.scvIndexes = scvIndexes;
    }

    public Set<Integer> getScvIdxs() {
        return scvIndexes;
    }
}
