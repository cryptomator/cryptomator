<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return "{\\field{\\*\\fldinst{HYPERLINK \"" + (p.url!"no url defined") + "\"}}{\\fldrslt " + p.artifactId + "}}" + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
    <#else>
        <#return "{\\field{\\*\\fldinst{HYPERLINK \"" + (p.url!"no url defined") + "\"}}{\\fldrslt " + p.name + "}}" + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
    </#if>
</#function>
{\rtf1\ansi\ansicpg1252\cocoartf2512
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica-Bold;\f1\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\paperw11900\paperh16840\vieww12000\viewh15840\viewkind0
\deftab720
\pard\tx566\tx1133\tx1700\tx2267\tx2834\tx3401\tx3968\tx4535\tx5102\tx5669\tx6236\tx6803\pardeftab720\partightenfactor0

\f0\b\fs24 \cf0 Cryptomator is distributed under the GPLv3 License, found below. Please see the bottom of this document for any other license applicable to code used within Cryptomator.
\f1\b0 \
\

\f0\b \'a9 2016 \'96 2024 Skymatic GmbH
\f1\b0 \
\
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\
\
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.\
\
You should have received a copy of the GNU General Public License along with this program.  If not, see {\field{\*\fldinst{HYPERLINK "http://www.gnu.org/licenses/"}}{\fldrslt http://www.gnu.org/licenses/}}.\
\

\f0\b Cryptomator uses ${dependencyMap?size} third-party dependencies under the following licenses:
\f1\b0 \
<#list licenseMap as e>
<#assign license = e.getKey()/>
<#assign projects = e.getValue()/>
<#if projects?size &gt; 0>
	${license}:\
<#list projects as project>
		- ${artifactFormat(project)}\
</#list>
</#if>
</#list>
\

\f0\b Cryptomator uses other third-party assets under the following licenses:
\f1\b0 \
	SIL OFL 1.1 License:\
		- {\field{\*\fldinst{HYPERLINK "https://fontawesome.com/"}}{\fldrslt Font Awesome}} (5.12.0)\
\
}
