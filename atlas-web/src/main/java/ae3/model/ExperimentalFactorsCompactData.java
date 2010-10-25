package ae3.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 22, 2010
 * Time: 9:24:40 AM
 * Class representing mapping between experimental factor values and assays, for a single experimental factor
 */
public class ExperimentalFactorsCompactData {
    // Experimental factor name
    private String name;
    // unique values corresponding to this class' experimental factor
    private List<String> uniqueEfvs = new ArrayList<String>();
    // This array is indexed by assayIndex and contains a position of an efv in uniqueEfvs that the corresponding assay contains
    private int[] assayEfvs;

    /**
     *
     * @param name Experimental factor name
     * @param numOfAssays Number of assays in the array design for which this data is being assembled
     */
    public ExperimentalFactorsCompactData(String name, Integer numOfAssays) {
        this.name = name;
        this.assayEfvs = new int[numOfAssays];
        uniqueEfvs.add(""); // Make sure that an empty value always has pos 0 in efvs, hence corresponds to an default initialised value 0 in int array, assayEfvs
    }

    /**
     * @return Experimental factor name
     */
    public String getName() {
        return name;
    }

    /**
     * @return unique values corresponding to this class' ef
     */
    public List<String> getEfvs() {
        return uniqueEfvs;
    }

    /**
     *
     * @return RLE-encoded List of efv positions in uniqueEfvs - when this list is decoded, contents of each (assay)
     *         index in this list determines what efv is contained in that assay, for this class' ef
     */
    public List<List<Integer>> getAssayEfvsRLE() {
        List<List<Integer>> assayEfvsRLE = new ArrayList<List<Integer>>();
        Integer cnt = 0;
        int prev = assayEfvs[0];
        for (int efvPos : assayEfvs) {
            if (efvPos != prev) {
                // Don't output cnt == 1 if a given value (prev) occurs only once
                assayEfvsRLE.add(createRLEArray(cnt, prev));
                cnt = 0;
                prev = efvPos;
            }
            cnt++;
        }
        if (cnt > 0) {
            assayEfvsRLE.add(createRLEArray(cnt, prev));
        }
        return assayEfvsRLE;
    }

    /**
     * @param cnt
     * @param val
     * @return Maximum 2-element List<Integer> where pos 0 contains value, and pos 1 contains the number of adjacent position in a RLE-decoded array with val in it
     *         Note that to avoid redundancy, pos 1 is populated only if cnt > 1
     */
    private List<Integer> createRLEArray(int cnt, int val) {
        List<Integer> rleVal = new ArrayList<Integer>();
        rleVal.add(val);
        if (cnt > 1) {
            rleVal.add(cnt);
        }
        return rleVal;
    }

    /**
     * Add efv from a Assay at assayIndex
     *
     * @param efv
     * @param assayIndex
     */
    public void addEfv(String efv, Integer assayIndex) {
        if (!uniqueEfvs.contains(efv)) {
            uniqueEfvs.add(efv);
        }
        assayEfvs[assayIndex] = uniqueEfvs.indexOf(efv);
    }
}
