package ae3.service.experiment;

import ae3.model.AtlasGene;

import java.util.*;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class BestDesignElementsResult implements Iterable<BestDesignElementsResult.Item> {

    private long total = 0;
    private final List<AtlasGene> genes = new ArrayList<AtlasGene>();
    private final List<String> deAccessions = new ArrayList<String>();
    private final List<Integer> deIndexes = new ArrayList<Integer>();
    private final List<Double> pvalues = new ArrayList<Double>();
    private final List<Double> tvalues = new ArrayList<Double>();
    private final List<String> efs = new ArrayList<String>();
    private final List<String> efvs = new ArrayList<String>();

    BestDesignElementsResult() {
    }

    void add(AtlasGene gene, long deAccession, int deIndex, double pval, double tstat, String ef, String efv) {
        genes.add(gene);
        deAccessions.add(deAccession);
        deIndexes.add(deIndex);
        pvalues.add(pval);
        tvalues.add(tstat);
        efs.add(ef);
        efvs.add(efv);
    }

    void setTotalSize(long total) {
        this.total = total;
    }

    public Item get(int i) {
        return new Item(genes.get(i),
                deAccessions.get(i),
                deIndexes.get(i),
                pvalues.get(i).floatValue(),
                tvalues.get(i).floatValue(),
                efs.get(i),
                efvs.get(i));
    }

    public long getTotalSize() {
        return total;
    }

    public int getPageSize() {
        return genes.size();
    }

    public Iterator<Item> iterator() {
        return new Iterator<Item>() {
            private int i = 0;

            public boolean hasNext() {
                return i < getPageSize();
            }

            public Item next() {
                return get(i++);
            }

            public void remove() {
                throw new IllegalStateException("The operation is not supported");
            }
        };
    }

    public Collection<AtlasGene> getGenes() {
        return Collections.unmodifiableCollection(genes);
    }

    public Collection<Integer> getDeIndexes() {
        return Collections.unmodifiableCollection(deIndexes);
    }

    public static BestDesignElementsResult empty() {
        return new BestDesignElementsResult();
    }

    public static class Item {
        private final AtlasGene gene;
        private final String deAccession;
        private final int deIndex;
        private final float pValue;
        private final float tValue;
        private final String ef;
        private final String efv;

        public Item(AtlasGene gene, String deAccession, int deIndex, float pValue, float tValue, String ef, String efv) {
            this.gene = gene;
            this.deAccession = deAccession;
            this.deIndex = deIndex;
            this.pValue = pValue;
            this.tValue = tValue;
            this.ef = ef;
            this.efv = efv;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public String getDeAccession() {
            return deAccession;
        }

        public int getDeIndex() {
            return deIndex;
        }

        public float getPValue() {
            return pValue;
        }

        public float getTValue() {
            return tValue;
        }

        public String getEf() {
            return ef;
        }

        public String getEfv() {
            return efv;
        }
    }
}
