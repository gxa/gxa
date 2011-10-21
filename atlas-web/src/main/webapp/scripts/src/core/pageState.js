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

/**
 * Triggers 'destroyed' event when an element is removed via the jQuery modifiers;
 * widgets could use this event to unregister from the PageState as soon as their DOM element is destroyed.
 */
(function($) {
    var oldClean = $.cleanData;

    $.cleanData = function(elems) {
        for (var i = 0, elem; (elem = elems[i]) != null; i++) {
            $(elem).triggerHandler("destroyed");
        }
        oldClean(elems)
    }
})(jQuery);

(function(A, $) {
    /**
     * Page state; it stores the current state of a page. The state is automatically serialized into
     * url hash tag; so we could use back & forward browser's buttons to switch between page states made by
     * script.
     * Global page state is a hierarchical object (like a tree); so changing in the upper state can affect
     * the lower ones. This could be extremely useful for pagination components where changing the page event
     * forces all other components on this page to clean up (or to be removed). A tree-path prefix is used to
     * retrieve a hierarchical state. An empty prefix is considered as root and allows to change the top
     * most state.
     *
     * Note: JQuery Address plugin is used to listen url hash change event.
     * An url hash could be changed internally (by $.address.value('...')) and externally
     * (by clicking on back & forward browser buttons; or by changing url manually in a browser).
     */
    A.PageState = function() {
        var pageState = {},
            pageStatePrefixes = {},
            _this = {},
            pageStateAware = false,
            pageStateChanged = "pageStateChanged",
            widgetStateChanged = "stateChanged";

        /**
         * Serializes obj into param string; arrays of objects aren't supported.
         * @param obj - an object to serialize
         */
        function serialize(obj) {
            function _serialize(obj, prefix) {
                var s = [],
                    add = function(key, value) {
                        s.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
                    };

                if ($.isArray(obj)) {
                    for (var i = 0, len = obj.length; i < len; i++) {
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
                } else if (obj || obj === 0) {
                    add(prefix, obj);
                }
                return s;
            }

            var s = _serialize(obj, "");
            return s.join("&") || "%20";
            /**
             * Note: An empty string means changing URL hash to "#", which is the valid page top reference;
             * so the page will jump to the top every time the state is cleared. Returning "%20"
             * (or other meaningless string) instead of empty string prevents page from jumping.
             **/
        }

        /**
         * Deserializes param string into an object.
         * @param str - a param string to deserialize
         */
        function deserialize(str) {
            str = str.replace(/\+/g, "%20");
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

        function triggerStateChangedEvent() {
            if (!pageStateAware) {
                return;
            }
            for (var p in pageStatePrefixes) {
                if (pageStatePrefixes.hasOwnProperty(p)) {
                    $(_this).trigger(pageStateChanged + "_" + p,
                        [$.extend({}, A.objProperty(pageState, pageStatePrefixes[p]))]);
                }
            }
        }

        function updateUrlHash() {
            if (pageStateAware) {
                $.address.hash(serialize(pageState));
            }
        }

        function addPrefix(prefix) {
            var v = normalizePrefix(prefix);
            pageStatePrefixes[v] = prefix;
            return v;
        }

        function removePrefix(prefix) {
            var v = normalizePrefix(prefix);
            delete pageState[v];
            return v;
        }

        function normalizePrefix(prefix) {
            return prefix.replaceAll(/\./g, "$");
        }

        return $.extend(true, _this, {
            register: function(widget, prefix, handler) {
                A.logError("register state as " + prefix || "root");
                $(_this).bind(pageStateChanged + addPrefix(prefix), handler);
                $(widget).bind(widgetStateChanged, function(ev, newState) {
                    A.objProperty(pageState, prefix, newState);
                    updateUrlHash();
                });
            },

            unregister: function(widget, prefix, handler) {
                A.logError("unregister state " + prefix);
                $(_this).unbind(pageStateChanged + removePrefix(prefix), handler);
                $(widget).unbind(widgetStateChanged);
            },

            stateFor: function(prefix) {
                return prefix ? A.objProperty(pageState, prefix) : pageState;
            },

            init: function() {
                $.address.externalChange(function() {
                    pageStateAware = true;
                    pageState = deserialize($.address.hash());
                    triggerStateChangedEvent();
                    return false;
                });
            }
        });
    }();

})(atlas || {}, jQuery);

