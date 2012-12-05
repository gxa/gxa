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

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="genes" type="java.util.Collection<uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity>" scope="request"/>
<jsp:useBean id="nextQuery" type="java.lang.String" scope="request"/>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <tmpl:stringTemplate name="geneIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <wro4j:all name="bundle-jquery" />
    <wro4j:all name="bundle-common-libs" />
    <wro4j:all name="bundle-gxa" />
    <wro4j:all name="bundle-gxa-page-gene-index" />

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>

    <script type="text/javascript">
        $(document).ready(function() {

            $("#moreResults").each(function() {
                $(this).click(function(e) {
                    var link = $(this),
                            url = link.attr("href");

                    link.hide();

                    atlas.ajaxLoader({
                        url: url,
                        onSuccess: function(data) {
                            var html = $("<div/>");
                            var sample = $("#geneList a").get(0);
                            for (var i = 0; i < data.genes.length; i++) {
                                var gene = data.genes[i];
                                var node = $(sample).clone();
                                var id = node.attr("id");
                                var name = node.text().replace(/^\s+|\s+$/g, "");
                                var href = node.attr("href");
                                var title = node.attr("title");

                                node.attr("id", gene.identifier);
                                node.attr("href", href.substring(0, href.length - id.length) + gene.identifier);
                                node.attr("title", title.substring(0, title.length - name.length) + gene.name);
                                node.html("<nobr>" + gene.name + "</nobr>");
                                html.append(node);
                                html.append(" ");
                            }

                            link.before(html);

                            if (data.nextQuery) {
                                var j = url.indexOf("?"),
                                        newUrl = url.substring(0, (j < 0 ? url.length : j)) + data.nextQuery;
                                link.attr("href", newUrl);
                                link.show();
                            }
                        }
                    }).load({}, link.parent());
                    return false;
                });
            });
        });
    </script>
</head>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
    <div class="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <c:set var="url">${contextPath}/gene/index.htm</c:set>
        <div class="alphabet-index">
            <c:forTokens items="123 a b c d e f g h i j k l m n o p q r s t u v w x y z" delims=" " var="letter">
               <c:set var="prefix" value="${letter == '123' ? '0' : letter}"/>
               <a ${param.prefix == prefix ? 'class="current"' : ''} href="${url}?prefix=${prefix}"
                  title ="Gene Expression Atlas Genes Starting With ${f:toUpperCase(letter)}">${f:toUpperCase(letter)}</a>
            </c:forTokens>
        </div>

        <div id="geneList" class="gene-list">
            <div>
                <c:forEach var="gene" items="${genes}" varStatus="status">
                    <a id="${gene.identifier}" href="${contextPath}/gene/${gene.identifier}"
                       title="Gene Expression Atlas Data For ${gene.name}">
                        <nobr>${gene.name}</nobr>
                    </a>
                </c:forEach>
            </div>

            <c:if test="${! empty nextQuery}">
                <a id="moreResults" href="${contextPath}/gene/index.html${nextQuery}">
                    <nobr>more&gt;&gt;</nobr>
                </a>
            </c:if>
        </div>

    </div>
</div>

</tmpl:stringTemplateWrap>
</html>
