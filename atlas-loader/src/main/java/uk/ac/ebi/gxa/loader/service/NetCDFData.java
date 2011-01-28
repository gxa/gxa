package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.*;

import static java.util.Collections.sort;

public class NetCDFData {
    EfvTree<CPair<String, String>> matchedEfvs = null;
    List<Assay> assays = new ArrayList<Assay>();
    DataMatrixStorage storage;
    String[] uEFVs;

    boolean isAnalyticsTransferred() {
        return matchedEfvs != null;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getTStatDataMap() {
        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedEfvs.getNameSortedList()) {
            final int oldPos = Arrays.asList(uEFVs).indexOf(efEfv.getPayload().getFirst() + "||" + efEfv.getPayload().getSecond());
            tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assays.size() + uEFVs.length + oldPos));
        }
        return tstatMap;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getPValDataMap() {
        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedEfvs.getNameSortedList()) {
            final int oldPos = Arrays.asList(uEFVs).indexOf(efEfv.getPayload().getFirst() + "||" + efEfv.getPayload().getSecond());
            pvalMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assays.size() + oldPos));
        }
        return pvalMap;
    }

    Map<String, DataMatrixStorage.ColumnRef> getAssayDataMap() {
        Map<String, DataMatrixStorage.ColumnRef> result = new HashMap<String, DataMatrixStorage.ColumnRef>();
        for (int i = 0; i < assays.size(); ++i)
            result.put(assays.get(i).getAccession(), new DataMatrixStorage.ColumnRef(storage, i));
        return result;
    }

    void matchEfvPatterns(EfvTree<CBitSet> oldEfvPats) {
        matchedEfvs = matchEfvs(oldEfvPats, getEfvPatterns());
    }

    EfvTree<CBitSet> getEfvPatterns() {
        Set<String> efs = new HashSet<String>();
        for (Assay assay : assays)
            efs.addAll(assay.getPropertyNames());

        EfvTree<CBitSet> efvTree = new EfvTree<CBitSet>();
        int i = 0;
        for (Assay assay : assays) {
            for (final String propName : efs) {
                String value = assay.getPropertySummary(propName);
                efvTree.getOrCreate(propName, value, new Maker<CBitSet>() {
                    public CBitSet make() {
                        return new CBitSet(assays.size());
                    }
                }).set(i, true);
            }
            ++i;
        }

        return efvTree;
    }

    private EfvTree<CPair<String, String>> matchEfvs(EfvTree<CBitSet> from, EfvTree<CBitSet> to) {
        final List<EfvTree.Ef<CBitSet>> fromTree = matchEfvsSort(from);
        final List<EfvTree.Ef<CBitSet>> toTree = matchEfvsSort(to);

        EfvTree<CPair<String, String>> result = new EfvTree<CPair<String, String>>();
        for (EfvTree.Ef<CBitSet> toEf : toTree) {
            List<EfvTree.Efv<CBitSet>> dest = toEf.getEfvs();

            boolean matched = false;
            for (EfvTree.Ef<CBitSet> fromEf : fromTree) {
                List<EfvTree.Efv<CBitSet>> src = fromEf.getEfvs();
                if (src.size() != dest.size()) {
                    continue;
                }

                // TODO: why we consider different size to be less important than different content?
                // ok, we're looking for a match rather than checking equality.
                // but that means, we'll get null in case there are two EFs with same number of EFVs, don't you think?
                if (!src.equals(dest))
                    return null;

                for (int i = 0; i < src.size(); ++i)
                    result.put(toEf.getEf(), dest.get(i).getEfv(),
                            new CPair<String, String>(fromEf.getEf(), src.get(i).getEfv()));
                matched = true;
            }
            if (!matched)
                return null;
        }
        return result; // TODO: what if all the sizes were different? We should get an empty tree then
    }

    private List<EfvTree.Ef<CBitSet>> matchEfvsSort(EfvTree<CBitSet> efvTree) {
        final List<EfvTree.Ef<CBitSet>> fromTree = efvTree.getNameSortedTree();
        for (EfvTree.Ef<CBitSet> ef : fromTree) {
            sort(ef.getEfvs());
        }
        return fromTree;
    }
}
