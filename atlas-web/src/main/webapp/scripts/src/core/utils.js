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
        return typeof s === "string" ? (s.charAt(0) === "#" ? s : "#" + s) : s;
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
     * @param opts {
     *      url            - relative to context path url of a service
     *      type           - type of response data (optional, default is "json")
     *      onSuccess      - a function to handle loading success (optional)
     *      onFailure      - a function to handle loading failure (optional, by default error message is logged in the js console)
     *      defaultParams  - params to add to every ajax call (optional)
     * }
     */
    A.ajaxLoader = function(opts) {
        opts = opts || {};

        var url = opts.url,
            type = opts.type,
            defaultParams = opts.defaultParams || {},
            _this = {},
            context = opts.context || _this;

        function successHandler() {
            if (opts.onSuccess) {
                opts.onSuccess.apply(context, arguments);
            }
        }

        function failureHandler(request, errorType, errorMessage) {
            if (opts.onFailure) {
                opts.onFailure.apply(context, arguments);
            } else {
                A.logError({
                    errorType:errorType,
                    errorMessage:errorMessage
                });
            }
        }

        return $.extend(true, _this, {
            load:function (params, activityTarget) {
                params = $.extend(true, {}, defaultParams, params);
                A.ajaxReq({
                    url:url,
                    params:params,
                    dataType:type,
                    target:activityTarget,
                    success:successHandler,
                    error:failureHandler
                });
            }
        });
    };

    A.ajaxReq = function(opts) {
        function startActivity(el) {
            if (el) {
                el.append("<div><span class='loading'>&nbsp;</span></div>");
            }
        }

        function stopActivity(func, el) {
            if (!el) {
                return func;
            }
            return function() {
                el.children(":last").remove();
                func(arguments);
            }
        }

        function filter(params) {
            function empty(o) {
                return o === undefined || o === null || o.length == 0 ||
                    ($.isPlainObject(o) && $.isEmptyObject(o));

            }
            if ($.isPlainObject(params)) {
                for(var p in params) {
                    if (params.hasOwnProperty(p)) {
                        if (empty(params[p]) || empty(filter(params[p]))) {
                            delete params[p];
                        }
                    }
                }
            } else if ($.isArray(params)) {
                for (var len = params.length, i = len - 1; i >= 0; i--) {
                    if (empty(params[i]) || empty(filter(params[i]))) {
                        params.splice(i, 1);
                    }
                }
            }
            return params;
        }

        opts = opts || {};
        var activity = A.$(opts.target);
        startActivity(activity);
        $.ajax({
            url: A.fullPathFor(opts.url),
            data: filter(opts.params),
            dataType: opts.dataType || "json",
            success: stopActivity(opts.success, activity),
            error: stopActivity(opts.error, activity)
        });
    };

    /**
     * @param opts {
     *      {all ajaxLoader options}
     *      limit - the batch size (optional)
     *      onNextBatch - a function called for each batch
     *      onComplete - a function called after all data is loaded
     *      totalCount - field to read as total counter (optional)
     * }
     */
    A.ajaxBatchLoader = function (opts) {
        opts = opts || {};
        opts.onSuccess = onBatchSuccess;
        opts.onFailure = onBatchFailure;

        var loader = A.ajaxLoader(opts),
            limit = opts.limit || 200,
            offset = 0,
            _this = {},
            _params = {},
            _activityTarget = null;

        function loadBatch() {
            var p = $.extend(true, {offset:offset, limit:limit}, _params);
            loader.load(p, _activityTarget);
        }

        function onBatchSuccess(data) {
            if (opts.onNextBatch) {
                opts.onNextBatch(offset, data);
            }

            var total = getTotal(data);
            if (total > 0 && total - offset > limit) {
                offset += limit;
                loadBatch();
            } else if (opts.onComplete){
                opts.onComplete();
            }
        }

        function onBatchFailure() {
            //TODO ?
        }

        function getTotal(data) {
            return opts.totalCount ? A.objProperty(data, opts.totalCount) : data.total;
        }

        return $.extend(true, _this, {
            load:function(params, activityTarget) {
                _params = params;
                _activityTarget = activityTarget;
                loadBatch();
            }
        });
    };

    /**
     * JQuery plugin: Checks if element ("<a>") has attribute "atlas-uri"; and if yes it adds application
     * context path to its "href" attribute value.
     */
    $.fn.atlasRelativeHref = function() {
        $(this).each(function() {
            var url = $(this).attr("atlas-uri");
            if (url) {
                $(this).attr("href", A.fullPathFor(url));
            }
        });
    }
})(atlas || {}, jQuery);
