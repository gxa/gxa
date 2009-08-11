package ae3.restresult;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author pashky
 */
public class JsonRestResultRenderer implements RestResultRenderer {

    private boolean indent = false;
    private int indentAmount = 4;
    private int currentIndent = 0;
    private Appendable where;
    private final static char NL = '\n';
    private Class profile;

    public JsonRestResultRenderer(boolean indent, int indentAmount) {
        this.indent = indent;
        this.indentAmount = indentAmount;
    }

    public void render(Object o, Appendable where, final Class profile) throws RenderException, IOException {
        this.where = where;
        this.profile = profile;
        process(o);
    }

    private void process(Object o) throws IOException, RenderException {
        if(o instanceof Number || o instanceof Boolean) {
            where.append(o.toString());
        } else if(o instanceof String || o.getClass().isAnnotationPresent(AsString.class) || o instanceof Enum) {
            appendQuotedString(o.toString());
        } else if(o instanceof Iterable || o instanceof Iterator) {
            processArray(o);
        } else {
            processMap(o);
        }
    }
    
    private void processMap(Object o) throws IOException, RenderException {
        if(o == null)
            return;

        where.append('{');
        if(indent) {
            currentIndent += indentAmount;
            where.append(NL);
        }
        
        boolean first = true;
        for(Util.Prop p : Util.iterableProperties(o, profile)) {
            if(p.value == null)
                continue;

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

            if(p.method != null && p.method.isAnnotationPresent(AsString.class)) {
                appendQuotedString(p.value.toString());
            } else {
                process(p.value);
            }
        }

        if(indent) {
            currentIndent -= indentAmount;
            where.append(NL);
        }
        appendIndent();
        where.append('}');

    }


    private void processArray(Object oi) throws RenderException, IOException {
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
                process(object);
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
