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

