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

    /**
     * Removes any content of the DOM element(s).
     * Accepts an Id (one or more) of DOM elements to clean up.
     */
    A.clearContent = function() {
        for (var i = 0, len = arguments.length; i < len; i++) {
            var elem = $(A.hsh(arguments[i]));
            if (elem.length) {
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

    A.objProperty = function(obj, propName, value) { // dot separated object property path
        obj = obj || {};
        var setter = arguments.length > 2;
        var parts = propName.split(".");
        var s = obj;
        for (var i = 0, len = parts.length; i < len; i++) {
            var p = parts[i];
            if (!s.hasOwnProperty(p)) {
                if (! setter) {
                    return undefined;
                }
                s[p] = {};
            }
            if (i == len - 1 && setter) {
                s[p] = value;
            }
            s = s[p];
        }
        return s;
    };

    /**
     * Page state; it stores the current state of a page. The state is automatically serialized into
     * url hash tag; so we could use back & forward browser's buttons to switch between page states made by
     * script.
     * The state is an object tree, any part of it is allowed to change; the only restriction here is
     * using arrays of objects.
     * Note: JQuery Address plugin is used to listen url hash change event.
     * An url hash could be changed internally (by $.address.value('...')) and externally
     * (by clicking on back & forward browser buttons; or by changing url manually in a browser).
     */
    A.PageState = function() {
        var state = {};
        var _this = {};

        function serialize(obj) {
            function _serialize(obj, prefix) {
                var s = [],
                    add = function(key, value) {
                        s.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
                    };

                if ($.isArray(obj)) {
                    for(var i=0, len = obj.length; i<len; i++) {
                       var item = obj[i];
                       if ($.isArray(item) || $.isPlainObject(item)) {
                           A.logError("PageState doesn't support arrays of objects");
                       } else {
                          add(prefix, item);
                       }
                    }
                } else if ($.isPlainObject(obj)) {
                    for (var p in obj) {
                        if (obj.hasOwnProperty(p)) {
                            s = s.concat(_serialize(obj[p], prefix ? prefix + "." + p : p));
                        }
                    }
                } else {
                    add(prefix, obj);
                }
                return s;
            }

            var s = _serialize(obj, "");
            return s.join("&").replace(/%20/g, "+");
        }

        function deserialize(str) {
            var _values = {"false":false, "true":true, "null":null, "undefined":undefined};
            var obj = {};
            var parts = str.split("&");
            for (var i = 0, len = parts.length; i < len; i++) {
                var pair = parts[i].split("=");
                if (pair.length < 2) {
                    continue;
                }
                var name = decodeURIComponent(pair[0]);
                var value = decodeURIComponent(pair[1]);

                value = value && !isNaN(value) ? +value
                    : _values.hasOwnProperty(value) ? _values[value]
                    : value;

                var v = A.objProperty(obj, name);
                if (v != undefined) {
                    (v = $.isArray(v) ? v : [v]).push(value);
                    A.objProperty(obj, name, v);
                } else {
                    A.objProperty(obj, name, value);
                }
            }
            return obj;
        }

        function updateHashTag() {
            $.address.value(A.Params.serialize(state));
            triggerStateChangedEvent();
        }

        function triggerStateChangedEvent() {
            $(_this).trigger("stateChanged");
        }
        return $.extend(true, _this, {
            update: function(newState, path) {
                if (path) {
                    A.objProperty(state, path, newState);
                } else {
                    state = $.extend(true, {}, newState);
                }
                updateHashTag();
            },

            init: function() {
                $.address.externalChange(function() {

                });
            }
        });
    }();

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
