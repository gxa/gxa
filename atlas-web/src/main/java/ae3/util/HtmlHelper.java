package ae3.util;

import ae3.service.structuredquery.UpdownCounter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

/**
 * Helper functions for parsing and managing structured query
 * @author pashky
 */
public class HtmlHelper {

    /**
     * Encode staring with URL encdoing (%xx's)
     * @param str url
     * @return encoded str
     */
    public static String escapeURL(String str)
    {
        try
        {
            return URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    /**
     * Quote string if it contains spaces
     * @param str url
     * @return quoted str
     */
    public static String optionalQuote(String str)
    {
        String escaped = str.replaceAll("\\\\","\\\\\\\\").replaceAll("\"", "\\\\\"");
        if(str.indexOf(' ') >= 0)
            return '"' + escaped + '"';
        return escaped;
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

    /**
     * Returns current system time, for util.tld
     * @return time in milliseconds
     */
    public static long currentTime()
    {
        return System.currentTimeMillis();
    }


    private static int coltrim(double v)
    {
        return Math.min(255, Math.max(0, (int)v));
    }

    public static String expressionBack(UpdownCounter ud, int updn) {
        if(ud.isZero())
            return "#ffffff";
        if(updn > 0) {
            int uc = coltrim(ud.getUps() != 0 ? (ud.getMpvUp() > 0.05 ? 0.05 : ud.getMpvUp()) * 255 / 0.05 : 255);            
            return String.format("#ff%02x%02x", uc, uc);
        } else {
            int dc = coltrim(ud.getDowns() != 0 ? (ud.getMpvDn() > 0.05 ? 0.05 : ud.getMpvDn()) * 255 / 0.05 : 255);
            return String.format("#%02x%02xff", dc, dc);
        }
    }

    public static String expressionText(UpdownCounter ud, int updn)
    {
        if(ud.isZero())
            return "#000000";
        
        double c;
        if(updn > 0) {
            c = ud.getUps() != 0 ? (ud.getMpvUp() > 0.05 ? 0.05 : ud.getMpvUp()) * 255 / 0.05 : 255;
        } else {
            c = ud.getDowns() != 0 ? (ud.getMpvDn() > 0.05 ? 0.05 : ud.getMpvDn()) * 255 / 0.05 : 255;
        }
        return c > 127 ? "#000000" : "#ffffff";
    }


    public static boolean isInSet(Set set, Object element)
    {
        return set.contains(element);
    }

    public static String truncateLine(String line, int num)
    {
        if(line.length() > num)
            return line.substring(0, num) + "...";
        else
            return line;
    }
}
