<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div>
    <jsp:include page="../includes/atlas-header.jsp"/>

    <div style="float:right;margin:20px;">
        <a href="http://www.ebi.ac.uk/arrayexpress/experiments/${exp.accession}" target="_blank"
           title="Experiment information and full data in ArrayExpress Archive" class="geneName"
           style="vertical-align: baseline">${exp.accession}</a>

        <div style="border:1px black solid;padding:5px;">
            <table cellpadding="2" cellspacing="0" border="0">
                <tr>
                    <td>Platform:</td>
                    <td>
                        <c:forEach var="arrayDesign" items="${arrayDesigns}">
                            <a href="${pageContext.request.contextPath}/experiment/${exp.accession}?ad=${arrayDesign}">${arrayDesign}</a>&nbsp;
                        </c:forEach>
                    </td>
                </tr>
                <tr>
                    <td>Organism:</td>
                    <td>${exp.organism}</td>
                </tr>
                <tr>
                    <td>Samples:</td>
                    <td>${exp.numSamples}</td>
                </tr>
                <tr>
                    <td>Individuals:</td>
                    <td>${exp.numIndividuals}</td>
                </tr>
                <tr>
                    <td>Study type:</td>
                    <td>${exp.studyType}</td>
                </tr>
            </table>
        </div>
        <ul style="padding-left:15px">
            <li><a href="${pageContext.request.contextPath}/experimentDesign/${exp.accession}" style="font-size:12px;font-weight:bold;">experiment design</a></li>
            <li><a href="${pageContext.request.contextPath}/experiment/${exp.accession}" style="font-size:12px;font-weight:bold;">experiment analysis</a></li>
            <!--
            <li><a href="#" style="font-size:12px;font-weight:bold;">similiarity</a></li>
            -->
	    <c:if test="${empty experimentDesign}">
	      <c:import url="gallery.jsp"></c:import>
	    </c:if>
        </ul>
    </div>
</div>

<div>
    <span class="sectionHeader" style="vertical-align: baseline">${exp.description}</span>

    <table style="border:none">
        <tr>
            <td>
                <div style="font-size:xx-small; font-style:italic;">data shown for array design:</div> 
                <span class="geneName">
                ${arrayDesign}
                </span>
            </td>
            <td>
                <p>
                    ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                                                           target="_blank">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>
            </td>
        </tr>
    </table>
</div>
    
