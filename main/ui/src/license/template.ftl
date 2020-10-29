<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.

Cryptomator uses ${dependencyMap?size} third-party dependencies under the following licenses:
<#list licenseMap as e>
    <#assign license = e.getKey()/>
    <#assign projects = e.getValue()/>
    <#if projects?size &gt; 0>
        ${license}:
        <#list projects as project>
			- ${artifactFormat(project)}
        </#list>
    </#if>
</#list>

Cryptomator uses other third-party assets under the following licenses:
SIL OFL 1.1 License:
- Font Awesome 5.12.0 (https://fontawesome.com/)
