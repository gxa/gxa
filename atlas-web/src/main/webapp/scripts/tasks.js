var currentState = {};
var atlas = { homeUrl: '' };
var selectedExperiments = {};
var selectAll = false;
var $time = {};
var $tpl = {};
var $options = {
    queueRefreshRate: 2000,
    searchDelay: 500
};

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
    if(stateString != urlParts[1])
        window.location.href = urlParts[0] + '#' + stateString;
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

    currentState = newState;
    redrawCurrentState();
}

function taskmanCall(op, params, func) {
    $('.loadIndicator').css('visibility', 'visible');
    return $.ajax({
        type: "GET",
        url: atlas.homeUrl + "tasks",
        dataType: "json",
        data: $.extend(params, { op : op }),
        success: function (json) {
            $('.loadIndicator').css('visibility', 'hidden');
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

function updateBrowseExperiments() {
    taskmanCall('searchexp', {

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
                $('#experimentList input.continue, #experimentList input.restart').removeAttr('disabled');
            else
                $('#experimentList input.continue, #experimentList input.restart').attr('disabled', 'disabled');
        }

        $('#experimentList').render(result, $tpl.experimentList);
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

        function startSelectedTasks(mode) {
            var accessions = [];
            for(var accession in selectedExperiments)
                accessions.push(accession);

            if(accessions.length == 0 && !selectAll)
                return;

            if(window.confirm('Do you really want to ' + mode.toLowerCase() + ' '
                    + (selectAll ? result.numTotal : accessions.length)
                    + ' experiment(s)?')) {
                function afterEnqueue() {
                    $('#tabs').tabs('select', 1);
                }

                if(selectAll) {
                    taskmanCall('enqueuesearchexp', {
                        runMode: mode,
                        search: $('#experimentSearch').val(),
                        fromDate: $('#dateFrom').val(),
                        toDate: $('#dateTo').val(),
                        pendingOnly: $('#incompleteOnly').is(':checked') ? 1 : 0,
                        autoDepends: 'true'
                    }, afterEnqueue);
                } else {
                    taskmanCall('enqueue', {
                        runMode: mode,
                        accession: accessions,
                        type: 'experiment',
                        autoDepends: 'true'
                    }, afterEnqueue);
                }
                
                selectedExperiments = {};
                selectAll = false;
            }
        }

        $('#experimentList input.continue').click(function () {
            startSelectedTasks('CONTINUE');
        });

        $('#experimentList input.restart').click(function () {
            startSelectedTasks('RESTART');
        });

        $('#experimentList .rebuildIndex input').click(function () {
            if(window.confirm('Do you really want to rebuild index?')) {
                taskmanCall('enqueue', {
                    runMode: 'RESTART',
                    accession: '',
                    type: 'index',
                    autoDepends: 'true'
                }, function(result) {
                    $('#tabs').tabs('select', 1);
                });
            }
        });
    });
}

function updatePauseButton(isRunning) {
    function unpauseTaskman() {
        taskmanCall('restart', {}, function () {
            updatePauseButton(true);
        });
    }

    function pauseTaskman() {
        taskmanCall('pause', {}, function () {
            updatePauseButton(true);
        });
    }
    $('#pauseButton').unbind('click').click(isRunning ? pauseTaskman : unpauseTaskman).val(isRunning ? 'Pause task execution' : 'Restart task execution');
    $('.taskmanPaused').css('display', isRunning ? 'none' : 'inherit');
}

function updateQueue() {
    clearTimeout($time.queue);
    taskmanCall('tasklist', {}, function (result) {
        $('#taskList').render(result, $tpl.taskList);

        for(var i in result.tasks) {
            (function (task) {
                $('#taskList .cancelButton' + task.id).click(function () {
                    if(confirm('Do you really want to cancel task ' + task.type + ' ' + task.accession + '?')) {
                        taskmanCall('cancel', { id: task.id }, function () {
                            updateQueue();
                        });
                    }
                });
            })(result.tasks[i]);
        }

        $('#taskList .cancelAllButton').attr('disabled', result.tasks.length ? '' : 'disabled').click(function () {
            if(confirm('Do you really want to cancel all tasks?')) {
                taskmanCall('cancelall', {}, function () {
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

function redrawCurrentState() {
    if(currentState['exp-s'] != null)
        $('#experimentSearch').val(currentState['exp-s']);
    if(currentState['exp-df'] != null)
        $('#dateFrom').val(currentState['exp-df']);
    if(currentState['exp-dt'] != null)
        $('#dateTo').val(currentState['exp-dt']);
    if(currentState['exp-io'] != null)
        $('#incompleteOnly').attr('checked', currentState['exp-io'] == 1);
    if(currentState['tab'] == 0) {
        clearTimeout($time.queue);
        $('#tabs').tabs('select', 0);
        updateBrowseExperiments();
    } else if(currentState['tab'] == 1) {
        clearTimeout($time.search);
        $('#tabs').tabs('select', 1);
        updateQueue();
    } else if(currentState['tab'] == 2) {
        $('#tabs').tabs('select', 2);

    } else {
        $('#tabs').tabs('select', 0);
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

function compileTemplates() {
    function compileTpl(name, func) {
        $tpl[name] = $('#' + name).compile(func);
        $('#' + name).empty();
    }

    compileTpl('experimentList', {
        '.exprow': {
            'experiment <- experiments' : {
                'label.accession': 'experiment.accession',
                '.stage': 'experiment.stage',
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

        '.rebuildIndex@style': function (r) { return r.context.indexStage == 'DONE' ? 'display:none' : ''; }
    });

    compileTpl('taskList', {
        'tr.task': {
            'task <- tasks': {
                '.state': 'task.state',
                '.type': 'task.type',
                '.accession': 'task.accession',
                '.stage': 'task.stage',
                '.runMode': 'task.runMode',
                '.progress': 'task.progress',
                'input@class+': 'task.id',
                '.@class+': 'task.state'
            }
        }
    });
}

$(document).ready(function () {

    compileTemplates();

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

    updatePauseButton(false);
    restoreState();
});
