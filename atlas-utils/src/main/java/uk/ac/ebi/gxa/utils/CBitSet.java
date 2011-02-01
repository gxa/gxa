package uk.ac.ebi.gxa.utils;

import java.util.BitSet;

/**
 * {@link Comparable} {@link BitSet}
 */
public class CBitSet extends BitSet implements Comparable<CBitSet> {
    public CBitSet(int nbits) {
        super(nbits);
    }

    public int compareTo(CBitSet o) {
        for (int i = 0; i < Math.max(size(), o.size()); ++i) {
            boolean b1 = get(i);
            boolean b2 = o.get(i);
            if (b1 != b2)
                return b1 ? 1 : -1;
        }
        return 0;
    }
}
