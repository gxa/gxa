<%@ page import="org.json.JSONArray" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.microarray.atlas.dao.AtlasDAO" %>
<%@ page import="uk.ac.ebi.microarray.atlas.model.Experiment" %>
<%@ page import="java.util.List" %>
<%--
  Admin page for the atlas.  Use this page to load new experiments or to build indexes or netcdfs, or to perform
  analyses, of experiments in the database.

  author: Tony Burdett
  date: 13-Nov-2009
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    // fetch load/index services
    AtlasDAO atlasDAO = (AtlasDAO) application.getAttribute(Atlas.ATLAS_DAO.key());

    // use DAO to fetch list of experiments
    List<Experiment> experiments = atlasDAO.getAllExperiments();
    JSONArray json = new JSONArray();
%>
<html>
<head>
    <script src="admin.js" type="text/javascript" language="JavaScript"></script>
    <script src="prototype.js" type="text/javascript" language="JavaScript"></script>
    <script src="fluxion-ajax.js" type="text/javascript" language="JavaScript"></script>
    <title>Atlas Administration - Load or recompute data in the Atlas</title>
</head>
<body>

<h1>Atlas Admin Page</h1>

<div id="load">
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

<div id="recompute">
    <h2>Existing Experiments</h2>

    <div id="experiments">
        <table>
            <!-- headers -->
            <tr id="headers">
                <td>Experiment Accession</td>
                <td>Has NetCDF</td>
                <td>Has Analytics</td>
                <td>In Index</td>
            </tr>

            <%
                // create a table row for each experiment in the database
                for (Experiment expt : experiments) {
                    // update our json array with this accession
                    json.put(expt.getAccession());
            %>
            <tr id="experiment_<%=expt.getAccession()%>">
                <td id="experiment_<%=expt.getAccession()%>_accession"><%=expt.getAccession()%>
                </td>
                <td id="experiment_<%=expt.getAccession()%>_netcdf" align="left">
                    <img src="../images/ajax-loader.gif" alt="?"/>
                </td>
                <td id="experiment_<%=expt.getAccession()%>_analytics" align="left">
                    <img src="../images/ajax-loader.gif" alt="?"/>
                </td>
                <td id="experiment_<%=expt.getAccession()%>_index" align="left">
                    <img src="../images/ajax-loader.gif" alt="?"/>
                </td>
            </tr>
            <%
                }
            %>
        </table>
        <script type="text/javascript">
            window.onload = function() {
                var exptArray = eval('(<%=json.toString()%>)');
                for (var i = 0; i < 10; i++) {
                    checkLoadDetails(exptArray[i], "experiment");
                }
            };
        </script>
    </div>
</div>

</body>
</html>