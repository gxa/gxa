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

    var downloadService = (function() {
        var resultUrl = 'download/result?token=',
            progressUrl = 'download/progress',
            cancelUrl = 'download/cancel',
            token = null,
            handler = null;

        function clear() {
            token = null;
            handler = {};
        }

        function handleError(error) {
            A.logError(error || {error: "server failure"});
        }

        function cancelDownload() {
            if (token != null) {
                notifyCancel();

                A.ajaxReq({
                    url:cancelUrl,
                    params:{token:token},
                    success:function () {
                        //ok
                    },
                    error:function (error) {
                        handleError(error);
                    }
                });

                clear();
            }
        }

        function startDownload(url, target, newHandler) {
            handler = newHandler || {};

            notifyStart();

            A.ajaxReq({
                url:url,
                target: target,
                success:function (data) {
                    token = data.token;
                    checkProgress();
                },
                error:function (error) {
                    handleError(error);
                    notifyError();
                }
            });
        }

        function checkProgress() {
            setTimeout(function () {
                if (token == null) {
                    return;
                }
                A.ajaxReq({
                    url:progressUrl,
                    params:{token:token},
                    success:function (data) {
                        var val = data.progress;
                        if (val < 100) {
                            notifyProgress(val);
                            checkProgress(data.token);
                        } else {
                            notifyProgress(val);
                            location.href = A.fullPathFor(resultUrl + token);
                            notifySuccess();
                            clear();
                        }
                    },
                    error:function (error) {
                        handleError(error);
                        notifyError();
                    }
                });
            }, 3000);
        }

        function notifyCancel() {
            if (handler.cancel) {
                handler.cancel();
            }
        }

        function notifyStart() {
            if (handler.start) {
                handler.start();
            }
        }

        function notifyProgress(val) {
            if (handler.progress) {
                handler.progress(val);
            }
        }

        function notifyError() {
            if (handler.error) {
                handler.error("Sorry, a server error occurred. Please try again later.");
            }
        }

        function notifySuccess() {
            if (handler.success) {
                handler.success();
            }
        }

        return {
            startDownload: function(newUrl, newTarget, newHandler) {
                cancelDownload();
                startDownload(newUrl, newTarget, newHandler);
            },
            cancel: function() {
                cancelDownload();
            }
        }
    })();

    A.inlineDownloadDialog = function (url, target) {
        var id = "_inlineDialog" + (new Date().getTime());

        function el(clazz) {
            var elm = $('#' + id);
            return clazz ? $(clazz, elm) : elm;
        }

        function transformUI() {
            $(target).hide();

            $('<div style="position:relative;" class="inlineDownload" id="' + id + '"></div>')
                .html(['<p class="message">Preparing...</p>',
                '<div style="float:left;margin-right:4px;margin-top:4px;" class="cancel-icon cancel" title="Cancel"></div>',
                '<div style="height:10px;margin-top:4px;" class="progressBar anim"></div>',
                '<div style="clear:both;"></div>'].join(""))
                .insertAfter(target);

            $(".cancel", el()).click(function() {
               downloadService.cancel();
            });

            el(".progressBar").progressbar({value:0});
        }

        function restoreUI() {
            el().remove();
            $(target).show();
        }

        function onStart() {
            transformUI();
        }

        function onProgress(val) {
            var bar = el(".progressBar"),
                msg = el(".message");

            if (val > 0 && val < 100) {
               msg.text("Packaging...");
               bar.removeClass("anim");
            } else if (val == 100) {
               msg.text("Done");
            }
            bar.progressbar({value:val});
        }

        function onSuccess() {
            el().delay(1500).queue(function () {
                restoreUI();
            });
        }

        function onError(error) {
            var bar = el(".progressBar"),
                w = bar.width();
            bar.replaceWith('<div style="color:red; width:' + w + 'px;">' + error + '</div>');
        }

        function onCancel() {
            restoreUI();
        }

        return {
            open:function () {
                downloadService.startDownload(url, null, {
                    start: onStart,
                    progress: onProgress,
                    error: onError,
                    success: onSuccess,
                    cancel: onCancel
                });
            }
        }
    }
})(atlas || {}, jQuery);

(function (A, $) {
    var pluginName = "inlineDownloadLink";

    $.extend($.fn, {
        inlineDownloadLink:function (opts) {
            if (this.length > 1) {
                this.each(function () {
                    $(this).inlineDownloadLink(opts);
                });
                return this;
            }

            opts = opts || {};
            var p = $(this).data(pluginName);
            if (!p) {
                var url = opts.url ? opts.url : $(this).attr("href");
                var target = opts.target ? opts.target : this;
                $(this).click(function (ev) {
                    ev.preventDefault();
                    A.inlineDownloadDialog(url, target).open();
                    return false;
                });
                $(this).data(pluginName, true);
            }
            return p;
        }
    });
})(atlas || {}, jQuery);

