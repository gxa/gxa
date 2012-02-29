/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
(function ($) {
    var pluginName = "uiTip";

    function UITip(elem, text) {
        $("&nbsp;<span class='uiTip'>?</span>").appendTo(elem);
        var tip = $(".uiTip", elem);
        tip.css({ opacity: 0.3 });
        tip.mouseenter(function() {
            $(this).fadeTo('fast', 1.0);
        });
        tip.mouseleave(function() {
            $(this).fadeTo('slow', 0.3);
        });

        tip.qtip({
            content:text,
            style: {
                classes: 'ui-tooltip-rounded'
            },
            position: {
                my: 'top left',
                at: 'bottom left'
            },
            show:'mouseover',
            hide:'mouseout'
        });
    }

    $.extend($.fn, {
        uiTip:function (text) {
            if (this.length > 1) {
                this.each(function () {
                    $(this).uiTip(text);
                });
                return this;
            }

            var p = $(this).data(pluginName);
            if (!p) {
                p = new UITip(this, text);
                $(this).data(pluginName, p);
            }
            return p;
        }
    });
})(jQuery);

