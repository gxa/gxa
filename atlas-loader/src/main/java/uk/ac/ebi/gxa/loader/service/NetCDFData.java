package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.data.DataMatrixStorage;
import uk.ac.ebi.gxa.data.KeyValuePair;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.*;

class NetCDFData {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    // Note that matchedUniqueEFVs includes both ef-efvs ad sc-scvs
    private EfvTree<CPair<String, String>> matchedUniqueEFVs = null;
    private DataMatrixStorage storage;
    private List<String> uniqueEFVs;
    private final Map<Assay, List<Sample>> assayToSamples = new LinkedHashMap<Assay, List<Sample>>();


    public void setStorage(DataMatrixStorage storage) {
        this.storage = storage;
    }

    public void addToStorage(String designElement, Iterator<Float> values) {
        storage.add(designElement, values);
    }

    public void setUniqueEFVs(List<KeyValuePair> uniqueValues) {
        // TODO: change this.uniqueEFVs to List of KeyValuePairs
        this.uniqueEFVs = new ArrayList<String>(uniqueValues.size());
        for (KeyValuePair pair : uniqueValues) {
            this.uniqueEFVs.add(pair.key + "||" + pair.value);
        }
    }

    public List<Assay> getAssays() {
        return new ArrayList<Assay>(assayToSamples.keySet());
    }

    public void addAssay(Assay assay) {
        assayToSamples.put(assay, assay.getSamples());
    }

    int getWidth() {
        return assayToSamples.keySet().size() + (isAnalyticsTransferred() ? uniqueEFVs.size() * 2 : 0);  // expressions + pvals + tstats
    }

    boolean isAnalyticsTransferred() {
        return matchedUniqueEFVs != null;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getTStatDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedUniqueEFVs.getNameSortedList()) {
            final int oldPos = uniqueEFVs.indexOf(encodeEfEfv(efEfv.getPayload()));
            tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assayToSamples.keySet().size() + uniqueEFVs.size() + oldPos));
        }
        return tstatMap;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getPValDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedUniqueEFVs.getNameSortedList()) {
            final int oldPos = uniqueEFVs.indexOf(encodeEfEfv(efEfv.getPayload()));
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

    //
    // TODO: there was the pattern-matching logic,
    // see rev. 48f0df44ce1fbaea42dff50167827d0138bd4eb1 for an attempt to fix it
    // and rev. 05be531ebb5a93df06d6045f982d0b25e4008a11 for nearly-original version
    //

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

        return efvTree;
    }
}
