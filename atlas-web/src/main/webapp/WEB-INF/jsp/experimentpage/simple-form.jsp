<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>

<script type="text/javascript">
        $(document).ready(function() {
            atlas.initSimpleForm();
        });
</script>

<form id="searchForm" action="javascript:void()">
        <table style="width:850px">
            <tr>
                <td class="atlastable"><label class="label" for="gene0">Genes</label></td>
                <td class="atlastable"></td>
                <td class="atlastable">
                    <label class="label" for="fval0">Conditions</label>
                </td>
                <td class="atlastable">
                    <label class="label" for="view">&nbsp;</label>
                </td>
                <td class="atlastable"></td>
            </tr>
            <tr>
                <td class="atlastable">
                    <input type="hidden" name="gprop_0" id="gprop0" value="${query.simple ? f:escapeXml(query.geneConditions[0].factor) : ''}">
                    <input type="text" class="value" name="gval_0" id="gene0" style="width:200px" value="${query.simple ? f:escapeXml(query.geneConditions[0].jointFactorValues) : ''}" /><br>
                </td>
                <td class="atlastable">
                    <select name="fexp_0" id="expr0">
                        <option ${query.simple && 'UP_DOWN' == query.conditions[0].expression ? 'selected="selected"' : ''} value="UP_DOWN">up/down in</option>
                        <option ${query.simple && f:startsWith(query.conditions[0].expression, 'UP') && !f:contains(query.conditions[0].expression, 'DOWN')  ? 'selected="selected"' : ''} value="UP">up in</option>
                        <option ${query.simple && f:startsWith(query.conditions[0].expression, 'DOWN') ? 'selected="selected"' : ''} value="DOWN">down in</option>
                        <option ${query.simple && query.conditions[0].expression == 'NON_D_E' ? 'selected="selected"' : ''} value="NON_D_E">non-d.e. in</option>
                    </select>
                    <input type="hidden" name="fact_0" value="">
                </td>
                <td class="atlastable">
                    <input type="text" class="value" name="fval_0" id="fval0" style="width:200px" value="${query.simple ? f:escapeXml(query.conditions[0].jointFactorValues) : ''}" />
                </td>
                <td class="atlastable" rowspan="2" class="label" nowrap="nowrap" style="vertical-align: top;">
                    <c:if test="${heatmap}">
                        <input type="radio" id="view_hm" name="view" style="vertical-align:bottom" value="hm" checked="checked"><label for="view_hm">Heatmap</label><br>
                        <input type="radio" id="view_ls" name="view" style="vertical-align:bottom" value="list"><label for="view_ls">List</label>
                    </c:if>
                    <c:if test="${list}">
                        <input type="radio" id="view_hm" name="view" style="vertical-align:bottom" value="hm"><label for="view_hm">Heatmap</label><br>
                        <input type="radio" id="view_ls" name="view" style="vertical-align:bottom" value="list" checked="checked"><label for="view_ls">List</label>
                    </c:if>
                </td>
                <td class="atlastable" align="right">
                    <input type="submit" value="Search Atlas" class="searchatlas">
                </td>
            </tr>
            <tr>
                <td class="label" colspan="2"><span style="font-style: italic" class="label">e.g. ASPM, "p53 binding"</span></td>
                <td class="label"><span style="font-style: italic" class="label">e.g. liver, cancer, diabetes</span></td>
                <td class="atlastable" valign="top" align="right" nowrap="nowrap">&nbsp;</td>
            </tr>
        </table>
    </form>