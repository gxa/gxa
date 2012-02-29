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
<%@include file="../includes/global-inc.jsp" %>

<jsp:useBean id="atlasQueryService" class="ae3.service.structuredquery.AtlasStructuredQueryService" scope="application"/>
<jsp:useBean id="query" class="ae3.service.structuredquery.AtlasStructuredQuery" scope="request"/>
<jsp:useBean id="searchMode" class="java.lang.String" scope="request"/>

<c:set var="inAdvancedView"><c:out value="${param.isAdvanced}" default="true"/></c:set>
<c:set var="showSimple" value="${!inAdvancedView || searchMode == 'simple'}"/>

<script type="text/javascript">
    var options = {
        expressions : [
            [ 'UP_DOWN', 'up or down' ],
            [ 'UP', 'up' ],
            [ 'DOWN', 'down' ],
            [ 'NON_D_E', 'non-d.e.' ],
            [ 'ANY', 'up/down/non-d.e. in' ]
        ],
        onlyexpressions : [
            [ 'UP_DOWN', 'up or down' ],
            [ 'UP', 'up' ],
            [ 'UP_ONLY', 'up only' ],
            [ 'DOWN', 'down' ],
            [ 'DOWN_ONLY', 'down only' ],
            [ 'NON_D_E', 'non-d.e.' ],
            [ 'ANY', 'up/down/non-d.e. in' ]
        ],
        species : [
            <c:forEach var="i" varStatus="s" items="${atlasQueryService.speciesOptions}">
            '${u:escapeJS(i)}'<c:if test="${!s.last}">,</c:if>
            </c:forEach>
        ]
    };

    $(document).ready(function() {
        var query = ${u:toJson(query)};
        atlas.initSearchForm(query, "${inAdvancedView ? 'advanced' : 'simple'}");
    });
</script>

<form id="simpleform" class="searchform" action="${pageContext.request.contextPath}/qrs" style="display:${showSimple ? 'visible' : 'none'};">
    <input type="hidden" name="searchMode" value="simple"/>
    <table>
        <tr>
            <td><label class="label" id="simple_genes_tip">Genes</label></td>
            <td></td>
            <td><label class="label">Organism</label></td>
            <td><label class="label" id="simple_conditions_tip">Conditions</label></td>
            <td>
                <c:if test="${inAdvancedView}">
                    <label class="label">View</label>
                </c:if>
            </td>
            <td></td>
        </tr>
        <tr>
            <td>
                <input type="text" name="genes"/><br/>
                <span class="example">e.g. ASPM, "p53 binding"</span>
            </td>
            <td>
                <select name="fexp_0" style="width:110px">
                    <option value="UP_DOWN">up/down in</option>
                    <option value="UP">up in</option>
                    <option value="DOWN">down in</option>
                    <option value="NON_D_E">non-d.e. in</option>
                    <option value="ANY">up/down/non-d.e. in</option>
                </select>
            </td>
            <td>
                <select name="specie_0" style="width:200px">
                    <option value="">(any)</option>
                    <c:forEach var="s"
                               items="${atlasQueryService.speciesOptions}">
                        <option value="${f:escapeXml(s)}">${f:escapeXml(u:upcaseFirst(s))}</option>
                    </c:forEach>
                </select>
            </td>
            <td>
                <input type="text" name="fval_0"/><br/>
                <span class="example">e.g. liver, cancer, diabetes</span>
            </td>
            <td class="label" nowrap="nowrap">
                <div ${inAdvancedView? '' : 'style="display:none"'}>
                    <input type="radio" name="view" style="vertical-align:bottom" value="hm"
                    ${heatmap || !list ? 'checked="checked"' :''}/>Heatmap <br/>
                    <input type="radio" name="view" style="vertical-align:bottom" value="list"
                    ${list ? 'checked="checked"' :''}/>List
                </div>
            </td>
            <td align="right">
                <input type="submit" value="Search Atlas" class="searchatlas">
                <div style="position:relative;width:100%;">
                    <div style="position:absolute;right:0;overflow:visible;height:auto;text-align:right;top:10px;">
                        <c:choose>
                            <c:when test="${inAdvancedView}">
                                <a class="smallgreen" style="font-size:12px" href="javascript:atlas.structMode();">
                                <nobr>advanced search</nobr>
                            </a>
                            </c:when>
                            <c:otherwise>
                                <a class="smallgreen" style="font-size:12px"
                                   href="${pageContext.request.contextPath}/qrs">
                                <nobr>advanced search</nobr>
                            </a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </td>
        </tr>
    </table>
</form>

<c:if test="${inAdvancedView}">
    <form id="structform" class="searchform" action="qrs" style="display:${!showSimple ? 'visible' : 'none'};">
        <input type="hidden" name="searchMode" value="advanced"/>
    <fieldset>
        <legend>Find genes matching all of the following conditions</legend>
        <table cellspacing="3">
            <tbody id="conditions">
            <tr id="helprow">
                <td colspan="4"><em>Empty query</em></td>
            </tr>
            </tbody>
        </table>
    </fieldset>

    <fieldset>
        <legend>Add conditions to the query</legend>
        <table cellspacing="3">
            <tr>
                <td colspan="3"></td>
                <td><label class="label">View</label></td>
                <td></td>
            </tr>
            <tr>
                <td>
                    <select id="geneprops" style="width:200px">
                        <option value="" selected="selected">Gene property</option>
                        <option value="">(any)</option>
                        <c:forEach var="i" items="${atlasQueryService.genePropertyOptions}">
                            <option value="${f:escapeXml(i)}">${f:escapeXml(atlasProperties.curatedGeneProperties[i])}</option>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <select id="factors" style="width:320px">
                        <option value="" selected="selected">Experimental factor</option>
                        <option value="">(any)</option>
                        <option value="efo">EFO</option>
                        <c:forEach var="factor" items="${atlasQueryService.experimentalFactorOptions}">
                            <option value="${factor.name}">${f:escapeXml(factor.displayName)}</option>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <select id="species" style="width:200px">
                        <option value="" selected="selected">Organism</option>
                        <c:forEach var="i" items="${atlasQueryService.speciesOptions}">
                            <option value="${f:escapeXml(i)}">${u:upcaseFirst(f:escapeXml(i))}</option>
                        </c:forEach>
                    </select>
                </td>
                <td class="label" nowrap="nowrap">
                    <input type="radio" name="view" style="vertical-align:bottom" value="hm"
                           ${heatmap ? 'checked="checked"' : ''}/>Heatmap<br/>
                    <input type="radio" name="view" style="vertical-align:bottom" value="list"
                           ${list ? 'checked="checked"' :''}/>List
                </td>
                <td align="right">
                    <input id="structclear" disabled="disabled" type="button" value="New Query"
                           onclick="atlas.clearQuery();"/>
                    <input id="structsubmit" disabled="disabled" type="submit" value="Search Atlas"
                           class="searchatlas"/>
                </td>
            </tr>
            <tr>
                <td colspan="5" align="right">
                        <a class="smallgreen" style="font-size:12px" href="javascript:atlas.simpleMode();">
                            <nobr>simple search</nobr>
                        </a>
                </td>
            </tr>
        </table>
    </fieldset>

        <c:forEach var="c" varStatus="s" items="${result.conditions}">
            <c:if test="${c.ignored}">
                <fieldset class="ignoretext top">
                    <span class="ignored">Ignoring condition &quot;<b>${f:escapeXml(atlasProperties.curatedEfs[c.anyFactor ? 'anything' : c.factor])}</b> matching <b><c:out
                            value="${c.jointFactorValues}"/></b>&quot; as no matching factor values were found</span>
                </fieldset>
            </c:if>
        </c:forEach>
    </form>
    </c:if>
