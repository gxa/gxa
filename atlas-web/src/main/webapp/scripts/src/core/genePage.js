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
     * @param opts {
     *     gene            - gene Id to filter the list of experiments by
     *     ef              - a default ef to list experiments for (optional)
     *     pageSize        - number of experiments per page (optional)
     *     pageState       - page state object to register for stateChange event (optional)
     *     pageStatePrefix - a hierarchical prefix for page state; required if pageState is given
     * }
     */
    A.genePage = function(opts) {
        opts = $.extend({
            gene: null,
            ef: null,
            pageSize: 5,
            pageState: null,
            pageStatePrefix: ""
        }, opts);

        var _this = {},
            defaults = {ef: opts.ef};

        var expList = atlas.geneExperimentList({
            gene: opts.gene,
            pageSize: opts.pageSize,
            pageState: opts.pageState,
            pageStatePrefix: (opts.pageStatePrefix ? opts.pageStatePrefix + "." : "") + "list",
            listTarget: "experimentList",
            listTemplate: "experimentListTemplate",
            pageTarget: "experimentListPage",
            pageTemplate: "experimentListPageTemplate"
        });

        if (opts.pageState) {
            opts.pageState.register(_this, opts.pageStatePrefix);
        }

        $("tr[efefv]").each(function(){
            var efefv = $(this).attr("efefv");
            $(this).attr("id", asElementId(efefv));
        });

        $("#allStudiesLink").live("click", function(ev) {
            var copy = $.extend(true, {}, defaults);
            externalChange(copy);
            return false;
        });

        function initialize() {
            loadExperiments(
                opts.pageState ? opts.pageState.stateFor(opts.pageStatePrefix) : {}
            );
        }

        function externalChange(params) {
            triggerStateChanged(params);
            loadExperiments(params);
        }

        function loadExperiments(state) {
            state = A.extendIfUndefined(state || {}, defaults);
            expList.load({ef: state.ef, efv: state.efv, efo: state.efo});
            outline(state);
        }

        function triggerStateChanged(state) {
            $(_this).trigger("stateChanged", [A.difference(state, defaults)]);
        }

        function outline(state) {
            state = state || {};
            var el = (state.ef && state.efv) ? $("#" + asElementId(state.ef + state.efv)) : [];
            outlineEl(el.length > 0 ? el[0] : null);
        }

        function outlineEl(el) {
            if (el) {
                var old = $(".heatmap .select");
                old.removeClass("select");
                $(el).addClass("select");
            } else {
                $(".heatmap .select").removeClass("select");
            }
        }

        function asElementId(str) {
            return str ? A.Base64.encode(str).replace(/=/g, "") : str;
        }

        return $.extend(true, _this, {
            /**
             * Reloads the list of experiments with the given parameters and updates the internal state.
             * Parameters are: {
             *     ef - an experiment factor to filter the list of experiments by
             *     efv - an experiment factor value to filter the list of experiments by
             *     efo - an experiment factor ontology term to filter the list of experiment by
             * }
             */
            filterExperiments: externalChange,
            /**
             * Reloads the list of experiments according the current internal state
             */
            init: initialize
        });
    };
})(atlas || {}, jQuery);