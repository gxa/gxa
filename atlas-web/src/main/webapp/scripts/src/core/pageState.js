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
     *
     * Note: JQuery Address plugin is used to listen url hash change event.
     * An url hash could be changed internally (by $.address.value('...')) and externally
     * (by clicking on back & forward browser buttons; or by changing url manually in a browser).
     */
    A.PageState = function() {
        var pageState = {},
            pageStateProps = {},
            _this = {},
            pageStateAware = false,
            pageStateChanged = "pageStateChanged",
            widgetStateChanged = "stateChanged";

        /**
         * Serializes obj into param string. Arrays of objects aren't supported.
         * @param obj - an object to serialize
         */
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
             * (could be any none existed hash reference) instead of empty string prevents page from jumping.
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
            if (pageStateAware) {
                for(var p in pageStateProps) {
                    if (pageStateProps.hasOwnProperty(p)) {
                        $(_this).trigger(pageStateChanged + "_" + p, pageState[p]);
                    }
                }
            }
        }

        function updateUrlHash() {
            for (var p in pageState) {
                if (pageState.hasOwnProperty(p) && !pageStateProps[p]) {
                    pageState[p] = null;
                }
            }
            if (pageStateAware) {
                $.address.hash(serialize(pageState));
            }
        }

        return $.extend(true, _this, {
            register: function(widget, uniqProp, handler) {
                A.logError("register: " + uniqProp);
                $(_this).bind(pageStateChanged + "_" + uniqProp, handler);
                $(widget).bind(widgetStateChanged, function(ev, newState) {
                    pageState[uniqProp] = newState;
                    updateUrlHash();
                });
                pageStateProps[uniqProp] = 1;
            },

            unregister: function(widget, uniqProp, handler) {
                A.logError("unregister: " + uniqProp);
                $(_this).unbind(pageStateChanged + "_" + uniqProp, handler);
                $(widget).unbind(widgetStateChanged);
                delete pageStateProps[uniqProp];
            },

            stateFor: function(uniqProp) {
                if (uniqProp) {
                    return pageState[uniqProp];
                }
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

