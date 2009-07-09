<?xml version="1.0" encoding="utf-8"?>
<!--
/**
 * Logic for patent.html.
 * Based on the idea of "style-free" stylesheets pioneered by Eric van der Vlist:
 * http://eric.van-der-vlist.com/blog/2368_The_influence_of_microformats_on_style-free_stylesheets.item
 *
 * @author  Antony Quinn
 * @version $Id$
 * @since   1.0
 */
 -->
<xsl:stylesheet	version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:z="http://www.ebi.ac.uk/Rebholz/"
        exclude-result-prefixes="z">

    <xsl:include href="entities.xsl"/>
    <xsl:include href="strings.xsl"/>
    <xsl:include href="stylefree.xsl"/>

    <xsl:output method="html"
                indent="yes"
                media-type="text/html"
                encoding="iso-8859-1"
                omit-xml-declaration="yes"
                standalone="yes"/>

    <!-- Relative to patent2html.xsl -->
    <xsl:param name="template-uri" select="'patent.html'"/>

    <!-- Relative to XML files (currently ../data/x/output/*.xml) -->
    <xsl:param name="resource-uri" select="'../../../web/'"/>

    <!-- Source XML document (patent XML) -->
    <xsl:variable name="source" select="/"/>

    <!-- HTML template -->
    <xsl:variable name="layout" select="document($template-uri)"/>

    <!-- Start matching nodes in the HTML template -->
    <xsl:template match="/">
        <xsl:apply-templates select="$layout/html" mode="stylefree-layout"/>
    </xsl:template>

    <!-- Set image path for sortable table -->
    <xsl:template match="script[not(@src)]" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            var image_path = "<xsl:value-of select="$resource-uri"/>images/";
        </xsl:copy>
    </xsl:template>

    <xsl:template match="title/text()" mode="stylefree-layout">
        <xsl:apply-templates select="."/>
        <xsl:value-of select="$source/BioTechPatent/PatentFullText/PublicationNumber/PatentNumber"/>
    </xsl:template>

    <xsl:template match="node()[@id='patent-id']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$source/BioTechPatent/PatentFullText/PublicationNumber/PatentNumber"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="node()[@id='patent-date']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$source/BioTechPatent/PatentFullText/PublicationNumber/Date"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="node()[@class='description']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$source/BioTechPatent/PatentFullText/Desc" mode="source"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="node()[@class='claims']" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$source/BioTechPatent/PatentFullText/Claims" mode="source"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="s" mode="source">
        <xsl:apply-templates select="node()" mode="source"/>
    </xsl:template>

    <xsl:template match="br" mode="source">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="source"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="text()" mode="source">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="z:e" mode="source">
        <span class="entity">
            <xsl:apply-templates select="." mode="entities-link-text"/>
            <sup>
                <xsl:value-of select="concat(@sem, ':')"/>
                <xsl:apply-templates select="." mode="entities-link-ids"/>
            </sup>
        </span>
    </xsl:template>

    <!-- Summary -->

    <xsl:template match="table[@class='sortable']" mode="stylefree-layout">
        <xsl:copy>
            <!-- Table tag attributes -->
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <!-- Header row (column titles) -->
            <xsl:apply-templates select="tr[@class='head']" mode="stylefree-layout"/>
            <!-- Reference to row template - this will be copied for each feature -->
            <xsl:variable name="row-template" select="node()[@class='template']"/>
            <!-- Rare case of for-each being more readable than templates -->
            <!--xsl:for-each select="$source//z:e"-->
            <!-- Select distinct z:e values -->
            <!--xsl:for-each select="$source//z:e[not(@ids = preceding::z:e/@ids)]"-->
            <xsl:for-each select="$source//z:e[not(. = preceding::z:e)]">
                <!-- Fill in values for row template (see xsl:template match="tr[@class='template'] below) -->
                <xsl:apply-templates select="$row-template" mode="stylefree-layout">
                    <xsl:with-param name="entity" select="current()"/>
                </xsl:apply-templates>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <!-- Row template (called for each entity in the source document) -->
    <xsl:template match="tr[@class='template']" mode="stylefree-layout">
        <xsl:param name="entity"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="node()" mode="stylefree-layout">
                <xsl:with-param name="entity" select="$entity"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!-- Entity -->
    <xsl:template match="node()[@class='entity']" mode="stylefree-layout">
        <xsl:param name="entity"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$entity" mode="entities-link-text"/>
        </xsl:copy>
    </xsl:template>

    <!-- sem -->
    <xsl:template match="node()[@class='sem']" mode="stylefree-layout">
        <xsl:param name="entity"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$entity/@sem"/>
            <xsl:apply-templates select="$entity[@provenance]" mode="source-sem"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="z:e[@provenance]" mode="source-sem">
        (<xsl:value-of select="@provenance"/>)
    </xsl:template>

    <!-- Frequency -->
    <xsl:template match="node()[@class='frequency']" mode="stylefree-layout">
        <xsl:param name="entity"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <!--xsl:value-of select="count($source//z:e[@ids = $entity/@ids])"/-->
            <xsl:value-of select="count($source//z:e[. = $entity])"/>
        </xsl:copy>
    </xsl:template>

    <!-- IDs -->
    <xsl:template match="node()[@class='ids']" mode="stylefree-layout">
        <xsl:param name="entity"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="$entity" mode="entities-link-ids"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
