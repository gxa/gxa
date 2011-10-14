<%--
  ~ Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

<%@ page isELIgnored="true" %>

<script id="experimentListTemplate" type="text/x-jquery-tmpl">
    <table>
        <tr>
            <td class="section-header-1">Expression Profiles</td>
            <td align="right">
                <span id="allStudiesLink" class="pagination_ie"></span>
                <span id="pagination" class="pagination_ie"></span>
            </td>
        </tr>
        <tr>
            <td>
                <table width="100%">
                    <tr>
                        <td valign="top">
                            <div class="section-header-2">
                                ${expTotal} experiment{{if expTotal > 1}}s{{/if}} showing differential expression
                                {{if efInfo}} in "${efInfo}"{{/if}}
                            </div>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <div id="experimentListPage">
                </div>
            </td>
        </tr>
    </table>
</script>

<script id="experimentListPageTemplate" type="text/x-jquery-tmpl">
    <table class="experiment-list" cellpadding="0">
        {{each exps}}
        <tr class="experiment-title">
            <td>
                <table width="100%">
                    <tr>
                        <td class="nowrap">
                            ${this.accession}:
                        </td>
                        <td>
                            ${this.description}
                            {{if this.pubmedId}}
                            &nbsp;&nbsp;<a class="external"
                                           href="http://www.ncbi.nlm.nih.gov/pubmed/${this.pubmedId}"
                                           target="_blank">PubMed ${this.pubmedId}</a>
                            {{/if}}
                        </td>
                    </tr>
                </table>
            </td>
        </tr>

        {{if this.experimentFactors}}
        <tr>
            <td>
                <div style="padding-top:5px;padding-bottom:5px;vertical-align:middle">
                    <div style="padding-bottom:5px;"><span class="section-header-2">Experimental Factors</span></div>
                    <div id="${this.accession}_${gene.geneId}_efPagination"></div>
                </div>
            </td>
        </tr>
        {{/if}}

        <tr>
            <td>
                <table width="100%">
                    <tr>
                        <td valign="top" width="300px">
                            <table>
                                <tr align="left">
                                    <td align="center">

                                        <a href="#"
                                           style="border:none;text-decoration:none;outline:none;"
                                           atlas-uri="/experiment/${this.accession}/${gene.geneIdentifier}?ef=${ef}">
                                           <div id="${this.accession}_${gene.geneId}_plot" class="plot"
                                                style="width: 300px; height: 150px;">
                                           </div>
                                        </a>
                                        <div id="${this.accession}_${gene.geneId}_plot_thm"></div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                        <td>
                            <div style="overflow-y: auto; width:330px; height:150px"
                                 id="${this.accession}_${gene.geneId}_legend"></div>
                        </td>
                    </tr>
                    <tr>
                        <td align="left">
                            <div align="left" id="${this.accession}_${gene.geneId}_arraydesign"></div>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td>
                &nbsp;Show <a title="Show expression profile in detail"
                              href="#" atlas-uri="/experiment/${this.accession}/${gene.geneIdentifier}?ef=${ef}">expression
                profile</a>
                &nbsp;/&nbsp;
                <a class="external" target="_blank" title="Show experiment details in ArrayExpress Archive"
                   href="http://www.ebi.ac.uk/arrayexpress/browse.html?keywords=${this.accession}&detailedview=on">experiment
                    details</a>
                <br/><br/>
            </td>
        </tr>

        <tr class="delimiter">
            <td>&nbsp;</td>
        </tr>

        {{/each}}
    </table>
</script>


