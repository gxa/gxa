package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.CBitSet;
import uk.ac.ebi.gxa.utils.CPair;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.Maker;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.*;

import static java.util.Collections.sort;

public class NetCDFData {
    EfvTree<CPair<String, String>> matchedEfvs = null;
    List<Assay> assays = new ArrayList<Assay>();
    DataMatrixStorage storage;
    String[] uEFVs;

    void matchEfvPatterns(EfvTree<CBitSet> oldEfvPats) {
        matchedEfvs = matchEfvs(oldEfvPats, getEfvPatternsFromAssays());
    }

    EfvTree<CBitSet> getEfvPatternsFromAssays() {
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
            for (EfvTree.Ef<CBitSet> fromEf : fromTree) {
                if (fromEf.getEfvs().size() != toEf.getEfvs().size()) {
                    continue;
                }
                int i;
                for (i = 0; i < fromEf.getEfvs().size(); ++i)
                    if (!fromEf.getEfvs().get(i).getPayload().equals(toEf.getEfvs().get(i).getPayload()))
                        return null;
                if (i == fromEf.getEfvs().size()) {
                    for (i = 0; i < fromEf.getEfvs().size(); ++i)
                        result.put(toEf.getEf(), toEf.getEfvs().get(i).getEfv(),
                                new CPair<String, String>(fromEf.getEf(), fromEf.getEfvs().get(i).getEfv()));
                }
            }
        }
        return result;
    }

    private List<EfvTree.Ef<CBitSet>> matchEfvsSort(EfvTree<CBitSet> from) {
        final List<EfvTree.Ef<CBitSet>> fromTree = from.getNameSortedTree();
        for (EfvTree.Ef<CBitSet> ef : fromTree) {
            sort(ef.getEfvs(), new Comparator<EfvTree.Efv<CBitSet>>() {
                public int compare(EfvTree.Efv<CBitSet> o1, EfvTree.Efv<CBitSet> o2) {
                    return o1.getPayload().compareTo(o2.getPayload());
                }
            });
        }
        return fromTree;
    }

}
