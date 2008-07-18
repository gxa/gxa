<%@ page import="ae3.service.ArrayExpressSearchService"%>
<%@ page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@page import="ae3.model.AtlasGene"%>
<%@page import="ae3.dao.AtlasDao"%>
<%@ page import="java.util.*"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="ae3.service.AtlasResultSet"%>
<%@page import="ae3.service.AtlasGeneService"%>
<%@page import="ae3.model.AtlasTuple"%>
<%@page import="ae3.model.AtlasExperiment"%>
<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Gene View
<jsp:include page="end_head.jsp"></jsp:include>
<style>
<!--
table.heatmap th {
	background-color: #bdd7d7;
	text-align: left;
	text-indent: 5px;
	font-family: Verdana;
	font-weight: bold;
	font-size: 11px;
	color: #404040;
}

table.heatmap {
	text-align: left;
	font-family: Verdana;
	font-weight: normal;
	font-size: 11px;
	color: #404040;
	background-color: #fafafa;
	border-collapse: collapse;
	border-spacing: 0px;
}

.foo {
	padding-left: 15px;
	padding-top: 20px;
}

.exp_summary {
	padding-left: 15px;
	padding-top: 10px;
}

.separator {
	background-image: url(images/sep.png);
	background-repeat: repeat-x;
	height: 5px;
}

.exp_text {
	color: #404040;
	line-height: 15px;
}

.geneAnnotHeader {
	font-size: 9pt;
	font-weight: lighter;
	color: #408c8c;
	font-family: helvetica, arial, sans-serif;
	text-align: right;
	width: 10%;
}

.moreLink {
	cursor: pointer;
	color: #408c8c;
	text-align: right;
}

.sectionHeader {
	color: #9e9e9e;
	font-size: 14pt;
	text-align: left;
}

.titleHeader {
	color: #9e9e9e;
	font-size: 16pt;
	text-align: left;
}

.fullSectionView {
	color: #404040
}

.RankInfoBar {
	width: 100px;
	height: 8px;
	border: solid 1px #5588ff;
	margin: 3px 4px 2px 0;
	float: left;
	overflow: hidden
}

.RankInfoInt {
	height: 100%;
	background: #99ccff;
	overflow: hidden
}
-->
</style>
<script src="scripts/jquery-1.2.3.js" type="text/javascript"></script>
<script src="scripts/jsdeferred.jquery.js" type="text/javascript"></script>
<script src="scripts/jquery.query-1.2.3.js" type="text/javascript"></script>

<!-- add header scripts here -->
<script type="text/javascript">
<!--
function
viewMore( id )
{

    id = String(id);
    var mainElt = $("#" + id);
    var extElt = $("#" + id.replace("_main", "_ext"));
    if ( mainElt.hasClass("tr_main_expanded")) {
        // collapse now
        mainElt.removeClass("tr_main_expanded");
        extElt.hide();
        mainElt.show();
    } else {
        mainElt.addClass("tr_main_expanded");
        mainElt.hide();
        extElt.show();
        
    }
    onWindowResize();
}
//-->
</script>

<link rel="stylesheet" href="stylesheets/ae_browse.css" type="text/css" />
<link rel="stylesheet" href="stylesheets/ae_index.css" type="text/css" />
<link rel="stylesheet" href="stylesheets/ae_common.css" type="text/css" />
<link rel="stylesheet" href="blue/style.css" type="text/css"
	media="print, projection, screen" />
<jsp:include page='start_body_no_menus.jsp' />
<jsp:include page='end_menu.jsp' />
<div class="foo">
<%
	AtlasGene atlasGene = null;
	String geneId = request.getParameter("gene");
	if (geneId != null) {
		atlasGene = AtlasDao.getGene(geneId);
		atlasGene.getGeneName();
	}
%>
<table class="" width="900">
	<tr>
		<td class="titleHeader">ArrayExpress Atlas Gene View</td>
		<td align="right">
		<div
			style="color: #e33e3e; text-align: right; font-size: 26pt; font-weight: bold">
		<%=atlasGene.getGeneName()%></div>
		<span
			style="border-bottom: thin; margin-top: 0; color: #1f7979; font-size: 9pt; font-weight: bold">
		<%=atlasGene.getGeneSpecies()%> </span></td>
	</tr>

</table>

<table width="900">
	<tr>
		<td class="geneAnnotHeader">Description:</td>
		<td align="left">NF-kapp-B inhibitor alpha</td>

	</tr>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>

	<tr>
		<td class="geneAnnotHeader">Synonyms:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
							"gene_synonym").toString().substring(1).replace(
							']', ' ')%></td>

	</tr>
	<%
		if (atlasGene.getGeneSolrDocument().getFieldValue("gene_disease") != null) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">Disease:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
								"gene_disease")%></td>

	</tr>
	<%
		}
	%>
	<%
		if (atlasGene.getGeneSolrDocument().getFieldValue("gene_goterm") != null) {
	%>
	<tr>
		<td></td>
		<td>
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td class="geneAnnotHeader">GO Terms:</td>
		<td align="left"><%=atlasGene.getGeneSolrDocument().getFieldValue(
								"gene_goterm").toString().substring(1).replace(
								']', ' ')%></td>
	</tr>
	<%
		}
	%>

	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>
</table>
<table width="900" style="margin-left: 100">
	<tr>
		<td class="sectionHeader">Expression Summary</td>
	</tr>
	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>

	<%
		AtlasResultSet atlasResultSet = AtlasGeneService.getExprSummary(atlasGene.getGeneId());
	%>

	<tr>
		<td>
		<div
			style="height: 250px; width: 100%; overflow: auto; overflow-x: hidden">
		<table border="1" class="heatmap" cellpadding="3" cellspacing="0">
			<tr>
				<th rowspan="2">Factor Value</th>
				<th rowspan="2" style="border-right: thick solid; border-left: thin">Studies</th>
				<%--<th><img src="tmp/<%=VerticalTextRenderer.drawString("Total up", application.getRealPath("tmp"))%>" title="Total up"/></th>--%>
				<%--<th style="border-right: thick solid"><img src="tmp/<%=VerticalTextRenderer.drawString("Total down", application.getRealPath("tmp"))%>" title="Total down"/></th>--%>
				<%
					List<HashMap> genes = atlasResultSet.getAtlasResultGenes();
					for (HashMap<String, String> gene : genes) {
				%>
				<th colspan="2" align="center"><%=gene.get("gene_name")%></th>
				<%
					}
				%>
			</tr>
			<tr>
				<th style="border-left: thick solid">UP</th>
				<th>DN</th>
			</tr>

			<%
				HashMap<String, HashMap<String, String>> gars = atlasResultSet.getAtlasResultAllGenesByEfv();
				for (HashMap<String, String> ar : atlasResultSet.getAtlasEfvCounts()) {
					if (!ar.get("efv").startsWith("V1")) {
			%>
			<tr>
				<td nowrap="true"><span style="font-weight: bold"
					title="Matched in experiment(s) <%=ar.get("experiments")%>">
				<%=ar.get("efv").startsWith("V1") ? "--" : ar
							.get("efv")%> </span></td>
				<td style="border-right: thick solid" align="right"><b><%=ar.get("experiment_count")%></b></td>
				<%--<td align="right"><b><%=ar.get("up_count")%></b></td>--%>
				<%--<td style="border-right: thick solid" align="right"><b><%=ar.get("dn_count")%></b></td>--%>

				<%
					for (HashMap<String, String> gene : genes) {
								HashMap<String, String> gar = gars.get(gene.get("gene_identifier")	+ ar.get("efv"));

								if (gar != null && gar.size() != 0) {
									Long r_dn = 255L;
									Long b_dn = 255L;
									Long g_dn = 255L;

									Long r_up = 255L;
									Long b_up = 255L;
									Long g_up = 255L;
									String mpvup = gar.get("mpvup");
									String mpvdn = gar.get("mpvdn");

									String countup = gar.get("countup").equals("0") ? " ": gar.get("countup");
									String countdn = gar.get("countdn").equals("0") ? " ": gar.get("countdn");

									String display = "";
									String display_up = "";
									String display_dn = "";
									String title = "Probes for "
											+ gene.get("gene_identifier")
											+ " found in experiment(s) "
											+ gar.get("experiment_count")
											+ ", observed up "
											+ (countup == null ? 0 : countup)
											+ " times (mean p="
											+ (mpvup == null ? "N/A" : String.format(
													"%.3g", Double.valueOf(mpvup)))
											+ ")"
											+ ", observed down "
											+ (countdn == null ? 0 : countdn)
											+ " times (mean p="
											+ (mpvdn == null ? "N/A" : String.format(
													"%.3g", Double.valueOf(mpvdn)))
											+ ")";

									if (mpvup == null && mpvdn == null) {
										r_up = g_up = b_up = g_dn = r_dn = b_dn = 255L;
									}
									if (mpvdn != null) {
										b_dn = 255L;
										g_dn = 255 - Math.round(Double.valueOf(mpvdn)
												* (-255D / 0.05D) + 255);
										r_dn = 255 - Math.round(Double.valueOf(mpvdn)
												* (-255D / 0.05D) + 255);
										display_up = "0";
										display_dn = countdn;
									}
									if (mpvup != null) {
										r_up = 255L;
										g_up = 255 - Math.round(Double.valueOf(mpvup)
												* (-255D / 0.05D) + 255);
										b_up = 255 - Math.round(Double.valueOf(mpvup)
												* (-255D / 0.05D) + 255);
										display_up = countup;
										display_dn = "0";
									} //else {
									//  g = 0L;
									//  r = Math.round(Double.valueOf(mpvup) * (-255D/0.05D) + 255);
									//  b = Math.round(Double.valueOf(mpvdn) * (-255D/0.05D) + 255);
									//  display = sumup + "/" + sumdn;
									// }
				%>
				<td align="center"
					style="background-color: rgb(<%=   r_up %>, <%=   g_up %>, <%=   b_up %>)"><span
					title="<%=title%>"
					style="text-decoration: none; font-weight: bold; color: lightgray"><%=countup == "" ? " " : countup%></span>
				</td>
				<td align="center"
					style="background-color: rgb(<%=   r_dn %>, <%=   g_dn %>, <%=   b_dn %>)"><span
					title="<%=title%>"
					style="text-decoration: none; font-weight: bold; color: lightgray"><%=countdn == "" ? " " : countdn%></span>
				</td>
				<%
					} else {
				%>
				<td>&nbsp;</td>
				<%
					}
				%>
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
		</td>
	</tr>

	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>
</table>
<%
	ArrayList<AtlasExperiment> exps = ArrayExpressSearchService
			.instance().getRankedGeneExperiments(atlasGene.getGeneId());
%>

<table width="900" cellpadding="2">
	<tr>
		<td class="sectionHeader">Studies(<%=exps.size()%>)</td>
	</tr>
	<%
		for (AtlasExperiment exp : exps) {
	%>
	<tr>
		<td colspan="2">
		<div class="separator"></div>
		</td>
	</tr>
	<tr>
		<td align="left">
		<h3><%=exp.getDwExpAccession()%>: <%=exp.getAerExpName()%></h3>
		</td>
		<t">
		</td>
	</tr>
	<tr>
		<td align="left" class="exp_text">
		<div id="<%=exp.getDwExpId().toString()%>_main"
			class="fullSectionView"><span><%=exp.getAerExpDescription().length() <= 250 ? exp
						.getAerExpDescription() : exp.getAerExpDescription()
						.substring(0, 250)%>
		</span><span class="moreLink"
			onclick="viewMore('<%=exp.getDwExpId().toString() %>_main')">...more</span>


		</div>
		<div id="<%=exp.getDwExpId().toString()%>_ext" class="fullSectionView"
			style="display: none"><span><%=exp.getAerExpDescription()%>
		</span><span class="moreLink"
			onclick="viewMore('<%=exp.getDwExpId().toString() %>_main')">...close</span>
		<br>
		<br>
		<div style="color: #9e9e9e; padding-left: 15px;">Atlas Results
		for <%=atlasGene.getGeneName()%> studied in <%=exp.getAerFactorAttributes().get(1)%></div>
		<div class="exp_summary">
		<table class="heatmap" cellpadding="2">

			<tr>
				<th class="subheading">Factor Value</th>
				<th class="subheading">P-value</th>
				<th class="subheading">Text Summary</th>
			</tr>
			<%
				List<AtlasTuple> atlusTuples = AtlasGeneService.getAtlasResult(
							atlasGene.getGeneId(), exp.getDwExpId().toString());
					for (AtlasTuple atuple : atlusTuples) {
						if (!atuple.getEfv().startsWith("V1")) {
			%>
			<tr>
				<td><%=atuple.getEfv()%></td>
				<td valign="top">
				<%
					if (atuple.getUpdn().equals(1)) {
				%><img src="images/up_arrow.gif">
				<%
					} else if (atuple.getUpdn().equals(-1)) {
				%><img
					src="images/dn_arrow.gif">
				<%
					}
				%><%=atuple.getPval() > 1e-16D ? String
								.format("%.3g", atuple.getPval()) : "< 1e-16"%></td>
				<td><%=atlasGene.getGeneName() + " "
								+ atuple.getTxtSummary()%></td>
			</tr>

			<%
				}
					}
			%>
		</table>
		</div>

		</div>

		</td>
	</tr>
	<%
		}
	%>


</table>
</div>
<!-- end page contents  here -->
<jsp:include page='end_body.jsp' />