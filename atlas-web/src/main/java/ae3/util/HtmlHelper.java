package ae3.util;

import ae3.service.structuredquery.UpdownCounter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

    public static String expressionBack(UpdownCounter ud)
    {
        if(ud.isZero())
            return "#ffffff";
        double uc = ud.getUps() != 0 ? (ud.getMpvUp() > 0.05 ? 0.05 : ud.getMpvUp()) * 255 / 0.05 : 255;
        double dc = ud.getDowns() != 0 ? (ud.getMpvDn() > 0.05 ? 0.05 : ud.getMpvDn()) * 255 / 0.05 : 255;
        double k = (double)ud.getUps() / (double)(ud.getUps() + ud.getDowns());
        return String.format("#%02x%02x%02x",
                coltrim(dc + (255.0 - dc) * k),
                coltrim(dc + (uc - dc) * k),
                coltrim(255.0 + (uc - 255.0) * k));
    }

    public static String expressionText(UpdownCounter ud)
    {
        if(ud.isZero())
            return "#000000";
        double uc = ud.getUps() != 0 ? (ud.getMpvUp() > 0.05 ? 0.05 : ud.getMpvUp()) * 255 / 0.05 : 255;
        double dc = ud.getDowns() != 0 ? (ud.getMpvDn() > 0.05 ? 0.05 : ud.getMpvDn()) * 255 / 0.05 : 255;
        double k = (double)ud.getUps() / (double)(ud.getUps() + ud.getDowns());
        return (dc + (uc - dc) * k) > 127 ? "#000000" : "#ffffff";
    }

}
