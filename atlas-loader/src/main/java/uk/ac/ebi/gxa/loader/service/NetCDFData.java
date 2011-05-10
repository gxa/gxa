package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.*;

import static java.util.Collections.sort;

class NetCDFData {
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

    public void addAssay(Assay assay, List<Sample> samples) {
        assayToSamples.put(assay, samples);
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

    void matchValuePatterns(EfvTree<CBitSet> oldEfvPats) {
        matchedUniqueValues = matchUniqueValues(oldEfvPats, getValuePatterns());
    }

    EfvTree<CBitSet> getValuePatterns() {
        Set<String> properties = new HashSet<String>();

        // First store assay patterns
        final Set<Assay> assays = assayToSamples.keySet();
        for (Assay assay : assays)
            for (AssayProperty property : assay.getProperties())
                properties.add(property.getName());

        EfvTree<CBitSet> efvTree = new EfvTree<CBitSet>();
        int i = 0;
        for (Assay assay : assays) {
            for (final String propName : properties) {
                String value = assay.getPropertySummary(propName);
                efvTree.getOrCreateCaseSensitive(propName, value, new Maker<CBitSet>() {
                    public CBitSet make() {
                        return new CBitSet(assays.size());
                    }
                }).set(i, true);
            }
            ++i;
        }

        // Now add to efvTree sample patterns
        properties = new HashSet<String>();
        for (Map.Entry<Assay, List<Sample>> entry : assayToSamples.entrySet()) {
            for (Sample sample : entry.getValue()) {
                for (SampleProperty property : sample.getProperties())
                    properties.add(property.getName());
            }
        }

        i = 0;
        for (Map.Entry<Assay, List<Sample>> entry : assayToSamples.entrySet()) {
            final List<Sample> samples = entry.getValue();
            for (Sample sample : samples) {
                for (final String propName : properties) {
                    String value = sample.getPropertySummary(propName);
                    efvTree.getOrCreateCaseSensitive(propName, value, new Maker<CBitSet>() {
                        public CBitSet make() {
                            return new CBitSet(samples.size());
                        }
                    }).set(i, true);
                }
                ++i;
            }
        }

        return efvTree;
    }

    private EfvTree<CPair<String, String>> matchUniqueValues
            (EfvTree<CBitSet> from, EfvTree<CBitSet> to) {
        final List<EfvTree.Ef<CBitSet>> fromTree = matchValuesSort(from);
        final List<EfvTree.Ef<CBitSet>> toTree = matchValuesSort(to);

        EfvTree<CPair<String, String>> result = new EfvTree<CPair<String, String>>();
        for (EfvTree.Ef<CBitSet> toProperty : toTree) {
            List<EfvTree.Efv<CBitSet>> dest = toProperty.getEfvs();

            boolean matched = false;
            for (EfvTree.Ef<CBitSet> fromProperty : fromTree) {
                List<EfvTree.Efv<CBitSet>> src = fromProperty.getEfvs();
                if (src.size() != dest.size()) {
                    continue;
                }

                // So basically for each EF/SC in the destination we find all the EF/SCs having the same number of EFVs/SCVs
                // and assume these are the same EFs/SCs as proven by comparing payloads, i.e. bit patterns
                // The very reason for it is, we can rename EFs/SCs, and we have no surrogate keys for them, so
                // we can only guess whether or not EFs/SCs are same. Still, as long as the number of EFVs stays the same
                // and assays are assigned to EFVs in the same manner, statistics don't change, hence we should be
                // safe to carry it over
                if (!src.equals(dest))
                    return null;

                for (int i = 0; i < src.size(); ++i)
                    result.putCaseSensitive(toProperty.getEf(), dest.get(i).getEfv(),
                            new CPair<String, String>(fromProperty.getEf(), src.get(i).getEfv()));
                matched = true;
            }
            if (!matched)
                return null;
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
