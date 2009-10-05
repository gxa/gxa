<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ Copyright 2007 Antony Quinn, EMBL-European Bioinformatics Institute
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
  ~ For further details of the mydas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://code.google.com/p/mydas/
  ~
  -->        

<!--
/**
 * Logic for features.html.
 * Based on the idea of "style-free" stylesheets pioneered by Eric van der Vlist:
 * http://eric.van-der-vlist.com/blog/2368_The_influence_of_microformats_on_style-free_stylesheets.item
 *
 * @author  Antony Quinn
 * @version $Id$
 * @since   1.0
 */
 -->
<xsl:stylesheet	version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:include href="urls.xsl"/>
    <xsl:include href="strings.xsl"/>
    <xsl:include href="stylefree.xsl"/>

    <xsl:output method="html"
                indent="yes"
                media-type="text/html"
                encoding="iso-8859-1"
                omit-xml-declaration="yes"
                standalone="yes"/>

    <!-- Relative to patent2html.xsl -->
    <xsl:param name="template-uri" select="'features.html'"/>

    <!-- Relative to XML files (currently ../test/*.xml) -->
    <xsl:param name="resource-uri" select="'../web/'"/>

    <!-- Source XML document (patent XML) -->
    <xsl:variable name="source" select="/"/>

    <!-- HTML template -->
    <xsl:variable name="layout" select="document($template-uri)"/>

    <!-- Start matching nodes in the HTML template -->
    <xsl:template match="/">
        <xsl:apply-templates select="$layout/html" mode="stylefree-layout"/>
    </xsl:template>

    <!-- Gene/protein ID -->
    <xsl:variable name="query-id" select="$source/DASGFF/GFF/SEGMENT/@id"/>

    <!-- Set image path for sortable table -->
    <xsl:template match="script[not(@src)]" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            var image_path = "<xsl:value-of select="$resource-uri"/>images/";
        </xsl:copy>
    </xsl:template>

    <!-- ======== Summary ======== -->

    <!-- Molecule ID -->
    <xsl:template match="node()[@class='molecule-id']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$source/DASGFF/GFF/SEGMENT/FEATURE[TYPE[@id='description']]/@label"/>
        </xsl:copy>
    </xsl:template>

    <!-- Molecule description -->
    <xsl:template match="node()[@class='molecule-description']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$source/DASGFF/GFF/SEGMENT/FEATURE[TYPE[@id='description']]/NOTE"/>
        </xsl:copy>
    </xsl:template>

    <!-- Sequence length -->
    <xsl:template match="node()[@class='sequence-length']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$source/DASGFF/GFF/SEGMENT/@stop"/>
        </xsl:copy>
    </xsl:template>

    <!-- ======== Feature table ======== -->

    <!-- Table -->
    <xsl:template match="table[@class='sortable']" mode="stylefree-layout">
        <!-- Table type -->
        <xsl:variable name="table-id" select="@id"/>
        <xsl:copy>
            <!-- Table tag attributes -->
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <!-- Header row (column titles) -->
            <xsl:apply-templates select="tr[@class='head']" mode="stylefree-layout"/>
            <!-- Reference to row template - this will be copied for each feature -->
            <xsl:variable name="row-template" select="node()[@class='template']"/>
            <!-- Start/end=0 means 'non-positional features'. -->
            <xsl:choose>
                <xsl:when test="$table-id='positional-features'">
                    <xsl:for-each select="$source/DASGFF/GFF/SEGMENT/FEATURE[not(START = '0') and not(END = '0')]">
                        <xsl:apply-templates select="$row-template" mode="stylefree-layout">
                            <xsl:with-param name="feature" select="current()"/>
                        </xsl:apply-templates>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="$source/DASGFF/GFF/SEGMENT/FEATURE[START = '0' and END = '0']">
                        <!-- Fill in values for row template (see xsl:template match="tr[@class='non-pos'] below) -->
                        <xsl:apply-templates select="$row-template" mode="stylefree-layout">
                            <xsl:with-param name="feature" select="current()"/>
                        </xsl:apply-templates>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>

    <!-- ======== Features (shared) ======== -->

    <!-- Feature row (called for each DAS feature in the source document) -->
    <xsl:template match="tr[@class='template']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="node()" mode="stylefree-layout">
                <xsl:with-param name="feature" select="$feature"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!-- Source -->
    <xsl:template match="node()[@class='source']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <!--xsl:value-of select="$source-id"/-->
        </xsl:copy>
    </xsl:template>

    <!-- Type -->
    <xsl:template match="node()[@class='type']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature/TYPE" mode="source-link"/>
        </xsl:copy>
    </xsl:template>

    <!-- Category -->
    <xsl:template match="node()[@class='category']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature/TYPE/@category"/>
        </xsl:copy>
    </xsl:template>

    <!-- Score -->
    <xsl:template match="node()[@class='score']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature/SCORE" mode="source"/>
        </xsl:copy>
    </xsl:template>

    <!-- Method -->
    <xsl:template match="node()[@class='method']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature/METHOD" mode="source-link"/>
        </xsl:copy>
    </xsl:template>

    <!-- Notes -->
    <xsl:template match="node()[@class='notes']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature/NOTE" mode="source"/>
        </xsl:copy>
    </xsl:template>

    <!-- Feature -->
    <xsl:template match="node()[@class='feature']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$feature" mode="source-link">
                <xsl:with-param name="label" select="$feature/@label"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!-- Graphic -->
    <xsl:template match="node()[@class='graphic' or @class='graphic-padding']" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout">
                <xsl:with-param name="feature" select="$feature"/>
            </xsl:apply-templates>
            <xsl:apply-templates mode="stylefree-layout">
                <xsl:with-param name="feature" select="$feature"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    <!-- Calculate padding (whitespace) as (feature-start / protein-length) * 100 -->
    <xsl:template match="span[@class='graphic-padding']/@style" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <xsl:attribute name="{name()}">
            width:<xsl:value-of select="format-number(($feature/START div $feature/../@stop) * 100, '0.0')"/>%;
        </xsl:attribute>
    </xsl:template>
    <!-- Calculate graphic as ((feature-end - feature-start) / protein-length) * 100 -->
    <xsl:template match="span[@class='graphic']/@style" mode="stylefree-layout">
        <xsl:param name="feature"/>
        <!-- TODO: background-color and border-color depend on feature/@category - get from DAS reference server stylesheet command, otherwise use defaults -->
        <xsl:attribute name="{name()}">
            background-color:<xsl:apply-templates select="$feature/TYPE" mode="background-color"/>;
            border-color:    <xsl:apply-templates select="$feature/TYPE" mode="border-color"/>;
            width:<xsl:value-of select="format-number((($feature/END - $feature/START) div $feature/../@stop) * 100, '0.0')"/>%;
        </xsl:attribute>
    </xsl:template>
    <!-- Lookup colours for given category -->
    <xsl:template match="TYPE" mode="background-color">
        <!-- TODO: include reference server stylesheet in aggregate - if missing, use default Dasty version -->
        <xsl:choose>
            <xsl:when test="@category='Molecule Processing'">
                blue
            </xsl:when>
            <xsl:when test="@category='Secondary structure'">
                red
            </xsl:when>
            <xsl:otherwise>
                purple
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="TYPE" mode="border-color">
        <!-- TODO: include reference server stylesheet in aggregate - if missing, use default Dasty version -->
        <xsl:choose>
            <xsl:when test="@category='Molecule Processing'">
                navy
            </xsl:when>
            <xsl:when test="@category='Secondary structure'">
                maroon
            </xsl:when>
            <xsl:otherwise>
                fuschia
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="NOTE | TARGET" mode="source">
	    <xsl:value-of select="."/><br/>
    </xsl:template>

    <xsl:template match="FEATURE | TYPE | METHOD" mode="source-link">
        <xsl:param name="label" select="."/>
        <xsl:variable name="link">
            <xsl:choose>
                <xsl:when test="name(.) = 'FEATURE'">
                    <xsl:apply-templates select="." mode="source-link-feature"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="." mode="source-link-ontology"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$label='' or $label=@id or starts-with(@label, '__dazzle__')">
                <xsl:copy-of select="$link"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$label"/>
                &#160;
                (<xsl:copy-of select="$link"/>)
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- TODO: get links from config.xml (see PSI XML) -->
    <xsl:template match="node()" mode="source-link-ontology">
        <xsl:choose>
            <xsl:when test="starts-with(@id, 'GO:')">
                <a href="http://www.ebi.ac.uk/ego/DisplayGoTerm?id={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="starts-with(@id, 'ECO:')">
                <a href="http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName={substring-before(@id, ':')}&amp;termId={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="@id"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="LINK" mode="source">
         <a href="{@href}" title="{@type}">
             <xsl:apply-templates select="." mode="source-text"/>
         </a>
    </xsl:template>
    <!-- TODO: sort out link code (bit of a mess) -->
    <xsl:template match="LINK[. = @href]" mode="source-text">
         <xsl:choose>
             <xsl:when test="starts-with(../@id, '__dazzle__')">
                Link
             </xsl:when>
             <xsl:otherwise>
                 <xsl:value-of select="../@id"/>
             </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
    <xsl:template match="LINK[not(. = @href)]" mode="source-text">
         <xsl:choose>
             <xsl:when test="starts-with(../@id, '__dazzle__')">
                <xsl:value-of select="."/>
             </xsl:when>
             <xsl:otherwise>
                 <xsl:value-of select="../@id"/>
                 &#160;
                 (<xsl:value-of select="."/>)
             </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
    <xsl:template match="FEATURE" mode="source-link-feature">
        <xsl:choose>
            <xsl:when test="LINK">
                <xsl:apply-templates select="LINK" mode="source"/>
            </xsl:when>
            <xsl:when test="starts-with(@id, '__dazzle__')">
                <!-- Do not show -->
            </xsl:when>
            <xsl:when test="METHOD[@id='IPI'] or starts-with(@id, 'IPI')">
                <a href="http://www.ebi.ac.uk/integr8/QuickSearch.do?pageContext=201&amp;action=doGeneSearch&amp;organismName=&amp;searchScope=geneSearchAll&amp;geneName={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="METHOD[@id='Uniprot/SWISSPROT']">
                <a href="http://www.ebi.uniprot.org/uniprot-srv/protein/uniProtView.do?proteinAc={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="METHOD[@id='UniGene'] or starts-with(@id, 'Hs.')">
                <a href="http://www.ncbi.nlm.nih.gov/UniGene/clust.cgi?ORG=Hs&amp;CID={substring-after(@id, 'Hs.')}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="METHOD[@id='EntrezGene']">
                <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&amp;cmd=Retrieve&amp;dopt=full_report&amp;list_uids={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="METHOD[@id='UGRepAcc'] or starts-with(@id, 'NM_')">
                <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&amp;db=nucleotide&amp;term={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:when test="METHOD[@id='REFSEQ'] or starts-with(@id, 'NP_')">
                <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&amp;db=protein&amp;term={@id}">
                    <xsl:value-of select="@id"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <!-- Try ontologies -->
                <xsl:apply-templates select="." mode="source-link-ontology"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



    <!-- TODO: make single template for getting Ensembl vs UniProt DAS FEATURE node -->
    <!-- eg. <xsl:variable name="uniprot" select="descendant::FEATURE[TYPE[@id='dbxref'] and METHOD[@id='Ensembl']]"/> -->
    <!-- Get Ensembl gene ID -->
    <xsl:template match="node()" mode="gene-id">
        <xsl:choose>
            <!-- UniProt DAS -->
            <xsl:when test="descendant::FEATURE[TYPE[@id='dbxref'] and METHOD[@id='Ensembl']]">
                <xsl:value-of select="descendant::FEATURE[TYPE[@id='dbxref'] and METHOD[@id='Ensembl']]/@id"/>
            </xsl:when>
            <!-- Ensembl DAS -->
            <xsl:otherwise>
                <xsl:value-of select="descendant::SEGMENT[1]/@id"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="node()" mode="gene-name">
        <xsl:value-of select="descendant::FEATURE[TYPE[@id='description']]/@label"/>
    </xsl:template>

    <xsl:template match="node()" mode="gene-description">
        <xsl:value-of select="descendant::FEATURE[TYPE[@id='description']]/NOTE"/>
    </xsl:template>

    <xsl:template match="node()" mode="chromosome">
        <xsl:apply-templates select="." mode="chromosome-value">
            <xsl:with-param name="getId" select="'true'"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="node()" mode="chromosome-label">
        <xsl:apply-templates select="." mode="chromosome-value">
            <xsl:with-param name="getId" select="'false'"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="node()" mode="chromosome-value">
        <xsl:param name="getId" select="true"/>
        <xsl:choose>
            <!-- Ensembl DAS -->
            <xsl:when test="descendant::FEATURE[TYPE[@id = 'chromosome']]">
                <xsl:variable name="feature"
                              select="descendant::FEATURE[TYPE[@id='chromosome']]"/>
                <xsl:choose>
                    <xsl:when test="$getId = 'true'">
                        <xsl:value-of select="$feature/@id"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$feature/@label"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <!-- UniProt DAS -->
            <xsl:otherwise>
                <xsl:variable name="feature"
                              select="descendant::FEATURE[TYPE[@id='dbxref'] and METHOD[@id='Ensembl']]"/>
                <xsl:choose>
                    <xsl:when test="$getId = 'true'">
                        <xsl:value-of select="substring-after($feature/NOTE, 'Chromosome ')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$feature/NOTE"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
