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
 * Logic for dsn.html.
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

    <xsl:include href="stylefree.xsl"/>

    <xsl:output method="html"
                indent="yes"
                media-type="text/html"
                encoding="iso-8859-1"
                omit-xml-declaration="yes"
                standalone="yes"/>

    <xsl:param name="template-uri" select="'dsn.html'"/>

    <xsl:param name="resource-uri" select="''"/>

    <!-- Source XML document (DSN XML) -->
    <xsl:variable name="source" select="/"/>

    <!-- HTML template -->
    <xsl:variable name="layout" select="document($template-uri)"/>

    <!-- Start matching nodes in the HTML template -->
    <xsl:template match="/">
        <xsl:apply-templates select="$layout/html" mode="stylefree-layout"/>
    </xsl:template>

    <xsl:template match="table[@class='sortable']" mode="stylefree-layout">
        <xsl:copy>
            <!-- Table tag attributes -->
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <!-- Header row (column titles) -->
            <xsl:apply-templates select="tr[@class='head']" mode="stylefree-layout"/>
            <!-- Reference to row template - this will be copied for each feature -->
            <xsl:variable name="row-template" select="node()[@class='template']"/>
            <!-- Rare case of for-each being more readable than templates -->
            <xsl:for-each select="$source/DASDSN/DSN">
                <!-- Fill in values for row template (see xsl:template match="tr[@class='template'] below) -->
                <xsl:apply-templates select="$row-template" mode="stylefree-layout">
                    <xsl:with-param name="dsn" select="current()"/>
                </xsl:apply-templates>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <!-- Row template (called for each DSN in the source document) -->
    <xsl:template match="tr[@class='template']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:apply-templates select="node()" mode="stylefree-layout">
                <xsl:with-param name="dsn" select="$dsn"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!-- Short name -->
    <xsl:template match="node()[@class='short-name']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$dsn/SOURCE/@id"/>
        </xsl:copy>
    </xsl:template>

    <!-- Full name -->
    <xsl:template match="node()[@class='full-name']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$dsn/SOURCE/text()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Version -->
    <xsl:template match="node()[@class='version']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$dsn/SOURCE/@version"/>
        </xsl:copy>
    </xsl:template>

    <!-- Mapmaster -->
    <xsl:template match="node()[@class='mapmaster']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$dsn/MAPMASTER/@version"/>
            <a href="{$dsn/MAPMASTER}/types">
                <xsl:value-of select="$dsn/MAPMASTER"/>
            </a>
        </xsl:copy>
    </xsl:template>

    <!-- Description -->
    <xsl:template match="node()[@class='description']" mode="stylefree-layout">
        <xsl:param name="dsn"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="stylefree-layout"/>
            <xsl:value-of select="$dsn/DESCRIPTION"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()" mode="stylefree-layout">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="stylefree-layout"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
