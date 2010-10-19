<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script type="text/javascript">
    $(document).ready(function () {
        var opts = {
            current_page: ${result.page},
            num_edge_entries: 1,
            items_per_page: ${result.rowsPerPage},
            link_to: '#', //${pageUrl}&p=__id__
            next_text: '>>',
            prev_text: '<<',
            callback: function(page) { bind_data(page); return false; }
        };
        opts.num_display_entries = 2;
        $(".page_short").pagination(${result.total}, opts);
        opts.num_display_entries = 5;
        $(".page_long").pagination(${result.total}, opts);

        $(".export_lnk").bind("click",function() {
            $.ajax({
                url: '${pageUrl}&export=true',
                cache: false,
                dataType: "json",
                success: function(qry, status) {
                    if(qry.qid > 0) {
                        var count = parseInt($("#dwnldCounter").text())+1;
                        $("#dwnldCounter").text(count);
                        $("#dwnldCounter").parent().show();
                    }
			        atlas.popup('downloads');
                }
            });

            alert("Download request sent. Please check the downloads window for status.");
            return false;
        });

        $(".tablesorter").collapsible("td.collapsible", {
            collapse: true,
            callback: atlas.showListThumbs
        }).tablesorter({
            // don't sort by first column
            headers: {0: {sorter: false}}
            // set the widgets being used - zebra stripping
            , widgets: ['zebra']
            , onRenderHeader: function (){
                this.wrapInner("<span></span>");
            }
            , debug: false
        });

    });

function bind_data(page){
    bindTable(page);
}

</script>

<div id="summary">
    <span id="pagetop" class="pagination_ie page_long"></span>
    Genes <c:out value="${result.page * result.rowsPerPage == 0 ? 1 : result.page * result.rowsPerPage}"/>-<c:out value="${(result.page + 1) * result.rowsPerPage > result.total ? result.total : (result.page + 1) * result.rowsPerPage }"/> of <b><c:out value="${result.total}" /></b> total found
    <c:if test="${result.total >= atlasProperties.queryDrilldownMinGenes}">
        <span>(you can <a href="#" onclick="$('#drilldowns').animate({width:'show'});$(this).parent().remove();return false;">refine your query</a>)</span>
    </c:if>
    &nbsp;¥&nbsp;
    <a class="export_lnk" title="Download results in a tab-delimited format." href="#" >Download all results</a>
    <span style="display:${noDownloads > 0 ? 'inline' : 'none' };">- <span id="dwnldCounter">${noDownloads}</span> download(s) <a href="javascript:void(0)" onclick="atlas.popup('downloads')">in progress</a></span>
    &nbsp;¥&nbsp; <c:import url="../includes/apilinks.jsp"><c:param name="apiUrl" value="${query.apiUrl}"/></c:import>
</div>
<div id="legendexpand" style="width:100%;height:30px">

    <div style="line-height:30px;white-space:nowrap">Legend: <img style="position:relative;top:6px" src="${pageContext.request.contextPath}/images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">over</span>/<span style="color:blue;font-weight:bold">under</span> expressed in</div>
</div>

<table id="squery" class="tablesorter" style="width:100%;table-layout:fixed;">
    <colgroup>
        <col style="width:20px" />
        <col style="width:110px" />
        <col style="width:110px" />
        <col style="width:150px" />
        <col style="width:180px"  />
        <col style="width:100%" />
        <col style="width:26px" />
        <col style="width:80px" />
        <col style="width:80px" />
        <col style="width:80px" />
        <col style="width:80px" />
    </colgroup>

    <thead>
        <tr class="header">
            <th style="border-right:none"></th>
            <th style="border-left:none" class="padded">Probe</th>
            <th class="padded">Gene</th>
            <th class="padded">Organism</th>
            <th class="padded">Experimental Factor</th>
            <th class="padded">Factor Value</th>
            <th></th>
            <th class="padded">P-value</th>
            <th class="padded">P-value</th>
            <th class="padded">P-value</th>
            <th class="padded">P-value</th>
        </tr>
    </thead>

    <tbody>

    <c:forEach var="row" items="${result.listResults}" varStatus="r">
        <tr id="${row.gene_id}_${row.ef}_${r.index}">
            <td class="collapsible" style="border-right:none;border-bottom:none;" title="Click here to show experiments...">
                <a href="#" onclick="addGeneToPlot('${row.gene_id}','${u:escapeJS(row.gene_identifier)}','${u:escapeJS(row.gene_name)}','${u:escapeJS(row.ef)}','${u:escapeJS(row.designElement)}');return false;" alt="plot" title="Click to plot profile">
                <img border="0" src="${pageContext.request.contextPath}/images/iconf.png" alt="" />
                </a>
            </td>
            <td class="padded" style="border-left:none">${row.designElement}</td>
            <td class="padded genename">
                <a href="gene/${f:escapeXml(row.gene.geneIdentifier)}">${row.gene_name}</a>
                <div class="gtooltip">
                    <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:forEach items="${atlasProperties.geneAutocompleteNameFields}" var="prop"><c:if test="${!empty row.gene.geneProperties[prop]}">${row.gene.hilitGeneProperties[prop]}, </c:if></c:forEach>${row.gene.geneIdentifier})</div>
                    <c:forEach items="${atlasProperties.geneTooltipFields}" var="prop">
                        <c:if test="${!empty row.gene.geneProperties[prop]}"><div><b>${f:escapeXml(atlasProperties.curatedGeneProperties[prop])}:</b> ${row.gene.hilitGeneProperties[prop]}</div></c:if>
                    </c:forEach>
                </div>
            </td>
            <td class="padded wrapok">${row.gene_species}</td>
            <td class="padded wrapok">${f:escapeXml(atlasProperties.curatedEfs[row.ef])}</td>
            <td class="padded wrapok lvrowefv">${row.fv}</td>
            <c:choose>
                <c:when test="${row.ups == 0 && row.downs == 0 && row.nones > 0}">
                    <td class="acounter" style="color:black;"><div class="osq">${row.nones}</div></td>
                </c:when>
                <c:when test="${row.ups > 0 && row.downs == 0 && row.nones == 0}">
                    <td class="acounter upback"><div class="osq">${row.ups}</div></td>
                </c:when>
                <c:when test="${row.ups == 0 && row.downs > 0 && row.nones == 0}">
                    <td class="acounter downback"><div class="osq">${row.downs}</div></td>
                </c:when>
                <c:when test="${row.ups > 0 && row.downs == 0 && row.nones > 0}">
                    <td class="acounter"><div class="sq"><div class="nuduo"></div><div class="nunoval">${row.nones}</div><div class="nuupval">${row.ups}</div></div></td>
                </c:when>
                <c:when test="${row.ups == 0 && row.downs > 0 && row.nones > 0}">
                    <td class="acounter"><div class="sq"><div class="ndduo"></div><div class="ndnoval">${row.nones}</div><div class="nddnval">${row.downs}</div></div></td>
                </c:when>
                <c:when test="${row.ups > 0 && row.downs > 0 && row.nones == 0}">
                    <td class="acounter"><div class="sq"><div class="udduo"></div><div class="uddnval">${row.downs}</div><div class="udupval">${row.ups}</div></div></td>
                </c:when>
                <c:otherwise>
                    <td class="acounter"><div class="sq"><div class="tri"></div><div class="tdnval">${row.downs}</div><div class="tupval">${row.ups}</div><div class="tnoval">${row.nones}</div></div></td>
                </c:otherwise>
            </c:choose>
            <td class="padded">${u:prettyFloatFormat(row.minPval)}</td>
            <td class="padded">${u:prettyFloatFormat(row.minPval)}</td>
            <td class="padded">${u:prettyFloatFormat(row.minPval)}</td>
            <td class="padded">${u:prettyFloatFormat(row.minPval)}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<div class="pagination_ie page_long"></div>