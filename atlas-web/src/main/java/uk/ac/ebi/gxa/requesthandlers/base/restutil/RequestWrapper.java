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

import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;


/**
 * Request wrapper utility class alowing to safely fetch HTTP parameters with type conversion and defaults
 * @author pashky
 */
public class RequestWrapper {
    private HttpServletRequest request;

    /**
     * Constructor wraps HTTP Servlet Request
     * @param request HTTP Servlet Request object
     */
    public RequestWrapper(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns safely parsed integer value of parameter (unbound, default is 0)
     * @param name parameter name
     * @return integer value
     */
    public int getInt(String name) {
        return getInt(name, 0);
    }

    /**
     * Returns safely parsed integer value of parameter (unbound, custom default)
     * @param name parameter name
     * @param def default value for invalid strings
     * @return integer value
     */
    public int getInt(String name, int def) {
        try {
            return Integer.valueOf(request.getParameter(name));
        } catch(Exception e) {
            return def;
        }
    }

    /**
     * Returns safely parsed integer value of parameter (custom default and minimum values)
     * @param name parameter name
     * @param def default value for invalid strings
     * @param min minimum value (inclusive, used to trim valid numbers but lower than this)
     * @return integer value
     */
    public int getInt(String name, int def, int min) {
        return Math.max(getInt(name, def), min);
    }

    /**
     * Returns safely parsed integer value of parameter (custom default and minimum values)
     * @param name parameter name
     * @param def default value for invalid strings
     * @param min minimum value (inclusive, used to trim valid numbers but lower than this)
     * @param max maximum value (inclusive, used to trim valid numbers but higher than this)
     * @return integer value
     */
    public int getInt(String name, int def, int min, int max) {
        return Math.max(Math.min(getInt(name, def), max), min);
    }

    /**
     * Returns array of string values (maybe empty, but never null)
     * @param name parameter name
     * @return array of strings
     */
    public String[] getStrArray(String name) {
        String[] v = request.getParameterValues(name);
        return v == null ? new String[0] : v;
    }

    /**
     * Returns string value of parameter (maybe empty but never null)
     * @param name parameter name
     * @return string value
     */
    public String getStr(String name) {
        String v = request.getParameter(name);
        return v == null ? "" : v;
    }

    /**
     * Parse boolean value of parameter. "1", "true" and "yes" (in any case) are counted as true,
     * everything else is false.
     * @param name parameter name
     * @return true or false
     */
    public boolean getBool(String name) {
        String v = request.getParameter(name);
        return v != null && ("1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v));
    }

    /**
     * Returns parameters map String -> String[]
     * @return parameters map
     */
    @SuppressWarnings("unchecked")
    public Map<String,String[]> getMap() {
        return request.getParameterMap();
    }

    /**
     * Parse enum value
     * @param name parameter name
     * @param def default value for invalid strings (also defines Enum type to use for parsing)
     * @param <T> enum type
     * @return parsed enum value
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String name, T def) {
        try {
            return Enum.valueOf((Class<T>)def.getClass(), request.getParameter(name));
        } catch(Exception e) {
            return def; 
        }
    }

    /**
     * Parse enum value or return null if string is invalid
     * @param name parameter name
     * @param clazz enum class
     * @param <T> enum type
     * @return enum value or null
     */
    public <T extends Enum<T>> T getEnumNullDefault(String name, Class<T> clazz) {
        try {
            return Enum.valueOf(clazz, request.getParameter(name));
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Returns remote host name or address
     * @return string
     */
    public String getRemoteHost() {
        String remoteId = request.getRemoteHost();
        if(Strings.isNullOrEmpty(remoteId))
            remoteId = request.getRemoteAddr();
        if(Strings.isNullOrEmpty(remoteId))
            remoteId = "unknown";
        return remoteId;
    }

    /**
     * Returns or creates HTTP session
     * @param create true if should create session
     * @return http session object
     */
    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }
    
}
