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

package uk.ac.ebi.gxa.utils;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * String escaping utility functions
 *
 * @author pashky
 */
public class EscapeUtil {
    /**
     * Escape and quote string for use in SOLR queries
     *
     * @param s source string
     * @return escape value
     */
    public static String escapeSolr(String s) {
        return "\"" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"";
    }

    /**
     * Escape and concatenate list of string for use in SOLR queries
     *
     * @param values iterable of string values
     * @return processed string value
     */
    public static String escapeSolrValueList(Iterable<String> values) {
        StringBuffer sb = new StringBuffer();
        for (String v : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(escapeSolr(v));
        }
        return sb.toString();
    }

    /**
     * Quote string if it contains spaces
     *
     * @param str url
     * @return quoted str
     */
    public static String optionalQuote(String str) {
        if (str.indexOf(' ') >= 0)
            return '"' + str.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + '"';
        return str;
    }

    /**
     * Optionally quote and join with spaces an iterable set of string values
     *
     * @param values iterable strings
     * @return processed string value
     */
    public static String joinQuotedValues(Iterable<String> values) {
        StringBuffer sb = new StringBuffer();
        for (String v : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(optionalQuote(v));
        }
        return sb.toString();
    }

    /**
     * Opposite to joinQuotedValues(Iterable)
     *
     * @param value string value
     * @return list of parsed strings
     */
    public static List<String> parseQuotedList(final String value) {
        List<String> values = new ArrayList<String>();
        if (value.startsWith("(all "))
            return values;

        try {
            Reader r = new StringReader(value);
            StringBuilder curVal = new StringBuilder();
            boolean inQuotes = false;
            while (true) {
                int c = r.read();
                if (inQuotes) {
                    if (c < 0)
                        return values; // skip last incorrect condition

                    if (c == '\\') {
                        c = r.read();
                        if (c < 0)
                            return values; // skip last incorrect condition

                        curVal.appendCodePoint(c);
                    } else if (c == '"') {
                        inQuotes = false;
                    } else {
                        curVal.appendCodePoint(c);
                    }
                } else {
                    if (c < 0 || Character.isSpaceChar(c)) {
                        if (curVal.length() > 0) {
                            values.add(curVal.toString());
                            curVal.setLength(0);
                        }
                    } else if (c == '"') {
                        inQuotes = true;
                    } else {
                        curVal.appendCodePoint(c);
                    }

                    if (c < 0)
                        break;
                }
            }
        } catch (IOException e) {
            throw createUnexpected("Unexpected exception!", e);
        }
        return values;
    }

    /**
     * Represent a collection as JavaScript comma separated array of strings
     *
     * @param c collection of objects
     * @return string, the javascript array source
     */
    public static String escapeJSArray(Collection c) {
        StringBuilder sb = new StringBuilder();
        for (Object o : c) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append('\'').append(StringEscapeUtils.escapeJavaScript(o.toString())).append('\'');
        }
        return sb.toString();
    }

    /**
     * Encodes EF/EFV pair so it's safe to use it in SOLR field names
     *
     * @param ef  factor string
     * @param efv factor value string
     * @return encoded string
     */
    public static String encode(String ef, String efv) {
        return encode(ef) + "_" + encode(efv);
    }

    /**
     * Encodes string, so it's safe to use it in SOLR and XML field names
     *
     * @param v string to encoded
     * @return encoded string
     */
    public static String encode(String v) {
        try {
            StringBuffer r = new StringBuffer();
            for (char x : v.toCharArray()) {
                if (Character.isJavaIdentifierPart(x))
                    r.append(x);
                else
                    for (byte b : Character.toString(x).getBytes("UTF-8"))
                        r.append("_").append(String.format("%x", b));
            }
            return r.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable to encode EFV in UTF-8", e);
        }
    }

    /**
     * Returns integer value of number or 0 if it's null
     *
     * @param i number
     * @return integer or 0
     */
    public static int nullzero(Number i) {
        return i == null ? 0 : i.intValue();
    }

    /**
     * Returns float value of number or 0f if it's null
     *
     * @param f number
     * @return float or 0
     */
    public static float nullzerof(Number f) {
        return f == null ? 0.0f : f.floatValue();
    }

    /**
     * Function parses incoming parameter as a quoted space separated list of value if it's string or
     * just passes value through if it's list already
     *
     * @param values string or list
     * @return list of value
     */
    @SuppressWarnings("unchecked")
    public static List<String> optionalParseList(Object values) {

        final List<String> vlist;
        if (values instanceof String)
            vlist = EscapeUtil.parseQuotedList((String) values);
        else if (values instanceof List) {
            vlist = (List<String>) values;
        } else
            throw new ClassCastException("Unknown type of parameter - should be either String or List<String>, got " + values.getClass());
        return vlist;
    }

    /**
     * Parses string as integer number safely not throwing any exceptions, setting default value if string is invalid
     * and capping it with minimum and maximum values
     *
     * @param s   string to parse
     * @param def default value
     * @param min minimum value
     * @param max maximum value
     * @return processed integer
     */
    public static int parseNumber(String s, int def, int min, int max) {
        try {
            int r = Integer.valueOf(s);
            return Math.min(Math.max(r, min), max);
        } catch (Exception e) {
            return def;
        }
    }
}
