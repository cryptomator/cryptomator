<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - {{\\field{\\*\\fldinst{HYPERLINK " + (p.url!"no url defined") + "}}{\\fldrslt{" + (p.url!"no url defined") + "\\ul0\\cf0}}}}\\f0\\fs16 ) ">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - {{\\field{\\*\\fldinst{HYPERLINK " + (p.url!"no url defined") + "}}{\\fldrslt{" + (p.url!"no url defined") + "\\ul0\\cf0}}}}\\f0\\fs16 ) ">
    </#if>
</#function>
{\rtf1\ansi\ansicpg1252\deff0\nouicompat\deflang1031{\fonttbl{\f0\fnil\fcharset0 Segoe UI;}}
{\colortbl ;\red0\green0\blue255;}
\vieww12000\viewh15840\viewkind0
\pard\tx283\tx567\tx850\tx2267\tx2834\tx3401\tx3968\tx4535\tx5102\tx5669\tx6236\tx6803\b\fs16\lang7 Cryptomator is distributed under the GPLv3 License, found below. Please see the bottom of this document for any other license applicable to code used within Cryptomator.\b0\par
\par
\b\'a9 2016 \'96 2024 Skymatic GmbH \b0\par
\par
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\par
\par
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.\par
\par
You should have received a copy of the GNU General Public License along with this program. If not, see {{\field{\*\fldinst{HYPERLINK http://www.gnu.org/licenses/ }}{\fldrslt{http://www.gnu.org/licenses/\ul0\cf0}}}}\f0\fs16 .\par
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
\tab\tab - Font Awesome (5.12.0 - {{\field{\*\fldinst{HYPERLINK https://fontawesome.com/ }}{\fldrslt{https://fontawesome.com/\ul0\cf0}}}}\f0\fs16 )\b\par
}