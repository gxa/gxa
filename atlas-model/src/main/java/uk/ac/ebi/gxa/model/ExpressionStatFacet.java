package uk.ac.ebi.gxa.model;

/**
 * @author pashky
 */
public class ExpressionStatFacet extends FacetQueryResultSet.FacetField<String,ExpressionStatFacet.UpDown> {
    public static class UpDown implements Comparable<UpDown> {
        private int up;
        private int down;

        public UpDown() {
            up = down = 0;
        }

        public void add(int v, boolean doUp)
        {
            if(doUp)
                up += v;
            else
                down += v;
        }

        public int getUp() {
            return up;
        }

        public int getDown() {
            return down;
        }

        public int compareTo(UpDown o) {
            // descending order
            return - Integer.valueOf(getDown() + getUp()).compareTo(o.getUp() + o.getDown());
        }
    }

    private String factor;

    public ExpressionStatFacet(String factor) {
        this.factor = factor;
    }

    public String getFactor() {
        return factor;
    }

    public UpDown createValue() {
        return new UpDown();
    }
}
