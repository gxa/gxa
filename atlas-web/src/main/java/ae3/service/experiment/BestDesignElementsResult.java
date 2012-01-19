package ae3.service.experiment;

import ae3.model.AtlasGene;
import uk.ac.ebi.gxa.data.DesignElementStatistics;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class BestDesignElementsResult implements Iterable<BestDesignElementsResult.Item> {

    private String adAccession;
    private long total = 0;
    private final List<AtlasGene> genes = new ArrayList<AtlasGene>();
    private List<DesignElementStatistics> stats = newArrayList();

    BestDesignElementsResult() {
    }

    public String getArrayDesignAccession() {
        return adAccession;
    }

    void setArrayDesignAccession(String adAccession) {
        this.adAccession = adAccession;
    }

    void add(AtlasGene gene, DesignElementStatistics statistics) {
        this.stats.add(statistics);
        genes.add(gene);
    }

    void setTotalSize(long total) {
        this.total = total;
    }

    Item get(int i) {
        return new Item(genes.get(i), stats.get(i));
    }

    public long getTotalSize() {
        return total;
    }

    int getPageSize() {
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

    public static class Item {
        private final AtlasGene gene;
        private final DesignElementStatistics statistics;

        public Item(AtlasGene gene, DesignElementStatistics statistics) {
            this.gene = gene;
            this.statistics = statistics;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public String getDeAccession() {
            return statistics.getDeAccession();
        }

        @Deprecated
        public int getDeIndex() {
            return statistics.getDeIndex();
        }

        public float getPValue() {
            return statistics.getP();
        }

        public float getTValue() {
            return statistics.getT();
        }

        public String getEf() {
            return statistics.getEfv().getFirst();
        }

        public String getEfv() {
            return statistics.getEfv().getSecond();
        }
    }
}
