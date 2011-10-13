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

(function(A, $) {
    /**
     * Ads the hash symbol at the beginning of the string if it is not there yet.
     *
     * @param s - a string with-or-without hash symbol
     */
    A.hsh = function(s) {
        return s ? (s.charAt(0) === "#" ? s : "#" + s) : s;
    };

    A.$ = function(s) {
        var el = $(A.hsh(s));
        return el.length ? el : null;
    };

    /**
     * Removes any content of the DOM element(s).
     * Accepts an Id (one or more) of DOM elements to clean up.
     */
    A.clearContent = function() {
        for (var i = 0, len = arguments.length; i < len; i++) {
            var elem = A.$(arguments[i]);
            if (elem) {
                elem.empty();
            }
        }
    };

    /**
     * Wraps a function into event handler. Any error happened during the function execution
     * is caught and logged in the console.
     *
     * @param func - a user defined function to wrap as event handler
     * @param context - a context of a user defined function execution (optional)
     */
    A.safeFunctionCall = function(func, context) {
        context = context || arguments.callee.caller;
        return function() {
            if (func && $.isFunction(func)) {
                try {
                    func.apply(context, arguments);
                } catch (e) {
                    A.logError(e);
                }
            }
            return false;
        }
    };

    /**
     *
     * @param opts {
     *     * url -
     *     * onSuccess -
     *     * onFailure - (optional, by default error message is logged in the js console)
     *     * defaultParams - (optional)
     *     * type - (optional, default is "json")
     * }
     */
    A.ajaxLoader = function(opts) {
        opts = opts || {};
        var url = opts.url;
        var type = opts.type || "json";
        var defaultParams = opts.defaultParams || {};

        var successHandler = A.safeFunctionCall(opts.onSuccess);
        var failureHandler = A.safeFunctionCall(
            opts.onFailure ||
                function(request, errorType, errorMessage) {
                    A.logError({
                        errorType:errorType,
                        errorMessage:errorMessage
                    });
                });

        return {
            load: function(params) {
                $.ajax({
                    url: A.fullPathFor(url),
                    data: $.extend(true, {}, defaultParams, params),
                    dataType: type,
                    success: successHandler,
                    error: failureHandler
                });
            }
        }
    };

    /**
     * JQuery plugin: Checks if element ("<a>") has attribute "atlas-uri"; and if yes it adds application
     * context path to its "href" attribute value.
     */
    $.fn.atlasRelativeHref = function() {
        var url = $(this).attr("atlas-uri");
        if (url) {
            $(this).attr("href", A.fullPathFor(url));
        }
    }
})(atlas || {}, jQuery);
