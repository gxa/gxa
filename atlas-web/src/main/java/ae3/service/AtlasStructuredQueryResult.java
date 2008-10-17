package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ae3.model.AtlasGene;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Log log = LogFactory.getLog(getClass());

    static public class UpdownCounter {
        private int ups;
        private int downs;
        private double mpvup;
        private double mpvdn;
        private String factor;
        private String factorValue;

        public UpdownCounter(int ups, int downs, double mpvup, double mpvdn, final String factor, final String factorValue) {
            this.ups = ups;
            this.downs = downs;
            this.mpvup = mpvup;
            this.mpvdn = mpvdn;
            this.factor = factor;
            this.factorValue = factorValue;
        }

        public int getUps() {
            return ups;
        }

        public int getDowns() {
            return downs;
        }

        public double getMpvUp() {
            return mpvup;
        }

        public double getMpvDn() {
            return mpvdn;
        }

        public String getFactor() {
            return factor;
        }

        public String getFactorValue() {
            return factorValue;
        }
    }


    static public class FacetUpDn implements Comparable<FacetUpDn> {
        private int up;
        private int down;

        public FacetUpDn() {
            up = down = 0;
        }

        public void setUp(int up) {
            this.up = up;
        }

        public void setDown(int down) {
            this.down = down;
        }

        public void addUp(int up) {
            this.up += up;
        }

        public void addDown(int down) {
            this.down += down;
        }

        public int getUp() {
            return up;
        }

        public int getDown() {
            return down;
        }

        public int compareTo(FacetUpDn o) {
            // descending order
            return - Integer.valueOf(getDown() + getUp()).compareTo(o.getUp() + o.getDown());
        }
    }

    static public class GeneResult {
        private AtlasGene gene;
        private List<UpdownCounter> updownCounters;

        public GeneResult(AtlasGene gene, List<UpdownCounter> updownCounters) {
            this.gene = gene;
            this.updownCounters = updownCounters;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public void setGene(AtlasGene gene) {
            this.gene = gene;
        }

        public List<UpdownCounter> getCounters() {
            return updownCounters;
        }

        public void setCounters(List<UpdownCounter> updownCounters) {
            this.updownCounters = updownCounters;
        }
    }

    private EfvTree<Boolean> queryEfvs;
    private List<GeneResult> results;

    private long total;
    private long start;
    private long rows;

    private EfvTree<FacetUpDn> efvFacet;

    public AtlasStructuredQueryResult(EfvTree<Boolean> queryEfvs, long start, long rows) {
        this.queryEfvs = queryEfvs;
        this.results = new ArrayList<GeneResult>();
        this.start = start;
        this.rows = rows;
    }

    public void addResult(GeneResult result) {
        results.add(result);
    }

    public int getSize() {
        return results.size();
    }

    public List<GeneResult> getResults() {
        return results;
    }

    public EfvTree<Boolean> getQueryEfvs() {
        return queryEfvs;
    }

    public long getStart() {
        return start;
    }

    public long getTotal() {
        return total;
    }

    public long getRows() {
        return rows;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setEfvFacet(EfvTree<FacetUpDn> efvFacet)
    {
        this.efvFacet = efvFacet;
    }

    public EfvTree<FacetUpDn> getEfvFacet()
    {
        return efvFacet;
    }
}
