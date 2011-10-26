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
     * Object property path getter/setter.
     * If the value (the third argument) is given then it behaves as setter: sets value according the given property
     * path and returns the updated object; otherwise it is getter and returns the corresponding object property value
     * or 'undefined' if the property path doesn't exist.
     *
     * An empty property path does nothing in setter mode.
     *
     * @param obj - an object to update or get properties from
     * @param propPath - dot separated object property path
     * @param value - a new value to set for the given property path
     */
    A.objProperty = function(obj, propPath, value) {
        obj = obj || {};
        var setter = arguments.length > 2;
        var parts = propPath.split(".");
        var s = obj;
        for (var i = 0, len = parts.length; i < len - 1; i++) {
            var prop = parts[i];
            if (!s.hasOwnProperty(prop)) {
                if (!setter) {
                    return undefined;
                }
                s[prop] = {};
            }

            s = s[prop];
        }

        var lastProp = parts[parts.length - 1];

        if (setter && lastProp != "") {
            var copy;
            if ($.isPlainObject(value)) {
                copy = $.extend(true, {}, value);
            } else if ($.isArray(value)) {
                copy = $.extend(true, [], value);
            } else {
                copy = value;
            }
            s[lastProp] = copy;
        }

        return lastProp === "" ? s : s[lastProp];
    };

    A.extendIfUndefined = function(obj, defaults) {
        function _extend(obj1, obj2) {
            if ($.isArray(obj2)) {
                if (!obj1) {
                    obj1 = [];
                    for (var i = 0, len = v.length; i < len; i++) {
                        obj1[i] = _extend(obj1[i], obj2[i]);
                    }
                }
            } else if ($.isPlainObject(obj2)) {
                for (var p in obj2) {
                    if (obj2.hasOwnProperty(p)) {
                        obj1[p] = _extend(obj1[p], obj2[p]);
                    }
                }
            } else if (obj1 === undefined || obj1 === null) {
                obj1 = obj2;
            }
            return obj1;
        }

        return _extend(obj, defaults);
    };

    A.difference = function(obj, defaults) {
        function _difference(obj1, obj2) {
            if ($.isArray(obj2)) {
                if (obj1 && $.isArray(obj1) && obj1.length == obj2.length) {
                    for (var len = v.length, i = len - 1; i > 0; i--) {
                        if (_difference(obj1[i], obj2[i])) {
                            return obj1;
                        }
                    }
                    return null;
                }
            } else if ($.isPlainObject(obj2)) {
                for (var p in obj2) {
                    if (obj2.hasOwnProperty(p)) {
                        obj1[p] = _difference(obj1[p], obj2[p]);
                        if (obj1[p] == null) {
                            delete obj1[p];
                        }
                    }
                }
                return obj1;
            }
            return (obj1 === obj2) ? null : obj1;
        }

        return _difference(obj, defaults);
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
        var _this = {};

        function successHandler() {
            var context = opts.context || _this;
            if (opts.onSuccess) {
                opts.onSuccess.apply(context, arguments);
            }
        }

        function failureHandler(request, errorType, errorMessage) {
            var context = opts.context || _this;
            if (opts.onFailure()) {
                opts.onFailure.apply(context, arguments);
            } else {
                A.logError({
                    errorType:errorType,
                    errorMessage:errorMessage
                });
            }
        }

        return $.extend(true, _this, {
            load: function(params) {
                $.ajax({
                    url: A.fullPathFor(url),
                    data: $.extend(true, {}, defaultParams, params),
                    dataType: type,
                    success: function(p) {
                        return function() {
                            successHandler.apply(_this, arguments);
                        }
                    }(params),
                    error: failureHandler
                });
            }
        });
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
