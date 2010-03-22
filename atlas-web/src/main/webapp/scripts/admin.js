/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

var currentState = {};
var atlas = { homeUrl: '' };
var selectedExperiments = {};
var selectAll = false;
var $time = {};
var $tpl = {};
var $tab = {};
var $options = {
    queueRefreshRate: 5000,
    searchDelay: 500,
    logNumItems: 20
};
var $msg = {
    operation: {
        ENQUEUE: 'Scheduled',
        CANCEL: 'Cancelled'
    },
    taskType: {
        experiment: 'Process experiment',
        loadexperiment: 'Load experiment',
        loadarraydesign: 'Load array design',
        index: 'Build index',
        unloadexperiment: 'Unload experiment'
    },
    runMode: {
        RESTART: 'Restart',
        CONTINUE: 'Continue'
    },
    event: {
        STARTED: 'Started',
        FINISHED: 'Finished',
        FAILED: 'Failed',
        STOPPED: 'Stopped'
    },
    expStage: {
        NONE: 'Loaded, No NetCDF, No analytics',
        NETCDF: 'NetCDF failed, No analytics',
        ANALYTICS: 'NetCDF complete, Not computed or Failed Analytics',
        DONE: 'Complete'
    },
    stage: {
        experiment: {
            NETCDF: 'Writing NetCDFs',
            ANALYTICS: 'Computing Analytics'
        },
        index: {
            INDEX: 'Indexing'
        },
        loadexperiment: {
            LOAD: 'Loading'
        },
        loadarraydesign: {
            LOAD: 'Loading'
        },
        unloadexperiment: {
            UNLOAD: 'Unloading'
        },
        NONE: 'Nothing',
        DONE: 'Complete'
    },
    taskState: {
        WORKING: 'Working',
        PENDING: 'Pending'
    }
};

function msgMapper(field, dict) {
    return function (r) { return r.item[field] ? $msg[dict][r.item[field]] : ''; };
}

function taskStageMsgMapper(r) {
    var m = $msg.stage[r.item.type][r.item.stage];
    if(!m)
        m = $msg.stage[r.item.stage];
    return m ? m : r.item.stage;
}

var currentStateHash;

function storeState() {
    var urlParts = window.location.href.split('#');
    var stateString = '';
    for(var key in currentState) {
        var value = escape(currentState[key]);
        if(value != '') {
            if(stateString.length > 0)
                stateString += '&';
            stateString += key + '=' + value;
        }
    }
    if(stateString != urlParts[1]) {
        window.location.href = urlParts[0] + '#' + stateString;
        currentStateHash = stateString;
    }
}

function restoreState() {
    var urlParts = window.location.href.split('#');
    var newState = {};
    if(urlParts.length > 1) {
        var states = urlParts[1].split('&');
        for(var i = 0; i < states.length; ++i) {
            var keyValue = states[i].split('=');
            if(keyValue.length == 2) {
                var key = keyValue[0].replace(/[^a-zA-Z0-9-]/g, '');
                newState[key] = unescape(keyValue[1]);
            }
        }
    }

    currentStateHash = urlParts[1];
    currentState = newState;
    redrawCurrentState();
}

function requireLogin(op, params, func) {
    $('#loginMessage').text('');
    $('#loginForm').show();
    $('#tabs').hide();
    $('#logout').hide();

    $('#loginForm form').unbind('submit').submit(function () {
        adminCall('login', { userName: $('#loginUser').val(), password: $('#loginPassword').val() }, function (resp) {
            if(resp.success) {
                $('#loginForm').hide();
                $('#tabs').show();
                $('#logout').show();
                $('#userName').text(resp.userName);
                if(op)
                    adminCall(op, params, func);
                else
                    redrawCurrentState();
            } else {
                $('#loginMessage').text('Invalid username or password');
            }
        });
        return false;
    });
}

function adminCall(op, params, func) {
    $('.loadIndicator').css('visibility', 'visible');
    return $.ajax({
        type: "POST",
        url: atlas.homeUrl + "admin",
        dataType: "json",
        data: $.extend(params, { op : op }),
        success: function (json) {
            $('.loadIndicator').css('visibility', 'hidden');
            if(json.notAuthenticated) {
                requireLogin(op, params, func);
                return;
            } else if(op != 'login' && !$('#logout').is(':visible')) {
                adminCall('getuser', {}, function (r) {
                    $('#userName').text(r.userName);
                    $('#logout').show();
                });
            }

            if(json.error)
                alert(json.error);
            else
                func(json);
        },
        error: function() {
            $('.loadIndicator').css('visibility', 'hidden');
            alert('AJAX error for ' + op + ' ' + params);
        }});
}

function switchToQueue() {
    $('#tabs').tabs('select', $tab.que);
}

function updateBrowseExperiments() {
    adminCall('searchexp', {

        search: $('#experimentSearch').val(),
        fromDate: $('#dateFrom').val(),
        toDate: $('#dateTo').val(),
        pendingOnly: $('#incompleteOnly').is(':checked') ? 1 : 0

    }, function (result) {

        function updateRestartContinueButtons() {
            var cando = selectAll;
            for(var k in selectedExperiments) {
                cando = true;
                break;
            }
            if(cando)
                $('#experimentList .opbuttons input').removeAttr('disabled');
            else
                $('#experimentList .opbuttons input').attr('disabled', 'disabled');
        }

        renderTpl('experimentList', result);
        $('#experimentList tr input.selector').click(function () {
            if($(this).is(':checked'))
                selectedExperiments[this.value] = 1;
            else
                delete selectedExperiments[this.value];
            updateRestartContinueButtons();
        });

        var newAccessions = {};
        for(var i = 0; i < result.experiments.length; ++i)
            newAccessions[result.experiments[i].accession] = 1;
        for(i in selectedExperiments)
            if(!newAccessions[i])
                delete selectedExperiments[i];
        updateRestartContinueButtons();

        $('#selectAll').click(function () {
            if($(this).is(':checked')) {
                $('#experimentList tr input.selector').attr('disabled', 'disabled').attr('checked','checked');
                selectAll = true;
                $('#selectCollapsed').css('visibility', 'visible');
            } else {
                $('#experimentList tr input.selector').removeAttr('disabled').removeAttr('checked');
                selectedExperiments = {};
                selectAll = false;
                $('#selectCollapsed').css('visibility', 'hidden');
            }
            updateRestartContinueButtons();
        });

        if(selectAll)
            $('#selectAll').attr('checked', 'checked');

        function startSelectedTasks(type, mode, title) {
            var accessions = [];
            for(var accession in selectedExperiments)
                accessions.push(accession);

            if(accessions.length == 0 && !selectAll)
                return;

            var autoDep = $('#experimentAutodep').is(':checked');
            if(window.confirm('Do you really want to ' + title + ' '
                    + (selectAll ? result.numTotal : accessions.length)
                    + ' experiment(s)' + (autoDep ? ' and rebuild index afterwards' : '') +'?')) {
                if(selectAll) {
                    adminCall('enqueuesearchexp', {
                        runMode: mode,
                        type: type,
                        search: $('#experimentSearch').val(),
                        fromDate: $('#dateFrom').val(),
                        toDate: $('#dateTo').val(),
                        pendingOnly: $('#incompleteOnly').is(':checked') ? 1 : 0,
                        autoDepends: autoDep
                    }, switchToQueue);
                } else {
                    adminCall('enqueue', {
                        runMode: mode,
                        accession: accessions,
                        type: type,
                        autoDepends: autoDep
                    }, switchToQueue);
                }
                
                selectedExperiments = {};
                selectAll = false;
            }
        }

        $('#experimentList input.continue').click(function () {
            startSelectedTasks('experiment', 'CONTINUE', 'continue processing of');
        });

        $('#experimentList input.restart').click(function () {
            startSelectedTasks('experiment', 'RESTART', 'restart processing of');
        });

        $('#experimentList input.unload').click(function () {
            startSelectedTasks('unloadexperiment', 'RESTART', 'unload');
        });

        $('#experimentList .rebuildIndex input').click(function () {
            if(window.confirm('Do you really want to rebuild index?')) {
                adminCall('enqueue', {
                    runMode: 'RESTART',
                    accession: '',
                    type: 'index',
                    autoDepends: 'true'
                }, switchToQueue);
            }
        });
    });
}

function updatePauseButton(isRunning) {
    function unpauseTaskman() {
        adminCall('restart', {}, function () {
            updatePauseButton(true);
            updateQueue();
        });
    }

    function pauseTaskman() {
        adminCall('pause', {}, function () {
            updatePauseButton(true);
            updateQueue();
        });
    }
    $('#pauseButton').unbind('click').click(isRunning ? pauseTaskman : unpauseTaskman).val(isRunning ? 'Pause task execution' : 'Restart task execution');
    $('.taskmanPaused').css('display', isRunning ? 'none' : 'inherit');
}

function updateQueue() {
    clearTimeout($time.queue);
    adminCall('tasklist', {}, function (result) {
        renderTpl('taskList', result);

        for(var i in result.tasks) {
            (function (task) {
                $('#taskList .cancelButton' + task.id).click(function () {
                    if(confirm('Do you really want to cancel task ' + task.type + ' ' + task.accession + '?')) {
                        adminCall('cancel', { id: task.id }, function () {
                            updateQueue();
                        });
                    }
                });
            })(result.tasks[i]);
        }

        $('#cancelAllButton').unbind('click').attr('disabled', result.tasks.length ? '' : 'disabled').click(function () {
            if(confirm('Do you really want to cancel all tasks?')) {
                adminCall('cancelall', {}, function () {
                    updateQueue();
                });
            }
        });

        updatePauseButton(result.isRunning);

        $time.queue = setTimeout(function () {
            updateQueue();
        }, $options.queueRefreshRate);
    });
}

function updateOperLog() {
    clearTimeout($time.queue);
    $time.queue = null;
    adminCall('operlog', { num: $options.logNumItems }, function (result) {
        renderTpl('operLog', result);
        $time.queue = setTimeout(function () {
            updateOperLog();
        }, $options.queueRefreshRate);
    });
}

function updateTaskLog() {
    clearTimeout($time.queue);
    $time.queue = null;
    adminCall('tasklog', { num: $options.logNumItems }, function (result) {
        renderTpl('taskLog', result);
        $('#taskLog .retry input').each(function (i,e) {
            var li = result.items[i];
            if(li.event == 'FAILED')
                $(e).click(function() {
                    if(window.confirm('Are you sure you want to re-try to '
                            + $msg.taskType[li.type].toLowerCase() + (li.accession != '' ? ' ' + li.accession : '') +'?'))
                    {
                        var autoDep = window.confirm('Do you want to run further processing tasks automatically afterwards?');
                        adminCall('enqueue', {
                            runMode: 'RESTART',
                            accession: li.accession,
                            type: li.type,
                            autoDepends: autoDep
                        }, switchToQueue);
                    }
                });
            else
                $(e).remove();
        });
        $time.queue = setTimeout(function () {
            updateTaskLog();
        }, $options.queueRefreshRate);
    });
}

function updateProperties() {
    adminCall('proplist', {}, function (result) {
        renderTpl('propList', result);
        $('#propList tr.property td.value').each(function (i, o) {
            var pr = result.properties[i];
            $(o).click(function () {
                var i = $('<input type="text"/>').attr('name', pr.name).addClass('value').val("" + pr.value);
                $(this).empty().append(i).unbind('click');
                i.focus();
                $('#savePropsButton,#cancelPropsButton').removeAttr('disabled');
            });
        });
        $('#propList form').submit(function () {
            saveProperties();
            return false;
        });
        $('#savePropsButton,#cancelPropsButton').attr('disabled', 'disabled');
    });
}

function saveProperties() {
    var newValues = {};
    $('#propList table input.value').each(function (i, o) {
        newValues[$(o).attr('name')] = $(o).val();
    });
    adminCall('propset', newValues, updateProperties);
}

function redrawCurrentState() {
    for(var timeout in $time) {
        clearTimeout($time[timeout]);
        delete $time[timeout];
    }
    if(currentState['exp-s'] != null)
        $('#experimentSearch').val(currentState['exp-s']);
    if(currentState['exp-df'] != null)
        $('#dateFrom').val(currentState['exp-df']);
    if(currentState['exp-dt'] != null)
        $('#dateTo').val(currentState['exp-dt']);
    if(currentState['exp-io'] != null)
        $('#incompleteOnly').attr('checked', currentState['exp-io'] == 1);
    if(currentState['tab'] == $tab.exp) {
        $('#tabs').tabs('select', $tab.exp);
        updateBrowseExperiments();
    } else if(currentState['tab'] == $tab.que) {
        $('#tabs').tabs('select', $tab.que);
        updateQueue();
    } else if(currentState['tab'] == $tab.load) {
        $('#tabs').tabs('select', $tab.load);
    } else if(currentState['tab'] == $tab.olog) {
        updateOperLog();
        $('#tabs').tabs('select', $tab.olog);
    } else if(currentState['tab'] == $tab.tlog) {
        updateTaskLog();
        $('#tabs').tabs('select', $tab.tlog);
    } else if(currentState['tab'] == $tab.prop) {
        updateProperties();
        $('#tabs').tabs('select', $tab.prop);
    } else {
        $('#tabs').tabs('select', $tab.exp);
        $('#experimentSearch').val('');
        updateBrowseExperiments();
    }
}

function storeExperimentsFormState() {
    currentState['exp-s'] = $('#experimentSearch').val(); 
    currentState['exp-df'] = $('#dateFrom').val();
    currentState['exp-dt'] = $('#dateTo').val();
    currentState['exp-io'] = $('#incompleteOnly').is(':checked') ? 1 : 0;
    storeState();
}

function renderTpl(name, data) {
    $('#'+name).render(data, $tpl[name]);
}

function compileTemplates() {
    function compileTpl(name, func) {
        $tpl[name] = $('#' + name).compile(func);
        $('#' + name).empty();
    }

    compileTpl('experimentList', {
        '.exprow': {
            'experiment <- experiments' : {
                'label.accession': 'experiment.accession',
                '.stage': msgMapper('stage', 'expStage'),
                '.loaddate': 'experiment.loadDate', 
                '.selector@checked': function (r) { return selectedExperiments[r.item.accession] || selectAll ? 'checked' : ''; },
                '.selector@disabled': function () { return selectAll ? 'disabled' : ''; },
                '.selector@value': 'experiment.accession',
                '.selector@id+': 'experiment.accession',
                'label@for+': 'experiment.accession'
            }
        },
        '.expall@style': function (r) { return r.context.experiments.length ? '' : 'display:none'; },
        '.expnone@style': function (r) { return r.context.experiments.length ? 'display:none' : ''; },

        '.expcoll@style': function (r) { return r.context.numCollapsed > 0 ? '' : 'display:none'; },
        '#selectCollapsed@style': function () { return selectAll ? '' : 'visibility:hidden'; },
        '.numcoll': 'numCollapsed',

        '.rebuildIndex .label': function (r) { return r.context.indexStage == 'DONE' ? 'Index build is complete' : 'Index build is incomplete'; }
    });

    compileTpl('taskList', {
        'thead@style': function(r) { return r.context.tasks.length ? '' : 'display:none'; },
        'tr.task': {
            'task <- tasks': {
                '.state': msgMapper('state', 'taskState'),
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'task.accession',
                '.user': 'task.user',
                '.stage': taskStageMsgMapper,
                '.runMode': msgMapper('runMode', 'runMode'),
                '.progress': 'task.progress',
                'input@class+': 'task.id',
                '.@class+': ' state#{task.state} mode#{task.runMode} type#{task.type}'
            }
        }
    });

    compileTpl('operLog', {
        'thead@style': function(r) { return r.context.items.length ? '' : 'display:none'; },
        'tbody tr' : {
            'litem <- items': {
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'litem.accession',
                '.runMode': msgMapper('runMode', 'runMode'),
                '.operation': msgMapper('operation', 'operation'),
                '.message': 'litem.message',
                '.time': 'litem.time',
                '.user': 'litem.user',
                '.@class+': ' operation#{litem.operation} mode#{litem.runMode} type#{litem.type}'
            }
        }
    });

    compileTpl('taskLog', {
        'thead@style': function(r) { return r.context.items.length ? '' : 'display:none'; },
        'tbody tr' : {
            'litem <- items': {
                '.type': msgMapper('type', 'taskType'),
                '.accession': 'litem.accession',
                '.stage': taskStageMsgMapper,
                '.event': msgMapper('event', 'event'),
                '.message': 'litem.message',
                '.time': 'litem.time',
                '.@class+': ' event#{litem.event} stage#{litem.stage} type#{litem.type}'
            }
        }
    });

    compileTpl('propList', {
        'tr.property' : {
            'property <- properties': {
                '.name': 'property.name',
                '.value': 'property.value'
            }
        }
    });
}

$(document).ready(function () {

    compileTemplates();
    
    $('#tabs li a').each(function (i, a) {
        $tab[$(a).attr('href').substr(5)] = i;
    });

    for(var k in $options)
        $('.option-'+k).text($options[k]);

    $('#tabs').tabs({
        show: function(event, ui) {
            if(currentState['tab'] != ui.index) {
                currentState['tab'] = ui.index;
                storeState();
                redrawCurrentState();
            }
        },
        selected: '-1'
    });

    $('#dateFrom,#dateTo').datepicker({
        dateFormat: 'dd/mm/yy',
        onSelect: function() {
            storeExperimentsFormState();
            updateBrowseExperiments();
        }
    });

    $('#experimentBrowseForm').bind('submit', function() {
        storeExperimentsFormState();
        updateBrowseExperiments();
        return false;
    });

    $('#incompleteOnly').bind('click', function () {
        storeExperimentsFormState();
        updateBrowseExperiments();
    });

    $('#experimentSearch').bind('keydown', function (event) {
        var keycode = event.keyCode;
        if(keycode == 13) {
            clearTimeout($time.search);
            $time.search = null;
            storeExperimentsFormState();
            updateBrowseExperiments();
        } else if((keycode == 8 || keycode == 46 ||
                  (keycode >= 48 && keycode <= 90) ||      // 0-1a-z
                  (keycode >= 96 && keycode <= 111) ||     // numpad 0-9 + - / * .
                  (keycode >= 186 && keycode <= 192) ||    // ; = , - . / ^
                  (keycode >= 219 && keycode <= 222))
                && $(this).val().length > 2) {

            clearTimeout($time.search);
            $time.search = setTimeout(function () {
                storeExperimentsFormState();
                updateBrowseExperiments();
            }, $options.searchDelay);
        }
    });

    $('#loadExperimentButton').click(function () {
        var url = $('#loadExperimentUrl').val();
        var autoDep = $('#loadExperimentAutodep').is(':checked');
        adminCall('enqueue', {
            runMode: 'RESTART',
            accession: url,
            type: 'loadexperiment',
            autoDepends: autoDep
        }, switchToQueue);
    });

    $('#loadArrayDesignButton').click(function () {
        var url = $('#loadArrayDesignUrl').val();
        var autoDep = $('#loadArrayDesignAutodep').is(':checked');
        adminCall('enqueue', {
            runMode: 'RESTART',
            accession: url,
            type: 'loadarraydesign',
            autoDepends: autoDep
        }, switchToQueue);
    });

    $('#cancelPropsButton').click(updateProperties);
    $('#savePropsButton').click(saveProperties);

    $('#logout a').click(function () {
        adminCall('logout', {}, function () {
            requireLogin(null, {}, null);
        });
        return false;
    });

    updatePauseButton(false);
    restoreState();

    setInterval(function () {
        if(document.location.href.indexOf('#') && currentStateHash && currentStateHash != document.location.href.substring(document.location.href.indexOf('#') + 1)) {
            console.log(document.location.href.substring(document.location.href.indexOf('#') + 1) + " " + currentStateHash);
            restoreState();
        }
    }, 500);
});
