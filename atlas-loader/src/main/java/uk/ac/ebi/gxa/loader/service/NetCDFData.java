package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.*;

import static java.util.Collections.sort;

public class NetCDFData {
    private EfvTree<CPair<String, String>> matchedEfvs = null;
    final List<Assay> assays = new ArrayList<Assay>();
    DataMatrixStorage storage;
    List<String> uEFVs;

    int getWidth() {
        return assays.size() + (isAnalyticsTransferred() ? uEFVs.size() * 2 : 0);  // expressions + pvals + tstats
    }

    boolean isAnalyticsTransferred() {
        return matchedEfvs != null;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getTStatDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> tstatMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedEfvs.getNameSortedList()) {
            final int oldPos = uEFVs.indexOf(encodeEfEfv(efEfv.getPayload()));
            tstatMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assays.size() + uEFVs.size() + oldPos));
        }
        return tstatMap;
    }

    Map<Pair<String, String>, DataMatrixStorage.ColumnRef> getPValDataMap() {
        if (!isAnalyticsTransferred())
            return null;

        Map<Pair<String, String>, DataMatrixStorage.ColumnRef> pvalMap = new HashMap<Pair<String, String>, DataMatrixStorage.ColumnRef>();
        for (EfvTree.EfEfv<CPair<String, String>> efEfv : matchedEfvs.getNameSortedList()) {
            final int oldPos = uEFVs.indexOf(encodeEfEfv(efEfv.getPayload()));
            pvalMap.put(Pair.create(efEfv.getEf(), efEfv.getEfv()),
                    new DataMatrixStorage.ColumnRef(storage, assays.size() + oldPos));
        }
        return pvalMap;
    }

    private String encodeEfEfv(CPair<String, String> pair) {
        return pair.getFirst() + "||" + pair.getSecond();
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

                // So basically for each EF in the destination we find all the EFs having the same number of EFVs
                // and assume these are the same EFs as proven by comparing payloads, i.e. bit patterns
                // The very reason for it is, we can rename EFs, and we have no surrogate keys for them, so
                // we can only guess whether or not EFs are same. Still, as long as the number of EFVs stays the same
                // and assays are assigned to EFVs in the same manner, statistics don't change, hence we should be
                // safe to carry it over
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
        return result;
    }

    private List<EfvTree.Ef<CBitSet>> matchEfvsSort(EfvTree<CBitSet> efvTree) {
        final List<EfvTree.Ef<CBitSet>> fromTree = efvTree.getNameSortedTree();
        for (EfvTree.Ef<CBitSet> ef : fromTree) {
            sort(ef.getEfvs());
        }
        return fromTree;
    }
}
