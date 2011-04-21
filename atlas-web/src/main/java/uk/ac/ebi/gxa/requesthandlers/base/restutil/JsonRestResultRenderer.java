/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * JSON format (http://www.json.org) REST result renderer.
 * This renderer is pretty simple in terms of mapping. All objects and maps are written as JS objects (in {})
 * and iterables are written as JS arrays (in []).
 * <p/>
 * {@link RestOut} properties used:
 * * name
 * * exposeEmpty
 * <p/>
 * Renderer allows to specify JSON/P callback name (see constructor)
 *
 * @author pashky
 */
public class JsonRestResultRenderer implements RestResultRenderer {

    private boolean indent = false;
    private int indentAmount = 4;
    private int currentIndent = 0;
    private Appendable where;
    private Class profile;
    private String callback;
    private ErrorWrapper errorWrapper;

    private static final char NL = '\n';
    private static final char LEFT_CURLY_BRACKET = '{';
    private static final char RIGHT_CURLY_BRACKET = '}';
    private static final char LEFT_ROUND_BRACKET = '(';
    private static final char RIGHT_ROUND_BRACKET = ')';
    private static final char LEFT_SQUARE_BRACKET = '[';
    private static final char RIGHT_SQUARE_BRACKET = ']';
    private static final char COMMA = ',';
    private static final char COLON = ':';
    private static final char SPACE = ' ';
    private static final char FWD_SLASH = '/';
    private static final char ESCAPE = '\\';
    private static final char DOUBLE_QUOTE = '"';
    private static final char LESS_THAN = '<';
    private static final char UNICODE_CTRL = '\u0080';
    private static final char NO_BREAK_SPACE = '\u00a0';
    private static final char UNICODE_EN_QUAD = '\u2000';
    private static final char UNICODE_ACCOUNT_OF = '\u2100';
    private static final char BACKSPACE = '\b';
    private static final char TAB = '\t';
    private static final char NEWLINE = '\n';
    private static final char FORMFEED = '\f';
    private static final char CR = '\r';
    private static final String ESC_BACKSPACE = ESCAPE + "b";
    private static final String ESC_TAB = ESCAPE + "t";
    private static final String ESC_NEWLINE = ESCAPE + "n";
    private static final String ESC_FORMFEED = ESCAPE + "f";
    private static final String ESC_CR = ESCAPE + "r";
    private static final String ESC_U = ESCAPE + "u";
    private static final String NULL = "null";
    private static final String HEX_ZEROS = "000";


    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor, allowing to set JSON/P callback name. The output will be wrapped in 'callback(...)' text to be used
     * in JS/HTML mash-ups
     *
     * @param indent       set to true if output should be pretty formatted and indented
     * @param indentAmount amount of each indentation step
     * @param callback     callback name
     */
    public JsonRestResultRenderer(boolean indent, int indentAmount, String callback) {
        this.indent = indent;
        this.indentAmount = indentAmount;
        this.callback = callback;
    }

    public void render(Object o, Appendable where, final Class profile) throws RestResultRenderException, IOException {
        this.where = where;
        this.profile = profile;
        if (callback != null) {
            where.append(callback).append(LEFT_ROUND_BRACKET);
        }
        try {
            process(o);
        } catch (IOException e) {
            throw e;
        } catch (RestResultRenderException e) {
            throw e;
        } catch (Throwable e) {
            log.error("Error rendering JSON", e);
            if (errorWrapper != null) {
                where.append(COMMA);
                process(errorWrapper.wrapError(e));
            } else
                throw new RestResultRenderException(e);
        } finally {
            if (callback != null) {
                where.append(RIGHT_ROUND_BRACKET);
            }
        }
    }

    public void setErrorWrapper(ErrorWrapper wrapper) {
        this.errorWrapper = wrapper;
    }

    private void process(Object o) throws IOException, RestResultRenderException {
        if (o == null)
            where.append(NULL);
        else
            process(o, null);
    }

    private void process(Object o, RestOut outProp) throws IOException, RestResultRenderException {
        if (o == null) {
            where.append(NULL);
            return;
        }
        outProp = RestResultRendererUtil.mergeAnno(outProp, o.getClass(), getClass(), profile);
        if (o instanceof Double) {
            Double d = (Double) o;
            where.append(toJSON(d));
        } else if (o instanceof Float) {
            Float f = (Float) o;
            where.append(toJSON(f.doubleValue()));
        } else if (o instanceof Number || o instanceof Boolean) {
            where.append(o.toString());
        } else if (o instanceof String || (outProp != null && outProp.asString()) || o instanceof Enum) {
            appendQuotedString(o.toString());
        } else if (o instanceof Iterable || o instanceof Iterator) {
            processIterable(o);
        } else if (o.getClass().isArray()) {
            processArray(o);
        } else {
            processMap(o);
        }
    }

    /**
     * JSON does not support NaN and Infinity
     *
     * @param v value to convert
     * @return value of v; null for NaN, null for Infinity (TODO)
     * @see <a href="http://bugs.jquery.com/ticket/6147">jQuery ticket 6147</a>
     */
    private CharSequence toJSON(double v) {
        if (Double.isInfinite(v)) {
            return NULL;
        }
        if (Double.isNaN(v)) {
            return NULL;
        }
        return Double.toString(v);
    }

    private void processMap(Object o) throws IOException, RestResultRenderException {
        if (o == null)
            return;

        where.append(LEFT_CURLY_BRACKET);
        if (indent) {
            currentIndent += indentAmount;
            where.append(NL);
        }

        try {
            boolean first = true;
            for (RestResultRendererUtil.Prop p : RestResultRendererUtil.iterableProperties(o, profile, this)) {
                if (first)
                    first = false;
                else {
                    where.append(COMMA);
                    if (indent)
                        where.append(NL);
                }
                appendIndent();
                appendOptQuotedString(p.name);

                if (indent)
                    where.append(SPACE);
                where.append(COLON);
                if (indent)
                    where.append(SPACE);

                process(p.value, p.outProp);
            }

            if (indent) {
                currentIndent -= indentAmount;
                where.append(NL);
            }
            appendIndent();
        } finally {
            where.append(RIGHT_CURLY_BRACKET);
        }
    }


    private void processIterable(Object oi) throws RestResultRenderException, IOException {
        where.append(LEFT_SQUARE_BRACKET);
        if (indent) {
            currentIndent += indentAmount;
            where.append(NL);
        }

        try {
            boolean first = true;
            Iterator i = oi instanceof Iterator ? (Iterator) oi : ((Iterable) oi).iterator();
            while (i.hasNext()) {
                Object object = i.next();
                if (first)
                    first = false;
                else {
                    where.append(COMMA);
                    if (indent)
                        where.append(NL);
                }
                appendIndent();
                if (object != null)
                    process(object, null);
                else
                    where.append(NULL);
            }

            if (indent) {
                currentIndent -= indentAmount;
                where.append(NL);
            }

        } finally {
            appendIndent();
            where.append(RIGHT_SQUARE_BRACKET);
        }
    }

    private void processArray(Object o) throws RestResultRenderException, IOException {
        final boolean primitive = o.getClass().getComponentType().isPrimitive();

        where.append(LEFT_SQUARE_BRACKET);
        if (indent && !primitive) {
            currentIndent += indentAmount;
            where.append(NL);
        }

        try {
            boolean first = true;
            for (int i = 0; i < Array.getLength(o); i++) {
                Object object = Array.get(o, i);
                if (first)
                    first = false;
                else {
                    where.append(COMMA);
                    if (indent)
                        where.append(primitive ? SPACE : NL);
                }
                if (!primitive) {
                    appendIndent();
                }
                if (object != null)
                    process(object, null);
                else
                    where.append(NULL);
            }

            if (indent && !primitive) {
                currentIndent -= indentAmount;
                where.append(NL);
            }

        } finally {
            if (!primitive)
                appendIndent();
            where.append(RIGHT_SQUARE_BRACKET);
        }
    }

    private void appendIndent() throws IOException {
        if (indent)
            for (int i = 0; i < currentIndent; ++i)
                where.append(SPACE);
    }

    private void appendOptQuotedString(String s) throws IOException {
        appendQuotedString(s);
    }

    private void appendQuotedString(String string) throws IOException {
        char b;
        char c = 0;
        int i;
        int len = string.length();
        String t;

        where.append(DOUBLE_QUOTE);
        try {
            for (i = 0; i < len; i += 1) {
                b = c;
                c = string.charAt(i);
                switch (c) {
                    case ESCAPE:
                    case DOUBLE_QUOTE:
                        where.append(ESCAPE);
                        where.append(c);
                        break;
                    case FWD_SLASH:
                        if (b == LESS_THAN) {
                            where.append(ESCAPE);
                        }
                        where.append(c);
                        break;
                    case BACKSPACE:
                        where.append(ESC_BACKSPACE);
                        break;
                    case TAB:
                        where.append(ESC_TAB);
                        break;
                    case NEWLINE:
                        where.append(ESC_NEWLINE);
                        break;
                    case FORMFEED:
                        where.append(ESC_FORMFEED);
                        break;
                    case CR:
                        where.append(ESC_CR);
                        break;
                    default:
                        if (c < SPACE || (c >= UNICODE_CTRL && c < NO_BREAK_SPACE) ||
                                (c >= UNICODE_EN_QUAD && c < UNICODE_ACCOUNT_OF)) {
                            t = HEX_ZEROS + Integer.toHexString(c);
                            where.append(ESC_U + t.substring(t.length() - 4));
                        } else {
                            where.append(c);
                        }
                }
            }
        } finally {
            where.append(DOUBLE_QUOTE);
        }
    }
}
