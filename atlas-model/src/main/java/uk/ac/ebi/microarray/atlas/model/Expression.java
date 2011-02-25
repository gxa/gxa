package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 2/25/11
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */

import com.sun.xml.bind.v2.schemagen.xmlschema.Any;

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
