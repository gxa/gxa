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

package ae3.util;

import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.model.ExpressionStat;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Set;
import java.util.Collection;

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

    public static String expressionBack(ExpressionStat ud, int updn) {
        if(ud.getUpExperimentsCount() == 0 && ud.getDnExperimentsCount() == 0)
            return "#ffffff";
        if(updn > 0) {
            int uc = coltrim(ud.getUpExperimentsCount() != 0 ? (ud.getUpPvalue() > 0.05 ? 0.05 : ud.getUpPvalue()) * 255 / 0.05 : 255);
            return String.format("#ff%02x%02x", uc, uc);
        } else {
            int dc = coltrim(ud.getDnExperimentsCount() != 0 ? (ud.getDnPvalue() > 0.05 ? 0.05 : ud.getDnPvalue()) * 255 / 0.05 : 255);
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

    public static String expressionTextNew(ExpressionStat ud, int updn)
    {
        if(ud.getUpExperimentsCount() == 0 && ud.getDnExperimentsCount() == 0)
            return "#000000";

        double c;
        if(updn > 0) {
            c = ud.getUpExperimentsCount() != 0 ? (ud.getUpPvalue() > 0.05 ? 0.05 : ud.getUpPvalue()) * 255 / 0.05 : 255;
        } else {
            c = ud.getDnExperimentsCount() != 0 ? (ud.getDnPvalue() > 0.05 ? 0.05 : ud.getDnPvalue()) * 255 / 0.05 : 255;
        }
        return c > 127 ? "#000000" : "#ffffff";
    }


    public static boolean isIn(Collection set, Object element)
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

    public static Comparable maxProperty(Iterable it, String prop) {
        Method method = null;
        Comparable r = null;
        for(Object o : it) {
            if(method == null) {
                try {
                    method = o.getClass().getMethod(prop, (Class[])null);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Comparable v = (Comparable)method.invoke(o, (Object[])null);
                if(r == null || r.compareTo(v) < 0) {
                    r = v;
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return r;
    }

    public static String upcaseFirst(String s) {
        if(s.length() > 1)
            return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
        return s.toUpperCase();
    }
}

