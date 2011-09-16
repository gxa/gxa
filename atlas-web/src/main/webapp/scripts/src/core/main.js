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

    /** public **/

    A.contextPath = function(newContextPath) {
        if (arguments.length > 0) {
            contextPath = normalizePath(newContextPath);
        }
        return contextPath;
    };

    A.pathFor = function(path) {
        return "/" + contextPath + "/" + normalizePath(path);
    };

    A.logError = function(msg) {
        if (console) {
            console.log(msg);
        }
    };

    return A;
})(atlas || {}, jQuery);

/** after initialization stuff **/
(function(atlas) {
   atlas.contextPath(window.ATLAS_APPLICATION_CONTEXTPATH || "");
})(atlas);