<?xml version="1.0" encoding="UTF-8"?>
<!-- 
 * Copyright (C) 2016 Dienst voor het kadaster en de openbare registers
 * 
 * This file is part of Imvertor.
 *
 * Imvertor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imvertor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Imvertor.  If not, see <http://www.gnu.org/licenses/>.
-->
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    
    xmlns:imvert="http://www.imvertor.org/schema/system"
    xmlns:ext="http://www.imvertor.org/xsl/extensions"
    xmlns:imf="http://www.imvertor.org/xsl/functions"
    
    exclude-result-prefixes="#all" 
    version="2.0">

    <!-- 
        Validation of Kadaster/KING rules. 
        
        Called from within kadaster/KING validation XSLTs.
    -->
    
    <!-- 
        attribute validation 
    -->
    <xsl:template match="imvert:attribute" priority="30">
        <!-- setup -->
        <xsl:variable name="this" select="."/>
        <xsl:variable name="class" select="../.."/>
        <xsl:variable name="defining-class" select="if (imvert:type-id) then imf:get-construct-by-id(imvert:type-id) else ()"/>
        
        <xsl:variable name="is-designated-datatype" select="$defining-class/imvert:stereotype = imf:get-config-stereotypes(('stereotype-name-datatype','stereotype-name-complextype'))"/>
        <xsl:variable name="is-designated-enumeration" select="$defining-class/imvert:stereotype = imf:get-config-stereotypes(('stereotype-name-enumeration','stereotype-name-codelist'))"/>
        <xsl:variable name="is-designated-referentielijst" select="$defining-class/imvert:stereotype = imf:get-config-stereotypes(('stereotype-name-referentielijst'))"/>
        <xsl:variable name="is-designated-interface" select="$defining-class/imvert:stereotype = imf:get-config-stereotypes(('stereotype-name-interface'))"/>
        <xsl:variable name="is-designated-union" select="$defining-class/imvert:stereotype = imf:get-config-stereotypes(('stereotype-name-union'))"/>
        <xsl:variable name="is-datatyped" select="
            $is-designated-datatype or 
            $is-designated-enumeration or 
            $is-designated-referentielijst or 
            $is-designated-interface or 
            $is-designated-union"/>
        
        <!-- Jira IM-420 -->
        <xsl:sequence select="imf:report-warning(., 
            not($is-datatyped or empty($defining-class)), 
            'Attribute type of [1] must be a datatype, but is not.', ($this/imvert:stereotype))"/>
        
        <xsl:next-match/>
    </xsl:template>
    
</xsl:stylesheet>