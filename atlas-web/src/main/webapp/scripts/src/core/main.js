/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

var atlas = (function(A, $) {
    var contextPath = "";

    function normalizePath(path) {
        var normalizedPath = [];
        var arr = path.split("/");
        for(var i=0, len = arr.length; i<len; i++) {
            var str = $.trim(arr[i]);
            if (str.length > 0) {
                normalizedPath.push(str);
            }
        }
        return normalizedPath.join("/");
    }

    function _dump(obj, depth, indent) {
        var m = [];

        if (obj === Object(obj) && depth >= 0) {
            var isArray = $.isArray(obj);
            m.push(isArray ? "[" : "{");
            for (var p in obj) {
                if (obj.hasOwnProperty(p)) {
                    m.push(indent + p + ": " + _dump(obj[p], depth - 1, indent + "    "));
                }
            }
            m.push(indent + (isArray ? "]" : "}"));
        } else {
            return "" + obj;
        }
        return m.join("\n");
    }

    /** public **/

    /**
     * Gets/sets application context path, which is used e.g. in ajax requests.
     * @param newContextPath - a new context path to set
     */
    A.applicationContextPath = function(newContextPath) {
        if (newContextPath) {
            var np = normalizePath(newContextPath);
            contextPath = np ? "/" + np : np;
        }
        return contextPath;
    };

    /**
     * Extends the given URI with the application context.
     * @param uri - an URI to extend
     */
    A.fullPathFor = function(uri) {
        uri = uri || "";
        if (uri.length > 4 && uri.substring(0, 4) === "http") {
            return uri;
        }

        if (uri.length >= contextPath.length && uri.substring(0, contextPath.length) === contextPath) {
            return uri;
        }
        return contextPath + "/" + normalizePath(uri);
    };

    /**
     * Dumps error object/message to the js console.
     * @param e - an error object or string to write in the console
     * @param depth - a depth to dump error object (optional)
     */
    A.logError = function(e, depth) {
        if (console) {
            console.log("Error:\n" + _dump(e, depth || 3, ""));
        }
    };

    /**
     * Dumps object/message to the js console.
     * @param obj - an object or string to write in the console
     * @param depth - a depth to dump error object (optional)
     */
    A.logDebug = function(obj, depth) {
        if (console && A.debug) {
            console.log("Debug:\n" + _dump(obj, depth || 3, ""));
        }
    };

    return A;
})(atlas || {}, jQuery);

/** after initialization stuff **/
(function(A) {
   A.applicationContextPath(window.ATLAS_APPLICATION_CONTEXTPATH || "");
})(atlas);