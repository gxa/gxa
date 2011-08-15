package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.CBitSet;
import uk.ac.ebi.gxa.utils.CPair;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.*;

import static java.util.Collections.sort;

class NetCDFData {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    // Note that matchedUniqueValues includes both ef-efvs ad sc-scvs
    private EfvTree<CPair<String, String>> matchedUniqueValues = null;
    private DataMatrixStorage storage;
    private List<String> uniqueValues; // scvs/efvs
    private final Map<Assay, List<Sample>> assayToSamples = new LinkedHashMap<Assay, List<Sample>>();


    public void setStorage(DataMatrixStorage storage) {
        this.storage = storage;
    }

    public void addToStorage(String designElement, Iterator<Float> values) {
        storage.add(designElement, values);
    }

    public void setUniqueValues(List<String> uniqueValues) {
        this.uniqueValues = uniqueValues;
    }

    public List<Assay> getAssays() {
        return new ArrayList<Assay>(assayToSamples.keySet());
    }

    public void addAssay(Assay assay) {
        assayToSamples.put(assay, assay.getSamples());
    }

    int getWidth() {
        return assayToSamples.keySet().size() + (isAnalyticsTransferred() ? uniqueValues.size() * 2 : 0);  // expressions + pvals + tstats
    }

    boolean isAnalyticsTransferred() {
        return matchedUniqueValues != null;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getTStatDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedUniqueValues.getNameSortedList()) {
            final int oldPos = uniqueValues.indexOf(encodeEfEfv(efEfv.getPayload()));
            tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assayToSamples.keySet().size() + uniqueValues.size() + oldPos));
        }
        return tstatMap;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getPValDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedUniqueValues.getNameSortedList()) {
            final int oldPos = uniqueValues.indexOf(encodeEfEfv(efEfv.getPayload()));
            pvalMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assayToSamples.keySet().size() + oldPos));
        }
        return pvalMap;
    }

    private String encodeEfEfv(CPair<String, String> pair) {
        return pair.getFirst() + "||" + pair.getSecond();
    }

    Map<String, DataMatrixStorage.ColumnRef> getAssayDataMap() {
        Map<String, DataMatrixStorage.ColumnRef> result = new HashMap<String, DataMatrixStorage.ColumnRef>();
        int i = 0;
        for (Assay assay : assayToSamples.keySet()) {
            result.put(assay.getAccession(), new DataMatrixStorage.ColumnRef(storage, i));
            i++;
        }
        return result;
    }

    private EfvTree<CPair<String, String>> matchUniqueValues
            (EfvTree<CBitSet> from, EfvTree<CBitSet> to) {
        final List<EfvTree.Ef<CBitSet>> fromTree = matchValuesSort(from);
        final List<EfvTree.Ef<CBitSet>> toTree = matchValuesSort(to);

        EfvTree<CPair<String, String>> result = new EfvTree<CPair<String, String>>();
        for (EfvTree.Ef<CBitSet> toProperty : toTree) {
            List<EfvTree.Efv<CBitSet>> dest = new ArrayList<EfvTree.Efv<CBitSet>>(toProperty.getEfvs());
            Collections.sort(dest);

            boolean matched = false;
            for (EfvTree.Ef<CBitSet> fromProperty : fromTree) {
                List<EfvTree.Efv<CBitSet>> src = new ArrayList<EfvTree.Efv<CBitSet>>(fromProperty.getEfvs());
                Collections.sort(src);

                // So basically for each EF/SC in the destination we find all the EF/SCs having the same number of EFVs/SCVs
                // and assume these are the same EFs/SCs as proven by comparing payloads, i.e. bit patterns
                // The very reason for it is, we can rename EFs/SCs, and we have no surrogate keys for them, so
                // we can only guess whether or not EFs/SCs are same. Still, as long as the number of EFVs stays the same
                // and assays are assigned to EFVs in the same manner, statistics don't change, hence we should be
                // safe to carry it over
                if (!src.equals(dest))
                    continue;

                for (int i = 0; i < src.size(); ++i)
                    result.putCaseSensitive(toProperty.getEf(), dest.get(i).getEfv(),
                            new CPair<String, String>(fromProperty.getEf(), src.get(i).getEfv()));
                matched = true;
            }

            if (!matched) {
                log.info("NetCDF and DB EF/SC values not matched -- will need to recompute statistics");
                return null;
            }
        }

        return result;
    }

    private List<EfvTree.Ef<CBitSet>> matchValuesSort(EfvTree<CBitSet> efvTree) {
        final List<EfvTree.Ef<CBitSet>> fromTree = efvTree.getNameSortedTree();
        for (EfvTree.Ef<CBitSet> ef : fromTree) {
            sort(ef.getEfvs());
        }
        return fromTree;
    }
}
