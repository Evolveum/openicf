# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2014 ForgeRock AS. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# http://forgerock.org/license/CDDLv1.0.html
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at http://forgerock.org/license/CDDLv1.0.html
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# " Portions Copyrighted [year] [name of copyright owner]"
#
# @author Gael Allioux <gael.allioux@forgerock.com>
#
#REQUIRES -Version 2.0

<#  
.SYNOPSIS  
    This is a sample Search script    
    
.DESCRIPTION
	The connector injects the following variables to the script:
	 - <prefix>.Configuration : handler to the connector's configuration object
	 - <prefix>.ObjectClass: The ObjectClass object(__ACCOUNT__ / __GROUP__ / other)
	 - <prefix>.Operation: an OperationType describing the action ("SEARCH" here)
	 - <prefix>.Options: a handler to the OperationOptions Map
	 - <prefix>.Query: a handler to the native query

.RETURNS
	Use <prefix>.Result.Complete() and <prefix>.Result.Process() method
  
.NOTES  
    File Name      : Search.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2
    Copyright 2014 - ForgeRock AS    

.LINK  
    Script posted over:  
    http://openicf.forgerock.org

#>

$attrUtil = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]
# Always put code in try/catch statement and make sure exceptions are rethrown to connector
try
{
	switch ($Connector.ObjectClass.Type)
	{
		"__ACCOUNT__"
		{
			$resultSet = @()
			if ($Connector.Query -ne $null)
			{
				$resultSet = Search-ConnectorObjectCache -ObjectClass $Connector.ObjectClass -Query $Connector.Query -SortKeys $Connector.Options.SortKeys
			}
			else
			{
				$resultSet = Search-ConnectorObjectCache -ObjectClass $Connector.ObjectClass -SortKeys $Connector.Options.SortKeys
			}
			
			if ($Connector.Options.PageSize -ne $null)
			{
				$pagedResultsCookie = $Connector.Options.PagedResultsCookie
				$currentPagedResultsCookie = $Connector.Options.PagedResultsCookie
				
				$pagedResultsOffset = 0
				if ($Connector.Options.PagedResultsOffset -ne $null)
				{
					$pagedResultsOffset = $Connector.Options.PagedResultsOffset
				}
				
				$pageSize = $Connector.Options.PageSize
				$index = 0
				$pageStartIndex = 0
				if ($Connector.Options.PagedResultsCookie -ne $null)
				{
					$pageStartIndex = -1
				}
				$handled = 0
				
				foreach($entry in $resultSet)
				{
					if(($pageStartIndex -lt 0) -and ($pagedResultsCookie -eq $entry.Name.GetNameValue()))
					{
						$pageStartIndex = $index + 1
					}
					
					if(($pageStartIndex -lt 0) -or ($index -lt $pageStartIndex))
					{
						$index++
						continue
					}
					
					if($handled -ge $pageSize)
					{
						break
					}
					
					if($index -ge $pagedResultsOffset + $pageStartIndex)
					{
						if($Connector.Result.Process($entry) )
						{
							$handled++
							$currentPagedResultsCookie = $entry.Name.GetNameValue()
						}
						else
						{
							break
						}
					}
					$index++
				}
				
				if($index -eq @($resultSet).Length)
				{
					$currentPagedResultsCookie = $null
				}
				$length = @($resultSet).Length - $index
				$complete = New-Object Org.IdentityConnectors.Framework.Common.Objects.SearchResult($currentPagedResultsCookie, $length) 
				$Connector.Result.Complete($complete)
			}
			else
			{
				foreach($res in $resultSet)
				{
					if(! $Connector.Result.Process($res))
					{
						break
					}
				}
			}
		}
		"__GROUP__"
		{
			return
		}
		"__TEST__"
		{
		#write-verbose -verbose ("Query type {0}" -f (Get-ConnectorQueryType $Connector.Query))
			$attrToGet = @{}
			if($Connector.Options.AttributesToGet -ne $null)
			{
				foreach($a in $Connector.Options.AttributesToGet)
				{
					$attrToGet.Add($a,$true)
				}
			}
			if ($Connector.Query -ne $null)
			{
				if($Connector.Query.GetType().Equals([Org.IdentityConnectors.Framework.Common.Objects.Filters.EqualsFilter]))
				{
					#Let's assume this is exact query on UID
					if($Connector.Query.GetAttribute().Name -eq "__UID__")
					{
						$attr = $Connector.Query.GetAttribute()
						$uid = $attrUtil::GetSingleValue($attr)
						
						$template = Get-ConnectorObjectTemplate
						$entry = @{"__UID__"= $uid; "__NAME__"= $uid}
						foreach($key in $template.Keys)
						{
							if($attrToGet.ContainsKey($key))
							{
								$entry.Add($key, $template[$key])
							}
						}
						if (!$Connector.Result.Process($entry))
						{
							break;
						}
					}
				}
			}
			else
			{
				$template = Get-ConnectorObjectTemplate
				foreach($i in (0..9))
				{
					$uid = "UID{0:d2}" -f $i
					$id = "TEST{0:d2}" -f $i
					$entry = @{"__UID__"= $uid; "__NAME__"= $id}
					foreach($key in $template.Keys)
					{
						if($attrToGet.ContainsKey($key))
						{
							$entry.Add($key, $template[$key])
						}
					}
					#Write-Verbose -verbose "Processing $entry" 
					if (!$Connector.Result.Process($entry))
					{
						break;
					}
				}
			}
		}
		"__SAMPLE__"
		{
			$entry1 = @{"__UID__" = "12"; "__NAME__" = "12"; "surName" = "Foo";
						"lastName" = "Bar"; "groups" = @("Group1","Group2"); "active" = $true
						}
			$Connector.Result.Process($entry1)
			$entry2 = @{"__UID__" = "13"; "__NAME__" = "13"; "surName" = "Foo"; 
						"lastName" = "Bar"; "groups" = @("Group1","Group2"); "active" = $true;
						"emails" = @{"address" = "foo@example.com"; "type" = "home"; "customeType" = ""; "primary" = $true}
						}
			$Connector.Result.Process($entry2)
			$Conector.Result.Complete
		}
		"__EMPTY__" 
		{
			$Connector.Result.Complete
		}
		default
		{	
			throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")
		}
	}
}
catch #Rethrow the original exception
{
	throw
}