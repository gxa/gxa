<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/templates" prefix="tmpl" %>
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

<jsp:useBean id="atlasProperties" type="uk.ac.ebi.gxa.properties.AtlasProperties" scope="application"/>
<jsp:useBean id="genes" type="java.util.Collection" scope="request"/>
<jsp:useBean id="more" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="nextUrl" type="java.lang.String" scope="request"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="eng">
<head>
    <style type="text/css">

        .alertNotice {
            padding: 50px 10px 10px 10px;
            text-align: center;
            font-weight: bold;
        }

        .alertNotice > p {
            margin: 10px;
        }

        .alertHeader {
            color: red;
        }

        #centeredMain {
            width: 740px;
            margin: 0 auto;
            padding: 50px 0;
            height: 100%;
        }

        .roundCorner {
            background-color: #EEF5F5;
        }

        a.Alphabet {
            margin: 10px;
        }

    </style>

    <tmpl:stringTemplate name="geneIndexPageHead"/>

    <meta name="Description" content="Gene Expression Atlas Summary"/>
    <meta name="Keywords"
          content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

    <script type="text/javascript" language="javascript" src='<c:url value="/scripts/jquery-1.3.2.min.js" />'></script>
    <!--[if IE]><script type="text/javascript" src='<c:url value="/scripts/excanvas.min.js"/>'></script><![endif]-->

    <script type="text/javascript" src='<c:url value="/scripts/jquery.pagination.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/feedback.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/jquery.tablesorter.min.js"/>'></script>
    <script type="text/javascript" src='<c:url value="/scripts/jquery.flot.atlas.js"/>'></script>

    <link rel="stylesheet" href='<c:url value="/atlas.css"/>' type="text/css"/>
    <link rel="stylesheet" href='<c:url value="/geneView.css"/>' type="text/css"/>


    <link rel="stylesheet" href='<c:url value="/blue/style.css"/>' type="text/css" media="print, projection, screen"/>
    <link rel="stylesheet" href='<c:url value="/structured-query.css"/>' type="text/css"/>

    <style type="text/css">
        @media print {
            body, .contents, .header, .contentsarea, .head {
                position: relative;
            }
        }
    </style>
</head>

<tmpl:stringTemplateWrap name="page">

<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <div style="margin:100px; font-weight:bold; font-size:larger; text-align:center;">
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=0"/>' title ="Gene Expression Atlas Genes Starting With Digit">123</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=a"/>' title ="Gene Expression Atlas Genes Starting With A">A</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=b"/>' title ="Gene Expression Atlas Genes Starting With B">B</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=c"/>' title ="Gene Expression Atlas Genes Starting With C">C</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=d"/>' title ="Gene Expression Atlas Genes Starting With D">D</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=e"/>' title ="Gene Expression Atlas Genes Starting With E">E</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=f"/>' title ="Gene Expression Atlas Genes Starting With F">F</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=g"/>' title ="Gene Expression Atlas Genes Starting With G">G</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=h"/>' title ="Gene Expression Atlas Genes Starting With H">H</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=i"/>' title ="Gene Expression Atlas Genes Starting With I">I</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=j"/>' title ="Gene Expression Atlas Genes Starting With J">J</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=k"/>' title ="Gene Expression Atlas Genes Starting With K">K</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=l"/>' title ="Gene Expression Atlas Genes Starting With L">L</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=m"/>' title ="Gene Expression Atlas Genes Starting With M">M</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=n"/>' title ="Gene Expression Atlas Genes Starting With N">N</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=o"/>' title ="Gene Expression Atlas Genes Starting With O">O</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=p"/>' title ="Gene Expression Atlas Genes Starting With P">P</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=q"/>' title ="Gene Expression Atlas Genes Starting With Q">Q</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=r"/>' title ="Gene Expression Atlas Genes Starting With R">R</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=s"/>' title ="Gene Expression Atlas Genes Starting With S">S</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=t"/>' title ="Gene Expression Atlas Genes Starting With T">T</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=u"/>' title ="Gene Expression Atlas Genes Starting With U">U</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=v"/>' title ="Gene Expression Atlas Genes Starting With V">V</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=w"/>' title ="Gene Expression Atlas Genes Starting With W">W</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=x"/>' title ="Gene Expression Atlas Genes Starting With X">X</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=y"/>' title ="Gene Expression Atlas Genes Starting With Y">Y</a>
            <a class="alphabet" href='<c:url value="/gene/index.htm?start=z"/>' title ="Gene Expression Atlas Genes Starting With Z">Z</a>
        </div>

        <c:forEach var="gene" items="${genes}">
            <a href='<c:url value="/gene/${gene.id}"/>' title="Gene Expression Atlas Data For ${gene.value}"
               target="_self">${gene.value}</a>&nbsp;
        </c:forEach>

        <c:if test="${more}">
            <a href='<c:url value="/gene/${nextUrl}"/>'>more&gt;&gt;</a>
        </c:if>

    </div>
</div>

</tmpl:stringTemplateWrap>
</html>
