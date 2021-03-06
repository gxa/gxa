<%--
  ~ Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>

<%@include file="includes/global-inc.jsp" %>

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>

    <jsp:include page="/WEB-INF/jsp/includes/global-inc-head.jsp"/>

    <tmpl:stringTemplate name="adminPageHead"/>

    <script type="text/javascript" src="scripts/jquery-1.7.1.min.js"></script>
    <script type="text/javascript" src="scripts/jquery-ui/jquery-ui-1.8.18.custom.min.js"></script>
    <script type="text/javascript" src="scripts/jquery.pagination.js"></script>
    <script type="text/javascript" src="scripts/jquery.tmpl.min.js"></script>
    <script type="text/javascript" src="scripts/pure2.js"></script>
    <script type="text/javascript" src="scripts/admin.js"></script>
    <link rel="stylesheet" href="atlas.css" type="text/css"/>
    <link rel="stylesheet" href="scripts/jquery-ui/jquery-ui-1.7.2.atlas.css" type="text/css"/>
    <link rel="stylesheet" href="admin.css" type="text/css"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>

</head>

<tmpl:stringTemplateWrap name="page">

<div class="ae_pagecontainer">
<table id="menu">
    <tr>
        <td>
            &nbsp;
            <b>administration</b>
            <span id="logout">| <a href="#">logout</a> (you are <span id="userName"></span>)</span>
        </td>
    </tr>
</table>

<div id="tabs">
<ul>
    <li><a href="#tab-que">Tasks</a></li>
    <li><a href="#tab-exp">Experiments</a></li>
    <li><a href="#tab-ad">Array designs</a></li>
    <li><a href="#tab-prop">Configuration</a></li>
    <li><a href="#tab-asys">System info</a></li>
    <li><a href="#tab-annSrc">Annotation Sources</a><sup>beta</sup></li>
</ul>

<div id="tab-exp">
    <fieldset>
        <form action="#" id="experimentBrowseForm">
            <table>
                <tr>
                    <td class="label"><label for="experimentSearch">Experiment:</label></td>
                    <td colspan="2">
                        <input type="text" id="experimentSearch" class="value" style="width:400px;">
                        <input type="submit" onclick="updateBrowseExperiments(); return false;">
                    </td>
                    <td>
                        <img src="images/indicator.gif" alt="" width="16" height="16" style="visibility:hidden;"
                             class="loadIndicator">
                    </td>
                </tr>
                <tr>
                    <td class="label"><label for="dateFrom">Load date:</label></td>
                    <td>
                        <input type="text" id="dateFrom" class="date value"> to <input type="text" id="dateTo"
                                                                                       class="date value">
                    </td>
                    <td style="padding-left:20px;" align="right">
                        <select id="incompleteOnly" style="width:100%">
                            <option value="ALL">Any status</option>
                            <option value="COMPLETE">Fully complete</option>
                            <option value="INCOMPLETE">Incomplete by any means</option>
                            <option value="INCOMPLETE_ANALYTICS">Waiting for analytics rebuild</option>
                            <option value="INCOMPLETE_NETCDF">Waiting for NetCDF rebuild</option>
                            <option value="INCOMPLETE_INDEX">Waiting for indexing</option>
                        </select>
                    </td>
                    <td></td>
                </tr>
            </table>
        </form>
    </fieldset>

    <div id="experimentList">
        <table class="results">
            <thead class="expall">
            <tr>
                <td class="sel">

                </td>
                <td class="accession">Accession</td>
                <td>Load Date</td>
                <td>Status</td>
                <td>Description</td>
                <td>Assays</td>
                <td>History</td>
            </tr>
            </thead>
            <tbody>
            <tr class="exprow">
                <td class="sel">
                    <input class="selector" type="checkbox" value="" id="selExp">
                </td>
                <td class="accession"><label class="accession" for="selExp">E-TABM-1</label></td>
                <td class="loaddate">10/10/2010</td>
                <td class="analytics">Loaded</td>
                <td class="description">Blah</td>
                <td class="numassays">Blah</td>
                <td>
                    <a class="history" href="#">Show history</a>
                </td>
            </tr>
            <tr class="expcoll whenanything">
                <td class="sel">
                    <input checked="checked" disabled="disabled" id="selectCollapsed" type="checkbox">
                </td>
                <td colspan="6" class="selall">
                    <label for="selectCollapsed">Total <span class="total">4</span> result(s) on pages </label>
                    &nbsp;&nbsp;&nbsp;
                    <span id="expPages" class="pagination_ie"></span>
                </td>
            </tr>
            <tr class="expall">
                <td class="sel">
                    <input id="selectAll" type="checkbox">
                </td>
                <td colspan="6" class="selall">
                    <label for="selectAll">Select all results <span class="expcoll">(on all pages)</span></label>
                </td>
            </tr>
            </tbody>
        </table>

        <div class="none">
            No matching experiments found! Please check your query.
        </div>

        <fieldset class="expall">
            <table class="rebuildIndex">
                <tr>
                    <td class="label"><label for="rebuildButton">Index build is incomplete</label></td>
                    <td style="padding-left:10px">
                        <input type="button" id="rebuildButton" value="Rebuild whole index">
                    </td>
                </tr>
            </table>
            <div class="opbuttons">
                <input type="button" value="Repair" class="repair">
                <input type="button" value="Unload" class="unload">
                |
                <input type="button" value="Make private" class="makeprivate">
                <input type="button" value="Make public" class="makepublic">
                |
                <input type="button" value="Recompute analytics" class="analytics">
                <input type="button" value="Update NetCDFs" class="updatenetcdf">
            </div>
            <div style="margin-top:5px;" class="opbuttons">
                <input type="checkbox" id="experimentAutodep">
                <label for="experimentAutodep">Automatically process depending tasks</label>
            </div>
        </fieldset>
    </div>
</div>

<div id="tab-ad">
    <fieldset>
        <form action="#" id="adBrowseForm">
            <table>
                <tr>
                    <td class="label"><label for="adSearch">Array design:</label></td>
                    <td>
                        <input type="text" id="adSearch" class="value" style="width:400px;">
                    </td>
                    <td>
                        <img src="images/indicator.gif" alt="" width="16" height="16" style="visibility:hidden;"
                             class="loadIndicator">
                    </td>
                </tr>
            </table>
        </form>
    </fieldset>

    <div id="adList">
        <table>
            <thead>
            <tr>
                <td class="accession">Accession</td>
                <td>Provider</td>
                <td>Description</td>
                <td>Accession Master</td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="accession">E-TABM-1</td>
                <td class="provider">Blah</td>
                <td class="description">Blah</td>
                <td class="accession-master">Blah</td>
                <td>
                    <a class="history" href="#">Show history</a>
                </td>
            </tr>
            </tbody>
            <tfoot>
            <tr>
                <td colspan="5">
                    Total <span class="total">4</span> result(s) <span class="pager">on pages</span>
                    &nbsp;&nbsp;&nbsp;
                    <span id="adPages" class="pagination_ie"></span>
                </td>
            </tr>
            </tfoot>
        </table>
        <div class="none">
            No matching array designs found! Please check your query.
        </div>
    </div>
</div>

<script id="softwareGroupTmpl" type="text/x-jQuery-tmpl">
    {{if softwareGroups }}

    {{each(key, group) softwareGroups}}
    <div id="software_group_\${group.id}" class="softwareGroup">
        <div class="softwareVersions">
            <div><b>\${group.name}</b><br/> versions:</div>
            {{each group.versions}}
            <div class="version" id="software_\${id}">\${version}
                {{if isActive}}
                <br/><span class="active" title="The currently used version">active</span>
                {{/if}}
            </div>
            {{/each}}
        </div>
        <div class="softwareContent">
            <div class="annotSourceList"></div>
        </div>
    </div>
    {{/each}}

    <fieldset id="batchActionButtons">
        <div>
            <button class="updateAnnotations">Update Annotations</button>
            <button class="updateMappings">Update Array Design Mappings</button>
        </div>
    </fieldset>

    {{else}}
    <div class="msg">No any annotation sources available</div>
    {{/if}}

    <fieldset>
        <div>
            <button class="createNewAnnotSource">Create New Annotation Source</button>
            <select id="annotSourceType">
                <option value="BioMart">BioMart</option>
                <option value="GeneSigDB">GeneSigDB</option>
                <option value="Reactome">Reactome</option>
            </select>
        </div>
        <div>
            <button class="syncAnnotationSources">Synchronise Annotation Sources</button>
            &nbsp;<span id="syncProgress" class="syncProgress"/>
        </div>
        <div class="syncErrors"/>
    </fieldset>
</script>

<script id="annotSourceListTmpl" type="text/x-jQuery-tmpl">
    {{if software}}
    <div class="softwareInfo">\${software.name} &raquo; \${software.version} &raquo;
        {{if software.isActive}}
        currently used
        {{else}}
        <a href="#" id="activateSoftware_\${software.id}" class="activateSoftware"
           title="Start using this version and rebuild the index">start using this version</a>
        {{if software.isObsolete}}
        &raquo;
        <a href="#" id="deleteSoftware_\${software.id}" class="deleteSoftware" title="Delete this version">delete</a>
        {{/if}}
        {{/if}}
    </div>
    {{/if}}
    {{if annotSources.length > 0}}
    <table>
        <tr>
            <th><input type="checkbox" class="selectAll" title="Select all"/></th>
            <th>Type</th>
            <th>Organism</th>
            <th>BioEntity Types</th>
            <th>Annotations</th>
            <th>Mappings</th>
            <th></th>
            <th></th>
            <th></th>
        </tr>
        {{each annotSources}}
        <tr
        {{if isObsolete}} class="obsolete" {{/if}}>
        <td><input id="annot_batch_\${id}" class="annotCheckbox" type="checkbox"/></td>
        <td>\${type}</td>
        <td>\${organism}</td>
        <td>\${bioEntityTypes}</td>
        <td>\${annotLoaded}</td>
        <td>\${mappingsLoaded}</td>
        <td><a href="#" id="annot_edit_\${id}" data-type="\${type}" class="edit"
               title="View annotation source properties">view/edit</a></td>
        {{if isObsolete}}
        <td>obsolete</td>
        {{else}}
        <td><a href="#" id="annot_test_\${id}" data-type="\${type}" class="test"
               title="Test if annotations could be loaded">test</a></td>
        {{/if}}
        </td>
        <td id="annot_test_results_\${id}">
            <div class="inlineValidation"></div>
        </td>
        </tr>
        {{/each}}
    </table>
    {{else}}
    <div class="msg">No any annotation sources available</div>
    {{/if}}
</script>

<script id="annotSourceEditTmpl" type="text/x-jQuery-tmpl">
    <div class="annotSourceForm">
        <form action="#" id="editAnnotSourceForm" onSubmit="return false">
            <input type="hidden" name="annotSourceId" value="\${id}"/>
            <input type="hidden" name="typeName" value="\${type}"/>
            <table cellspacing="5">
                <tr>
                    <td><label>Type:&nbsp;</label>\${type}</td>
                </tr>
                <tr>
                    <td><label>Annotation Source:</label></td>
                </tr>
                <tr>
                    <td>
                        <textarea rows="20" cols="100" name="body" class="value">\${body}</textarea>
                    </td>
                </tr>
                <tr>
                    <td class="validationErrors"></td>
                </tr>
                <tr>
                    <td>
                        <fieldset>
                            {{if !isObsolete}}
                            <button class="saveAnnotSource">Save</button>
                            {{/if}}
                            <button class="cancelAnnotSource">Cancel</button>
                        </fieldset>
                    </td>
                </tr>
            </table>
        </form>
    </div>
</script>

<script id="annotSourceFormErrorsTmpl" type="text/x-jQuery-tmpl">
    <p>Errors:</p>
    <ul>
        {{each(i, value) errors}}
        <li>\${value}</li>
        {{/each}}
    </ul>
</script>

<script id="annotSourceInlineErrorsTmpl" type="text/x-jQuery-tmpl">
    {{if errors.length > 0}}
    <div class="failure">
        <span>there are failures:</span>
        <ul>
            {{each(i, value) errors}}
            <li>\${value}</li>
            {{/each}}
        </ul>
    </div>
    {{else}}
    <div class="success">success</div>
    {{/if}}
</script>

<div id="tab-annSrc">
    <div id="annotSourceList"></div>
    <div id="annotSourceEditor"></div>
</div>

<div id="tab-que">
    <fieldset>
        <legend>Load experiment / array design</legend>
        <table>
            <tr>
                <td style="vertical-align:top">
                    <select id="loadType">
                        <option value="auto">Autodetect by file name(s)</option>
                        <option value="experiment">Load Experiment(s)</option>
                        <option value="arraydesign">Load Array Design(s)</option>
                    </select>
                </td>
                <td colspan="2">
                    <input type="text" class="value" id="loadUrl">
                </td>
                <td style="vertical-align:top;padding-top:5px;">
                    <a href="#" id="loadExpand">more files</a>
                </td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input type="checkbox" id="loadAutodep">
                    <label for="loadAutodep">Automatically process the experiment after loading</label>
                </td>
                <td></td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input type="checkbox" id="private" checked="checked">
                    <label for="private">Mark the experiment as private</label>
                </td>
                <td></td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input type="checkbox" id="useRawData" checked="checked">
                    <label for="useRawData">Use raw experimental data if possible</label>
                </td>
            </tr>
            <tr>
                <td></td>
                <td> Use:
                    <select id="normalizationMode">
                        <option value="oligo">oligo</option>
                        <option value="affy">affy</option>
                    </select>
                    mode for experiment normalisation
                </td>
                <td style="text-align:right">
                    <input type="button" value="Load" id="loadButton">
                </td>
            </tr>
        </table>
    </fieldset>
    <fieldset>
        <legend>Running tasks</legend>
        <table style="width:100%">
            <tr>
                <td style="white-space:nowrap">
                    <input type="button" value="Pause queue" id="pauseButton">
                    <input id="cancelAllButton" type="button" value="Cancel all tasks">
                </td>
                <td style="padding-left:10px;white-space:nowrap">
                    <span id="numWorking">1</span> task(s) working / <span id="numPending">2</span> task(s) pending
                </td>
                <td style="padding-left:10px;white-space:nowrap">
                    <div id="taskListPages" class="pagination_ie"></div>
                </td>
                <td><img src="images/indicator.gif" alt="" width="16" height="16" style="visibility:hidden;"
                         class="loadIndicator"></td>
                <td style="text-align:right;width:90%">
                    <input type="checkbox" checked="checked" id="taskLogRefresh">
                    <label for="taskLogRefresh">Auto-refresh log</label>
                </td>
            </tr>
        </table>
    </fieldset>
    <fieldset class="taskmanPaused">
        Task manager is now paused. All currently working tasks will finish, but no new tasks will be started.
    </fieldset>
    <div id="taskList">
        <table class="results">
            <thead>
            <tr>
                <td>State</td>
                <td>Task Type</td>
                <td>Accession/URL</td>
                <td>User</td>
                <td>Progress</td>
                <td class="elapsed">Elapsed</td>
                <td class="cancel"></td>
            </tr>
            </thead>
            <tbody>
            <tr class="task">
                <td class="state">WORKING</td>
                <td style="white-space:nowrap;">
                    <span class="type">experiment</span>
                    <span class="runMode">RESTART</span>
                </td>
                <td class="accession">E-TABM-1</td>
                <td class="user">foo</td>
                <td class="progress">Wasting time...</td>
                <td class="elapsed"></td>
                <td class="cancel">
                    <input class="cancelButton" type="button" value="Cancel">
                </td>
            </tr>
            </tbody>
        </table>
    </div>

    <div id="taskLog">
        <table>
            <thead class="pager">
            <tr>
                <td colspan="7" style="padding:10px;font-weight:normal;">
                    <div class="pagination_ie" id="taskLogPages"></div>
                </td>
            </tr>
            </thead>
            <thead>
            <tr>
                <td>Time</td>
                <td>Event</td>
                <td>User</td>
                <td>Task Type</td>
                <td>Accession/URL</td>
                <td colspan="2">Comment</td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <select id="taskLogEventFilter">
                        <option value=" " class="anyOption">Any</option>
                    </select>
                </td>
                <td>
                    <select id="taskLogUserFilter">
                        <option value=" " class="anyOption">Any</option>
                    </select>
                </td>
                <td>
                    <select id="taskLogTypeFilter">
                        <option value=" " class="anyOption">Any</option>
                    </select>
                </td>
                <td>
                    <input type="text" class="value" id="taskLogAccessionFilter">
                </td>
                <td colspan="2"></td>
            </tr>
            </thead>
            <tbody id="taskLogItems">
            <tr>
                <td class="time">10:34</td>
                <td class="event">STARTED</td>
                <td class="user">user</td>
                <td style="white-space:nowrap;">
                    <span class="type">experiment</span>
                    <span class="runMode">RESTART</span>
                </td>
                <td class="accession">E-TABM-1</td>
                <td class="message">
                    Blah...
                </td>
                <td class="retry"><input type="button" value="Retry"></td>
            </tr>
            </tbody>
        </table>
        <div class="none">
            No matching log entries were found
        </div>
    </div>

</div>

<div id="tab-prop">
    <div id="propList">
        <form action="#">
            <table>
                <thead>
                <tr>
                    <td>Name</td>
                    <td>Value</td>
                </tr>
                </thead>
                <tbody>
                <tr class="property">
                    <td class="name">atlas.blah.balh</td>
                    <td class="value">some value</td>
                </tr>
                </tbody>
            </table>
        </form>
    </div>
    <fieldset>
        <input type="button" value="Save" id="savePropsButton">
        <input type="button" value="Cancel" id="cancelPropsButton">
        <img src="images/indicator.gif" alt="" width="16" height="16" style="visibility:hidden;" class="loadIndicator">
    </fieldset>
</div>

<div id="tab-asys">
    <table id="aboutSystem">
        <tr>
            <td>Datasource</td>
            <td class="dbUrl"></td>
        </tr>
        <tr>
            <td>EFO</td>
            <td class="efo"></td>
        </tr>
        <tr>
            <td>Data Path</td>
            <td class="pathData"></td>
        </tr>
        <tr>
            <td>Index Path</td>
            <td class="pathIndex"></td>
        </tr>
        <tr>
            <td>Webapp Path</td>
            <td class="pathWebapp"></td>
        </tr>
    </table>
</div>

</div>
<!-- tabs -->

<div id="loginForm" style="display:none;">
    <form action="#">
        <fieldset>
            <legend>Login</legend>
            <table>
                <tr>
                    <td><label for="loginUser">Username:</label></td>
                    <td><input type="text" id="loginUser" class="value"></td>
                </tr>
                <tr>
                    <td><label for="loginPassword">Password:</label></td>
                    <td><input type="password" id="loginPassword" class="value"></td>
                </tr>
                <tr>
                    <td>
                        <input type="submit" id="loginButton" value="Login">
                    </td>
                    <td id="loginMessage"></td>
                </tr>
            </table>
        </fieldset>
    </form>
</div>

<div class="expTaskLog">
    <table>
        <thead>
        <tr>
            <td>Time</td>
            <td>Task Type</td>
            <td>Event</td>
            <td>Accession/URL</td>
            <td>User</td>
            <td>Comment</td>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td class="time">10:34</td>
            <td style="white-space:nowrap;">
                <span class="type">experiment</span>
                <span class="runMode">RESTART</span>
            </td>
            <td class="event">STARTED</td>
            <td class="accession">E-TABM-1</td>
            <td class="user">user</td>
            <td class="message">
                Blah...
            </td>
        </tr>
        </tbody>
    </table>
    <div class="none">
        No associated events were found in system logs
    </div>
</div>

</div>
<!-- ae_pagecontainer -->

</tmpl:stringTemplateWrap>
</html>
