package ae3.service.structuredquery;

import java.util.Arrays;

/**
 * Case insensitive prefix search tree implementation
 * @author pashky
 */
class PrefixNode {
    private char[] chars = null;
    private PrefixNode[] children = null;
    private int count = -1;

    public interface WalkResult {
        void put(String name, int count);
        boolean enough();
    }

    public void add(String name, int count) {
        PrefixNode node = this;
        for(char c : name.toCharArray()) {
            if(node.chars == null) {
                node.chars = new char[1];
                node.children = new PrefixNode[1];
                node.children[0] = new PrefixNode();
                node.chars[0] = c;
                node = node.children[0];
            } else {
                int i = Arrays.binarySearch(node.chars, c);
                if(i < 0) {
                    i = -i - 1;
                    char[] nc = new char[node.chars.length + 1];
                    System.arraycopy(node.chars, 0, nc, 0, i);
                    System.arraycopy(node.chars, i, nc, i + 1, node.chars.length - i);
                    nc[i] = c;
                    node.chars = nc;
                    PrefixNode[] np = new PrefixNode[node.children.length + 1];
                    System.arraycopy(node.children, 0, np, 0, i);
                    System.arraycopy(node.children, i, np, i + 1, node.children.length - i);
                    np[i] = new PrefixNode();
                    node.children = np;
                }
                node = node.children[i];
            }
        }

        node.count = count;
    }


    public void collect(String sofar, WalkResult result) {
        if(result.enough())
            return;
        if(this.count >= 0) {
            result.put(sofar, this.count);
        }
        if(this.chars != null)
            for(int i = 0; i < this.chars.length; ++i)
                this.children[i].collect(sofar + this.chars[i], result);
    }

    public void walk(String text, int pos, String sofar, WalkResult result) {
        if(result.enough())
            return;

        if(pos >= text.length()) {
            this.collect(sofar, result);
        } else if(this.chars != null) {
            char c = Character.toUpperCase(text.charAt(pos));
            int i = Arrays.binarySearch(this.chars, c);
            if(i >= 0)
                this.children[i].walk(text, pos + 1, sofar + c, result);

            char lc = Character.toLowerCase(c);
            if(lc != c) {
                i = Arrays.binarySearch(this.chars, lc);
                if(i >= 0)
                    this.children[i].walk(text, pos + 1, sofar + lc, result);
            }
        }
    }
}

