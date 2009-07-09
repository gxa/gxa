<?xml version="1.0" encoding="utf-8"?>
<!--
/**
 * Templates for handling entities, for example:
 * <z:e sem="chebi" ids="17883">hydrochloric acid</z:e>
 *
 * @author  Antony Quinn
 * @version $Id$
 *
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:z="http://www.ebi.ac.uk/Rebholz/">

    <xsl:include href="urls.xsl"/>

    <!-- Create HTML links based on node text and @sem -->
    <xsl:template match="z:e" mode="entities-link-text">
        <xsl:call-template name="urls-create-link">
            <xsl:with-param name="database" select="@sem"/>
            <xsl:with-param name="query"    select="."/>
            <xsl:with-param name="type"     select="'text'"/>
            <xsl:with-param name="title"    select="@provenance"/>
        </xsl:call-template>
    </xsl:template>

    <!-- Create HTML links based on @ids and @sem -->
    <xsl:template match="z:e[not(@provenance)]" mode="entities-link-ids">
        <xsl:call-template name="urls-create-links">
            <xsl:with-param name="database"  select="@sem"/>
            <xsl:with-param name="query"     select="@ids"/>
        </xsl:call-template>
    </xsl:template>

    <!-- Create HTML links based on @ontIDs and @sem -->
    <xsl:template match="z:e[@provenance and @ontIDs]" mode="entities-link-ids">
        <xsl:call-template name="urls-create-links">
            <xsl:with-param name="database" select="substring-before(@ontIDs, ':')"/>
            <xsl:with-param name="query"    select="substring-after(@ontIDs, ':')"/>
            <xsl:with-param name="prefix"   select="concat(substring-before(@ontIDs, ':'), ':')"/>
        </xsl:call-template>
    </xsl:template>

   <xsl:template match="z:e" mode="entities-term-context">
       <!--Parent:  <xsl:value-of select="name(parent::node())" /> = <xsl:value-of select="parent::text()" />-->
       <xsl:apply-templates select="parent::node()" mode="string-concat-siblings">
           <xsl:with-param name="stop-node-id" select="generate-id()"/>
       </xsl:apply-templates>
       <!--Sibling: <xsl:value-of select="name(preceding-sibling::node())" /> = <xsl:value-of select="preceding-sibling::text()" />-->
        <!-- TODO: replace with span + class -->
       <!--xsl:value-of select="substring(preceding-sibling::text(), string-length(preceding-sibling::text()) - 20)" />&#160;
       <b><xsl:value-of select="." /></b>
       <xsl:value-of select="substring(following-sibling::text(), 1, 20)" /-->
   </xsl:template>

    <!-- TODO: move to strings.xsl -->
    <xsl:template match="node()" mode="string-concat-siblings">
        <xsl:param name="stop-node-id"/>
        <xsl:variable name="id" select="generate-id()"/>
        <xsl:if test="not($id = $stop-node-id)">
            <!-- Only return text nodes -->
            <xsl:if test="name(.) = ''">
                <xsl:value-of select="."/>
            </xsl:if>
            <xsl:apply-templates select="descendant::node()" mode="string-concat-siblings">
               <xsl:with-param name="stop-node-id" select="$stop-node-id"/>
           </xsl:apply-templates>
        </xsl:if>
        <!--xsl:param name="stop-node-id"/>
        <xsl:variable name="id" select="generate-id()"/>
        <br/>
        Stop Node ID: <xsl:value-of select="$stop-node-id"/><br/>
        Node ID:      <xsl:value-of select="$id"/>
        <xsl:if test="not($id = $stop-node-id)">
            <xsl:if test="name(.) = ''">
                <br/>
                <xsl:value-of select="."/>
                [<xsl:value-of select="name(.)" />]
                <br/>
            </xsl:if>
            <xsl:apply-templates select="descendant::node()" mode="string-concat-siblings">
               <xsl:with-param name="stop-node-id" select="$stop-node-id"/>
           </xsl:apply-templates>
        </xsl:if-->
    </xsl:template>

    <!-- Create HTML links based on @ontIDs and @sem -->
    <xsl:template match="z:e[@provenance and not(@ontIDs)]" mode="entities-link-ids">
        <!-- Do nothing -->
    </xsl:template>

</xsl:stylesheet>
