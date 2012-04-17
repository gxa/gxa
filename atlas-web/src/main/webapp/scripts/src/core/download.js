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
(function (A, $) {
    A.downloadDialog = function (url) {
        var downloadUrl = url,
            statusUri = 'download/status',
            cancelUri = 'download/cancel',
            token = null;

        var elem = $('<div style="position:relative;" class="downloadDialog"></div>')
            .html('<p class="message"></p><div class="progressBar"></div>').dialog({
                autoOpen:false,
                resizable:false,
                position:'center',
                modal:true,
                buttons:{
                    "cancel":function () {
                        $(this).dialog("close");
                    },
                    "to background":function () {
                        $(this).dialog("close");
                    }
                },
                open:function () {
                    $(".ui-dialog-titlebar").hide();
                    $('.ui-dialog :button').blur();
                    $('.ui-dialog-buttonpane').css('text-align', 'center');
/*
                    $(".downloadDialog .progressBar").progressbar({
                        value:37
                    });
*/
                }
            });

        function startDownload() {
            A.ajaxReq({
                url:downloadUrl,
                target: $(".downloadDialog .progressBar"),
                success:function () {
                    alert("Success");
                },
                error:function () {
                    alert("Failure");
                }
            });
        }

        return {
            open:function () {
                elem.dialog('open');
                startDownload();
            }
        }
    }
})(atlas || {}, jQuery);

(function (A, $) {
    var pluginName = "downloadLink";

    $.extend($.fn, {
        downloadLink:function (opts) {
            if (this.length > 1) {
                this.each(function () {
                    $(this).downloadLink(opts);
                });
                return this;
            }

            opts = opts || {};
            var p = $(this).data(pluginName);
            if (!p) {
                var url = opts.url ? opts.url : $(this).attr("href");
                $(this).click(function (ev) {
                    ev.preventDefault();
                    A.downloadDialog(url).open();
                    return false;
                });
                $(this).data(pluginName, true);
            }
            return p;
        }
    });
})(atlas || {}, jQuery);

