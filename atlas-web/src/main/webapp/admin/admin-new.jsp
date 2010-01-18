<!--
Admin page for the atlas.  Use this page to load new experiments or to calculate indexes, netcdfs, or analytics of
experiments in the database.

This page is html, but dynamically loads the computer.jsp page via an object.  The computer.jsp is where per-experiment
operations happen.

author: Tony Burdett
date: 13-Nov-2009
-->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <script src="admin.js" type="text/javascript" language="JavaScript"></script>
    <script src="prototype.js" type="text/javascript" language="JavaScript"></script>
    <script src="fluxion-ajax.js" type="text/javascript" language="JavaScript"></script>
    <title>Atlas Administration - Load or recompute data in the Atlas</title>
</head>
<body>

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

<div id="compute.table">
    <%@ include file="computer.jsp" %>
    <%--<jsp:include page="computer.jsp" />--%>
</div>

</body>
</html>