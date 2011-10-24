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
     *     pageSize        - number of experiments per page (optional)
     *     ef              - a default ef to list experiments for (optional)
     *     pageState       - page state object to register for stateChange event (optional)
     *     pageStatePrefix - a hierarchical prefix for page state; required if pageState is given
     * }
     */
    A.genePage = function(opts) {
        var _this = {},
            defaults = {ef: opts.ef || null},
            pageState = opts.pageState || null,
            pageStatePrefix = opts.pageStatePrefix || "";

        var expList = atlas.geneExperimentList({
            gene: opts.gene,
            pageSize: opts.pageSize || 5,
            pageState: pageState,
            pageStatePrefix: (pageStatePrefix ? pageStatePrefix + "." : "") + "expl",
            listTarget: "experimentList",
            listTemplate: "experimentListTemplate",
            pageTarget: "experimentListPage",
            pageTemplate: "experimentListPageTemplate"
        });

        if (pageState) {
            pageState.register(_this, pageStatePrefix, function(state) {
                loadExperiments(state);
            });
            outline(
                loadExperiments(
                    pageState.stateFor(pageStatePrefix)));
        }

        function loadExperiments(state) {
            state = A.withComplementary(state || {}, defaults);
            expList.load(state);
            return state;
        }

        function triggerStateChanged(state) {
            $(_this).trigger("stateChanged", [A.difference(state || {}, defaults)]);
        }

        function outline(state) {
            state = state || {};
            if (state.ef && state.efv) {
                outlineRow();
            }
        }

        function outlineRow(el) {
            if (el) {
                var old = $(".heatmap_over");
                old.removeClass("heatmap_over");
                old.addClass("heatmap_row");
                el.className = "heatmap_over";
            } else {
                $(".heatmap_over").removeClass("heatmap_over");
            }
        }

        return $.extend(true, _this, {
            listExperimentsByEfEfv: function(el, ef, efv) {
                triggerStateChanged(
                    loadExperiments({ef:ef, efv:efv})
                );
                outlineRow(el);
            },

            listExperimentsByEfEfo: function(ef, efo) {
                triggerStateChanged(
                    loadExperiments({ef:ef, efo:efo})
                );
            }
        });
    };
})(atlas || {}, jQuery);