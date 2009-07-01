package ae3.util;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * @author pashky
 */
public class EscapeUtil {
    public static String escapeSolr(String s) {
        return "\"" + s.replaceAll("\\\\","\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"";
    }

    public static String escapeSolrValueList(Iterable<String> values) {
        StringBuffer sb = new StringBuffer();
        for (String v : values)
        {
            if(sb.length() > 0)
                sb.append(" ");
            sb.append(escapeSolr(v));
        }
        return sb.toString();
    }

    /**
     * Quote string if it contains spaces
     * @param str url
     * @return quoted str
     */
    public static String optionalQuote(String str)
    {
        if(str.indexOf(' ') >= 0)
            return '"' + str.replaceAll("\\\\","\\\\\\\\").replaceAll("\"", "\\\\\"") + '"';
        return str;
    }

    public static String joinQuotedValues(Iterable<String> values) {
        StringBuffer sb = new StringBuffer();
        for (String v : values)
        {
            if(sb.length() > 0)
                sb.append(" ");
            sb.append(optionalQuote(v));
        }
        return sb.toString();
    }

    public static List<String> parseQuotedList(final String value)
    {
        List<String> values = new ArrayList<String>();
        if(value.startsWith("(all "))
            return values;

        try {
            Reader r = new StringReader(value);
            StringBuilder curVal = new StringBuilder();
            boolean inQuotes = false;
            while(true) {
                int c = r.read();
                if(inQuotes)
                {
                    if(c < 0)
                        return values; // skip last incorrect condition

                    if(c == '\\') {
                        c = r.read();
                        if(c < 0)
                            return values; // skip last incorrect condition

                        curVal.appendCodePoint(c);
                    } else if(c == '"') {
                        inQuotes = false;
                    } else {
                        curVal.appendCodePoint(c);
                    }
                } else {
                    if(c < 0  || Character.isSpaceChar(c))
                    {
                        if(curVal.length() > 0) {
                            values.add(curVal.toString());
                            curVal.setLength(0);
                        }
                    } else if(c == '"') {
                        inQuotes = true;
                    } else {
                        curVal.appendCodePoint(c);
                    }

                    if(c < 0)
                        break;
                }
            }
        } catch(IOException e) {
            throw new RuntimeException("Unexpected exception!", e);
        }
        return values;
    }

    public static String escapeJSArray(Collection c) {
        StringBuilder sb = new StringBuilder();
        for(Object o : c) {
            if(sb.length() > 0)
                sb.append(',');
            sb.append('\'').append(StringEscapeUtils.escapeJavaScript(o.toString())).append('\'');
        }
        return sb.toString();
    }
}
