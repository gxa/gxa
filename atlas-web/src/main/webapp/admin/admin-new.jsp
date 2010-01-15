<%@ page import="org.json.JSONArray" %>
<%@ page import="uk.ac.ebi.gxa.dao.AtlasDAO" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.microarray.atlas.model.Experiment" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
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

    // sort this list based on alphabetic comparison of accessions
    Collections.sort(experiments, new Comparator<Experiment>() {

        public int compare(Experiment experiment1, Experiment experiment2) {
            return experiment1.getAccession().compareTo(experiment2.getAccession());
        }
    });

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

                // page number session variable
                int pageNumber = session.getAttribute(Atlas.ADMIN_PAGE_NUMBER.key()) == null
                        ? 1
                        : (Integer) session.getAttribute(Atlas.ADMIN_PAGE_NUMBER.key());

                // but, only do 25 at a time, based on page number variable
                int numOfPages = experiments.size() % 25 == 0 ? experiments.size() / 25 : (experiments.size() / 25) + 1;

                for (int i = (pageNumber * 25) - 25; i < experiments.size() && i < (pageNumber * 25); i++) {
                    Experiment expt = experiments.get(i);

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

        <table>
            <tr>
                <%
                    if (pageNumber - 3 > 0) {
                %>
                <td>&nbsp;|&lt;&nbsp;</td>
                <td>&nbsp;&lt;&nbsp;</td>
                <td>&nbsp;...&nbsp;</td>
                <% }
                    if (pageNumber - 2 > 0) {
                %>
                <td>&nbsp;<%=pageNumber - 2%>&nbsp;</td>
                <% }
                    if (pageNumber - 1 > 0) {
                %>
                <td>&nbsp;<%=pageNumber - 1%>&nbsp;</td>
                <% } %>
                <td>&nbsp;<%=pageNumber%>&nbsp;</td>
                <%
                    if ((pageNumber + 1) * 25 < experiments.size()) {
                %>
                <td>&nbsp;<%=pageNumber + 1%>&nbsp;</td>
                <% }
                    if ((pageNumber + 2) * 25 < experiments.size()) {
                %>
                <td>&nbsp;<%=pageNumber = 2%>&nbsp;</td>
                <% {
                    if ((pageNumber + 3) * 25 < experiments.size()) {
                %>
                <td>&nbsp;...&nbsp;</td>
                <td>&nbsp;&gt;&nbsp;</td>
                <td>&nbsp;&gt;|&nbsp;</td>
                <% } %>
            </tr>
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