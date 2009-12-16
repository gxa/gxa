<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="uk.ac.ebi.microarray.atlas.dao.AtlasDAO" %>
<%@ page import="uk.ac.ebi.microarray.atlas.model.LoadDetails" %>
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
    <%
        // use DAO to fetch load monitor table
        List<LoadDetails> loadDetails = atlasDAO.getLoadDetails();
    %>
    <table>
        <tr>
            <!-- headers -->
            <td>Experiment Accession</td>
            <td colspan="2">In Index</td>
            <td colspan="2">Has NetCDF</td>
            <td colspan="2">Has Analytics</td>
        </tr>
        <%
            for (LoadDetails details : loadDetails) {
                // only do experiments here
                if (details.getLoadType().equals("experiment")) {
                    // bind useful values
                    String accession = details.getAccession();
                    boolean indexed = details.getSearchIndex().equals("done");
                    boolean netCDFed = details.getNetCDF().equals("done");
                    boolean analyticsed = details.getRanking().equals("done");
        %>
        <tr>
            <td><%=accession%>
            </td>
            <%
                if (indexed) {
            %>
            <td>
                <img src="../images/green-tick.png" alt="done" align="left">
            </td>
            <td>
                <!-- form to recompute index -->
                <form id="reschedule.index"
                      name="reschedule.index"
                      action="doreschedule.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="schedule for reindexing" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
            }
            else {
            %>
            <td>
                <img src="../images/red-cross.png" alt="pending" align="left">
            </td>
            <td>
                Pending indexing
            </td>
            <%
                }

                if (netCDFed) {
            %>
            <td>
                <img src="../images/green-tick.png" alt="done" align="left">
            </td>
            <td>
                <!-- form to regenerate NetCDF -->
                <form id="recompute.netcdf"
                      name="recompute.netcdf"
                      action="donetcdf.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="Regenerate NetCDF" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
            }
            else {
            %>
            <td>
                <img src="../images/red-cross.png" alt="pending" align="left">
            </td>
            <!-- form to generate NetCDF -->
            <td>
                <form id="compute.netcdf"
                      name="compute.netcdf"
                      action="donetcdf.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="Generate NetCDF" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
                }

                if (analyticsed) {
            %>
            <td>
                <img src="../images/green-tick.png" alt="done" align="left">
            </td>
            <td>
                <!-- form to recalculate analytics -->
                <form id="recompute.analytics"
                      name="recompute.analytics"
                      action="doanalytics.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="Regenerate Analytics" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
            }
            else {
            %>
            <td>
                <img src="../images/red-cross.png" alt="pending" align="left">
            </td>
            <!-- form to calculate analytics -->
            <td>
                <form id="compute.analytics"
                      name="compute.analytics"
                      action="doanalytics.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="Generate Analytics" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
                }
            %>
        </tr>
        <%
                }
            }
        %>
    </table>
</div>

<h2>Existing Genes</h2>

<div id="genes">
    <%
        // use DAO to fetch load monitor table
        List<LoadDetails> geneLoadDetails = atlasDAO.getLoadDetails();
    %>
    <table>
        <tr>
            <!-- headers -->
            <td>Gene Accession</td>
            <td>In Index</td>
        </tr>
        <%
            for (LoadDetails details : geneLoadDetails) {
                // only do genes here
                if (details.getLoadType().equals("genes")) {
                    // bind useful values
                    String accession = details.getAccession();
                    boolean indexed = details.getSearchIndex().equals("done");
        %>
        <tr>
            <td><%=accession%>
            </td>
            <%
                if (indexed) {
            %>
            <td>
                <img src="../images/green-tick.png" alt="done" align="left">
            </td>
            <td>
                <!-- form to recompute index -->
                <form id="reschedule.gene.index"
                      name="reschedule.gene.index"
                      action="doreschedule.web"
                      method="post"
                      enctype="application/x-www-form-urlencoded">

                    <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                    <input type="hidden" name="accession" value="<%=accession%>"/>
                    <input type="button" value="schedule for reindexing" onclick="this.form.submit();"/>
                </form>
            </td>
            <%
            }
            else {
            %>
            <td>
                <img src="../images/red-cross.png" alt="pending" align="left">
            </td>
        </tr>
        <%
                    }
                }
            }
        %>
    </table>
</div>

</div>


</body>
</html>