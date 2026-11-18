<#function link url>
    <#return "{\\field{\\*\\fldinst{HYPERLINK \"" + url + "\"}}{\\fldrslt " + url + "}}">
</#function>
<#function artifactFormat p>
    <#assign url = p.url?has_content?string(link(p.url!""), "no url defined")/>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + url + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + url + ")">
    </#if>
</#function>
{\rtf1\ansi\ansicpg1252\deff0{\fonttbl{\f0\fswiss\fcharset0 ###FONT_NAME###;}}
\vieww12000\viewh15840\viewkind0
\pard\tx567\tx1134\tx1701\tx2268\fs###FONT_SIZE###\b Cryptomator is distributed under the GPLv3 License, found below. Please see the bottom of this document for any other license applicable to code used within Cryptomator.\b0\par
\par
\b\'a9 2016 \'96 ###COPYRIGHT_END_YEAR### Skymatic GmbH\b0\par
\par
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\par
\par
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.\par
\par
You should have received a copy of the GNU General Public License along with this program. If not, see ${link("http://www.gnu.org/licenses/")}.\par
\par
\b Cryptomator uses ${dependencyMap?size} third-party dependencies under the following licenses:\b0\par
<#list licenseMap as e>
<#assign license = e.getKey()/>
<#assign projects = e.getValue()/>
<#if projects?size &gt; 0>
\tab ${license}:\par
<#list projects as project>
\tab\tab - ${artifactFormat(project)}\par
</#list>
</#if>
</#list>
\par
\b Cryptomator uses other third-party assets under the following licenses:\b0\par
\tab SIL OFL 1.1 License:\par
\tab\tab - Font Awesome (5.12.0 - ${link("https://fontawesome.com/")})\par
<#if ###WITH_WINFSP###>
\par
\b Cryptomator dynamically links to third-party libraries under the following license:\b0\par
\tab Uncategorized License:\par
\tab\tab - WinFsp - Windows File System Proxy, Copyright (C) Bill Zissimopoulos (${link("https://github.com/billziss-gh/winfsp")})\par
</#if>
}
