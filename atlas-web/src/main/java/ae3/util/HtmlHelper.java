package ae3.util;

import ae3.service.structuredquery.AtlasStructuredQuery;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

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
     * Returns array of gradually spread colors from rgb1 to rgb2,
     * the gradient is proportionally split according to up:down ratio
     * @param r1 red component of color 1
     * @param g1 green component of color 1
     * @param b1 blue component of color 1
     * @param r2 red component of color 2
     * @param g2 green component of color 2
     * @param b2 blue component of color 2
     * @param n number of steps
     * @param up number of ups
     * @param down number of downs
     * @return array of hex html colors
     */
    public static String[] gradient(int r1, int g1, int b1, int r2, int g2, int b2, int n, int up, int down)
    {
        String[] result = new String[n];

        double x = 0;
        if(up + down > 0)
            x = (double)up / (double)(up+down);

        int k = 3 + (int)(x * (double)(n - 7));
        for(int i = 0; i < n; ++i)
        {
            int r, g, b;
            if(i < k)
            {
                r = r1 + (r2-r1) * i / 2 / k;
                g = g1 + (g2-g1) * i / 2 / k;
                b = b1 + (b2-b1) * i / 2 / k;
            } else {
                r = (r1 + r2 + (r2-r1) * (i - k) / (n - 1 - k)) / 2;
                g = (g1 + g2 + (g2-g1) * (i - k) / (n - 1 - k)) / 2;
                b = (b1 + b2 + (b2-b1) * (i - k) / (n - 1 - k)) / 2;
            }
            result[i] = String.format("#%02x%02x%02x", r, g, b);
        }
        return result;
    }

    /**
     * Returns current system time, for util.tld
     * @return time in milliseconds
     */
    public static long currentTime()
    {
        return System.currentTimeMillis();
    }
}
