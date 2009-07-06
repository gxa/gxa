<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
    <xsl:output indent="no"/>
    <xsl:template match="/">
        <update>
            <xsl:apply-templates select="/response/result/doc" />
        </update>
    </xsl:template>
    <xsl:template match='/response/result/doc'>
        <add>
            <doc>
                <xsl:apply-templates select="*" />
            </doc>
        </add>
    </xsl:template>
    <xsl:template match="arr">
        <xsl:apply-templates select="*">
            <xsl:with-param name="name"><xsl:value-of select="@name" /></xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="str|long|short">
        <xsl:param name="name"><xsl:value-of select="@name" /></xsl:param>
        <xsl:element name="field">
            <xsl:attribute name="name">
                <xsl:value-of select="$name" />
            </xsl:attribute>
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>