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
<head><title>Atlas Administration - Load or recompute data in the Atlas</title></head>
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
                        <input type="button" id="submit_button" value="Load" onclick="this.form.submit();"/>
                    </td>

                    <!-- end of form -->
                </form>
            </tr>
        </table>
    </div>
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
                <td>In Index</td>
                <td>Has NetCDF</td>
                <td>Has Analytics</td>
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
                <td>
                    <%
                        if (indexed) {
                    %>
                    <img src="../images/green-tick.png" alt="done">
                    <!-- form to recompute index -->
                    <form id="recompute.index"
                          name="recompute.index"
                          action="doindex.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Recompute Index" onclick="this.form.submit();"/>
                    </form>
                    <%
                    }
                    else {
                    %>
                    <img src="../images/red-cross.png" alt="pending">
                    <!-- form to recompute index -->
                    <form id="compute.index"
                          name="compute.index"
                          action="doindex.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Compute Index" onclick="this.form.submit();"/>
                    </form>
                    <%

                        }
                    %>
                </td>
                <td>
                    <%
                        if (netCDFed) {
                    %>
                    <img src="../images/green-tick.png" alt="done">
                    <!-- form to recompute index -->
                    <form id="recompute.netcdf"
                          name="recompute.netcdf"
                          action="donetcdf.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Regenerate NetCDF" onclick="this.form.submit();"/>
                    </form>
                    <%
                    }
                    else {
                    %>
                    <img src="../images/red-cross.png" alt="pending">
                    <!-- form to recompute index -->
                    <form id="compute.netcdf"
                          name="compute.netcdf"
                          action="donetcdf.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Generate NetCDF" onclick="this.form.submit();"/>
                    </form>
                    <%

                        }
                    %>
                </td>
                <td>
                    <%
                        if (analyticsed) {
                    %>
                    <img src="../images/green-tick.png" alt="done">
                    <!-- form to recompute index -->
                    <form id="recompute.analytics"
                          name="recompute.analytics"
                          action="doanalytics.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Regenerate Analytics" onclick="this.form.submit();"/>
                    </form>
                    <%
                    }
                    else {
                    %>
                    <img src="../images/red-cross.png" alt="pending">
                    <!-- form to recompute index -->
                    <form id="compute.analytucs"
                          name="compute.analytics"
                          action="doanalytics.web"
                          method="post"
                          enctype="application/x-www-form-urlencoded">

                        <input type="hidden" name="type" value="<%=details.getLoadType()%>"/>
                        <input type="hidden" name="accession" value="<%=accession%>"/>
                        <input type="button" value="Generate Analytics" onclick="this.form.submit();"/>
                    </form>
                    <%

                        }
                    %>
                </td>
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
                <td>Experiment Accession</td>
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
                <td>
                    <%
                        if (indexed) {
                    %>
                    Done! <!-- todo - add green tick and recompute option -->
                    <%
                    }
                    else {
                    %>
                    Pending! <!-- todo - add red cross and  "index" button -->
                    <%

                        }
                    %>
                </td>
            </tr>
            <%

                    }
                }
            %>
        </table>
    </div>

</div>


</body>
</html>