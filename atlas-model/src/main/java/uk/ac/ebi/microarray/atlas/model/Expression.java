package uk.ac.ebi.microarray.atlas.model;

/**
 * Expression of gene in condition - up, down or non-differentially expressed - moved from atlas-web by rpetry
 * @author pashky
 */
public enum Expression {
    UP {
        public boolean isUp() { return true; }
        public boolean isNo() { return false; }
    },
    DOWN {
        public boolean isUp() { return false; }
        public boolean isNo() { return false; }
    },
    NONDE {
        public boolean isUp() { return false; }
        public boolean isNo() { return true; }
    },
    ANY {
        public boolean isUp() { return true; }
        public boolean isNo() { return true; }

    };
    public abstract boolean isUp();
    public abstract boolean isNo();
}
