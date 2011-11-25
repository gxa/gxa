package uk.ac.ebi.gxa.data;

import java.util.*;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class BestDesignElementsResult implements Iterable<BestDesignElementsResult.Item> {

    private String adAccession;
    private long total = 0;
    private final List<Long> geneIds = new ArrayList<Long>();
    private final List<Integer> deIndices = new ArrayList<Integer>();
    private final List<String> deAccessions = new ArrayList<String>();
    private final List<Double> pvalues = new ArrayList<Double>();
    private final List<Double> tvalues = new ArrayList<Double>();
    private final List<String> efs = new ArrayList<String>();
    private final List<String> efvs = new ArrayList<String>();

    BestDesignElementsResult() {
    }

    public String getArrayDesignAccession() {
        return adAccession;
    }

    void setArrayDesignAccession(String adAccession) {
        this.adAccession = adAccession;
    }

    void add(Long geneId, int deIndex, String deAccession, double pval, double tstat, String ef, String efv) {
        geneIds.add(geneId);
        deIndices.add(deIndex);
        deAccessions.add(deAccession);
        pvalues.add(pval);
        tvalues.add(tstat);
        efs.add(ef);
        efvs.add(efv);
    }

    void setTotalSize(long total) {
        this.total = total;
    }

    public Item get(int i) {
        return new Item(geneIds.get(i),
                deIndices.get(i),
                deAccessions.get(i),
                pvalues.get(i).floatValue(),
                tvalues.get(i).floatValue(),
                efs.get(i),
                efvs.get(i));
    }

    public long getTotalSize() {
        return total;
    }

    public int getPageSize() {
        return geneIds.size();
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

    public List<Long> getGeneIds() {
        return Collections.unmodifiableList(geneIds);
    }

    public static class Item {
        private final Long geneId;
        private final int deIndex;
        private final String deAccession;
        private final float pValue;
        private final float tValue;
        private final String ef;
        private final String efv;

        Item(Long geneId, int deIndex, String deAccession, float pValue, float tValue, String ef, String efv) {
            this.geneId = geneId;
            this.deIndex = deIndex;
            this.deAccession = deAccession;
            this.pValue = pValue;
            this.tValue = tValue;
            this.ef = ef;
            this.efv = efv;
        }

        public Long getGeneId() {
            return geneId;
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

