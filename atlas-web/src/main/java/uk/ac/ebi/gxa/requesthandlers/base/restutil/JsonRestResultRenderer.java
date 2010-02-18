package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import java.io.IOException;
import java.util.Iterator;

/**
 * JSON format (http://www.json.org) REST result renderer.
 * This renderer is pretty simple in terms of mapping. All objects and maps are written as JS objects (in {})
 * and iterables are written as JS arrays (in []).
 *
 * {@link RestOut} properties used:
 * * name
 * * exposeEmpty
 *
 * Renderer allows to specify JSON/P callback name (see constructor)
 * 
 * @author pashky
 */
public class JsonRestResultRenderer implements RestResultRenderer {

    private boolean indent = false;
    private int indentAmount = 4;
    private int currentIndent = 0;
    private Appendable where;
    private final static char NL = '\n';
    private Class profile;
    private String callback;

    /**
     * Constructor
     * @param indent set to true if output should be pretty formatted and indented
     * @param indentAmount amount of each indentation step
     */
    public JsonRestResultRenderer(boolean indent, int indentAmount) {
        this(indent, indentAmount, null);
    }

    /**
     * Constructor, allowing to set JSON/P callback name. The output will be wrapped in 'callback(...)' text to be used
     * in JS/HTML mash-ups
     * @param indent set to true if output should be pretty formatted and indented
     * @param indentAmount amount of each indentation step
     * @param callback callback name
     */
    public JsonRestResultRenderer(boolean indent, int indentAmount, String callback) {
        this.indent = indent;
        this.indentAmount = indentAmount;
        this.callback = callback;
    }

    public void render(Object o, Appendable where, final Class profile) throws RestResultRenderException, IOException {
        this.where = where;
        this.profile = profile;
        if(callback != null) {
            where.append(callback).append('(');
        }
        process(o, null);
        if(callback != null) {
            where.append(')');
        }
    }

    private void process(Object o, RestOut outProp) throws IOException, RestResultRenderException {
        outProp = RestResultRendererUtil.mergeAnno(outProp, o.getClass(), getClass(), profile);

        if(o instanceof Number || o instanceof Boolean) {
            where.append(o.toString());
        } else if(o instanceof String || (outProp != null && outProp.asString()) || o instanceof Enum) {
            appendQuotedString(o.toString());
        } else if(o instanceof Iterable || o instanceof Iterator) {
            processArray(o);
        } else {
            processMap(o);
        }
    }
    
    private void processMap(Object o) throws IOException, RestResultRenderException {
        if(o == null)
            return;

        where.append('{');
        if(indent) {
            currentIndent += indentAmount;
            where.append(NL);
        }
        
        boolean first = true;
        for(RestResultRendererUtil.Prop p : RestResultRendererUtil.iterableProperties(o, profile, this)) {
            if(first)
                first = false;
            else {
                where.append(',');
                if(indent)
                    where.append(NL);
            }
            appendIndent();
            appendOptQuotedString(p.name);

            if(indent)
                where.append(' ');
            where.append(':');
            if(indent)
                where.append(' ');

            process(p.value, p.outProp);
        }

        if(indent) {
            currentIndent -= indentAmount;
            where.append(NL);
        }
        appendIndent();
        where.append('}');

    }


    private void processArray(Object oi) throws RestResultRenderException, IOException {
        where.append('[');
        if(indent) {
            currentIndent += indentAmount;
            where.append(NL);
        }

        boolean first = true;
        Iterator i = oi instanceof Iterator ? (Iterator)oi : ((Iterable)oi).iterator();
        while(i.hasNext()) {
            Object object = i.next();
            if(first)
                first = false;
            else {
                where.append(',');
                if(indent)
                    where.append(NL);
            }
            appendIndent();
            if(object != null)
                process(object, null);
            else
                where.append("null");
        }

        if(indent) {
            currentIndent -= indentAmount;
            where.append(NL);
        }

        appendIndent();
        where.append(']');

    }

    private void appendIndent() throws IOException {
        if(indent)
            for(int i = 0; i < currentIndent; ++i)
                where.append(' ');
    }

    private void appendOptQuotedString(String s) throws IOException {
        appendQuotedString(s);
    }

    private void appendQuotedString(String string) throws IOException {
        char         b;
        char         c = 0;
        int          i;
        int          len = string.length();
        String       t;

        where.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                where.append('\\');
                where.append(c);
                break;
            case '/':
                if (b == '<') {
                    where.append('\\');
                }
                where.append(c);
                break;
            case '\b':
                where.append("\\b");
                break;
            case '\t':
                where.append("\\t");
                break;
            case '\n':
                where.append("\\n");
                break;
            case '\f':
                where.append("\\f");
                break;
            case '\r':
                where.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                               (c >= '\u2000' && c < '\u2100')) {
                    t = "000" + Integer.toHexString(c);
                    where.append("\\u" + t.substring(t.length() - 4));
                } else {
                    where.append(c);
                }
            }
        }
        where.append('"');
    }
}
