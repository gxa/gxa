package ae3.util;

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
}
