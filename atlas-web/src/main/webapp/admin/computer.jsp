<!--
Second half of the admin page for the atlas. This page handles per-experiment operations, and whereas admin.html is
static, this is much more dynamic. It checks for the presence of the "atlas.page.number" session attribute, and if
present will dynamically fetch load details for the experiments on this page. Clicking the link to change pages will
invoke javascript to reload the entire object with the new page number session variable stored.

author: Tony Burdett
date: 16-Jan-2009
-->
<%@ page import="org.json.JSONArray" %>
<%@ page import="uk.ac.ebi.gxa.dao.AtlasDAO" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.microarray.atlas.model.Experiment" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>

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

<head>
    <script src="admin.js" type="text/javascript" language="JavaScript"></script>
    <script src="prototype.js" type="text/javascript" language="JavaScript"></script>
    <script src="fluxion-ajax.js" type="text/javascript" language="JavaScript"></script>
    <title>Atlas Administration - Recompute data in the Atlas</title>
</head>
<body>
<div id="compute.table">
    <h2>Experiment Recompute Tasks</h2>

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
    </div>

    <div id="page.switcher">
        <table>
            <tr>
                <%
                    if (pageNumber - 3 > 0) {
                %>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), 1)%>changePage(1);">
                        &nbsp;|&lt;&nbsp;
                    </a>
                </td>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber-1)%>changePage(<%=pageNumber-1%>);">
                        &nbsp;&lt;&nbsp;
                    </a>
                </td>
                <td>
                    &nbsp;...&nbsp;
                </td>

                <% }
                    if (pageNumber - 2 > 0) {
                %>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber-2)%>changePage(<%=pageNumber-2%>);">
                        &nbsp;<%=pageNumber - 2%>&nbsp;
                    </a>
                </td>
                <% }
                    if (pageNumber - 1 > 0) {
                %>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber-1)%>changePage(<%=pageNumber-1%>);">
                        &nbsp;<%=pageNumber - 1%>&nbsp;
                    </a>
                </td>
                <% } %>
                <td>&nbsp;<%=pageNumber%>&nbsp;</td>
                <%
                    if ((pageNumber + 1) < numOfPages) {
                %>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber+1)%>changePage(<%=pageNumber+1%>);">
                        &nbsp;<%=pageNumber + 1%>&nbsp;
                    </a>
                </td>
                <% }
                    if ((pageNumber + 2) < numOfPages) {
                %>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber+2)%>changePage(<%=pageNumber+2%>);">
                        &nbsp;<%=pageNumber + 2%>&nbsp;
                    </a>
                </td>
                <% }
                    if ((pageNumber + 3) < numOfPages) {
                %>
                <td>&nbsp;...&nbsp;</td>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber+1)%>changePage(<%=pageNumber+1%>);">
                        &nbsp;&gt;&nbsp;
                    </a>
                </td>
                <td>
                    <a onclick="<%=session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), numOfPages)%>changePage(<%=pageNumber-3%>);">
                        &nbsp;&nbsp;&gt;|&nbsp;&nbsp;
                    </a>
                </td>
                <% } %>
            </tr>
        </table>
        <script type="text/javascript">
            window.onload = function() {
                var exptArray = eval(<%=json.toString()%>);
                for (var i = 0; i < 10; i++) {
                    checkLoadDetails(exptArray[i], "experiment");
                }
            };
        </script>
    </div>
</div>

</body>
</html>