<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

<!--
Admin page for the atlas. Use this page to load new experiments or to calculate indexes, netcdfs, or analytics of
experiments in the database.

This page is jsp, as it stores in a session variable the current pageNumber, the number of experiments per page, and the
total number of experiments. These are then used to populate divs on the page with dynamic elements, which can further
be reloaded by scripts that poll for progress of loading jobs via AJAX.

author: Tony Burdett
date: 13-Nov-2009
-->
<%@ page import="uk.ac.ebi.gxa.dao.AtlasDAO" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.microarray.atlas.model.LoadDetails" %>
<%@ page import="java.util.List" %>

<%
    // fetch load/index services
    AtlasDAO atlasDAO = (AtlasDAO) application.getAttribute(Atlas.ATLAS_DAO.key());

    // page number session variable
    int pageNumber = session.getAttribute(Atlas.ADMIN_PAGE_NUMBER.key()) == null
            ? 1
            : (Integer) session.getAttribute(Atlas.ADMIN_PAGE_NUMBER.key());

    // experimentPerPage session variable
    int experimentsPerPage = session.getAttribute(Atlas.ADMIN_EXPERIMENTS_PER_PAGE.key()) == null
            ? 25
            : (Integer) session.getAttribute(Atlas.ADMIN_EXPERIMENTS_PER_PAGE.key());

    List<LoadDetails> exptDetails = atlasDAO.getLoadDetailsForExperiments();

    // number of experiments
    int maxExperiments = exptDetails.size();
%>

<html>
<head>
    <script src="admin.js" type="text/javascript" language="JavaScript"></script>
    <script src="prototype.js" type="text/javascript" language="JavaScript"></script>
    <script src="fluxion-ajax.js" type="text/javascript" language="JavaScript"></script>
    <title>Atlas Administration - Load or recompute data in the Atlas</title>
</head>
<body onload="updateComputeTable(<%=pageNumber%>, <%=experimentsPerPage%>, <%=maxExperiments%>);">

<h1>Atlas Admin Page</h1>

<div id="loader">
    <h2>Load New Experiment</h2>

    <div id="load_submission">
        <table>
            <tr>
                <td>Enter URL of MAGE-TAB format document:</td>
                <!-- Submission form contains url of document to load -->
                <form id="submission"
                      name="submission"
                      action="doload.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <!-- input text entry column -->
                    <td align="left">
                        <input id="input_text"
                               name="magetab.url"
                               type="text"
                               size="70"
                               onkeypress="handleKeyPress(event, this.form);"/>
                    </td>

                    <!-- submit button for form, JScript handles values -->
                    <td align="left">
                        <input type="button" id="submit_button" value="load" onclick="this.form.submit();"/>
                    </td>

                    <!-- end of form -->
                </form>
            </tr>
        </table>
    </div>
</div>

<div id="scheduler">
    <h2>Task Scheduler</h2>

    <div id="netcdfgenerator">
        <h2>NetCDF Repository Scheduler</h2>

        <table>
            <tr>
                <td>
                    <form id="build.netcdfs"
                          name="build.netcdfs"
                          action="donetcdf.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">
                        <input type="hidden" name="type" value="experiment"/>
                        <input type="hidden" name="accession" value="ALL"/>
                        <input type="button" value="regenerate all NetCDFs" onclick="this.form.submit();"/>
                    </form>
                </td>
            </tr>
        </table>
    </div>

    <div id="analyticsgenerator">
        <h2>Analytics Scheduler</h2>

        <table>
            <tr>
                <td>
                    <form id="calculate.analytics"
                          name="calculate.analytics"
                          action="doanalytics.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">
                        <input type="hidden" name="type" value="experiment"/>
                        <input type="hidden" name="accession" value="ALL"/>
                        <input type="button" value="recompute all analytics" onclick="this.form.submit();"/>
                    </form>
                </td>
            </tr>
        </table>
    </div>

    <div id="indexbuilder">
        <h2>SOLR Index Scheduler</h2>

        <table>
            <tr>
                <td>
                    <form id="build.index"
                          name="build.index"
                          action="doindex.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">
                        <input type="hidden" id="pending" name="pending"/>
                        <input type="button" value="index pending objects" onclick="buildIndex(true, this.form);"/>
                        <input type="button" value="rebuild entire index" onclick="buildIndex(false, this.form);"/>
                    </form>
                </td>
            </tr>
        </table>
    </div>
</div>

<!-- loaded by javascript, shows details for experiments one at a time -->
<div id="compute.table">
</div>

<!-- loaded by javascript, enables switching of pages to scroll through experiments -->
<div id="page.switcher">
</div>

</body>
</html>