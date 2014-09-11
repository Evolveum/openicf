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
    This is a sample Update script        
.DESCRIPTION

.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: an OperationType correponding to the action ("UPDATE" here)
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Attributes: A collection of ConnectorAttributes to update
	- <prefix>.Uid: Corresponds to the OpenICF __UID__ attribute 
	
.RETURNS
	Must return the user unique ID (__UID__) if either UID or Revision has been modified 
	To do so, set the <prefix>.Result.Uid property with the modified Uid object

.NOTES  
    File Name      : Update.ps1  
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
if ($Connector.Operation -eq "UPDATE")
{
	switch ($Connector.ObjectClass.Type)
	{
		"__ACCOUNT__" 
		{
			$current = Get-ConnectorObjectCache $Connector.ObjectClass $Connector.Uid
			if($current -ne $null)
			{
				$cobld = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorObjectBuilder
				$cobld.setUid($Connector.Uid)
				$cobld.ObjectClass = $Connector.ObjectClass
				$cobld.AddAttributes($attrUtil::FilterUid($current.First.GetAttributes()))
				
				foreach($a in $Connector.Attributes)
				{
					if($a.Is("__NAME__"))
					{
						if(($a.Value -eq $null) -or ($a.Value.Count -eq 0))
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting non empty value")
						}
						elseif($a.Value.Count -gt 1) 
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting single value")
						}
						elseif($attrUtil::GetSingleValue($a).GetType().Equals([string]))
						{
							$cobld.setName($attrUtil::GetSingleValue($a))
						}
						else
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting String value")
						}
					}
					elseif($a.Is("userName"))
					{
						if(($a.Value -eq $null) -or ($a.Value.Count -eq 0))
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting non empty value")
						}
						elseif($a.Value.Count -gt 1) 
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting single value")
						}
						elseif($attrUtil::GetSingleValue($a).GetType().Equals([string]))
						{
							$cobld.AddAttribute("userName",$attrUtil::GetStringValue($a))
						}
						else
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting String value")
						}
					}
					elseif($a.Is("email"))
					{
						if(($a.Value -eq $null) -or ($a.Value.Count -eq 0))
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting non null value")
						}
						else
						{
							foreach($object in $a.Value)
							{
								if(!$object.GetType().Equals([string]))
								{
									throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting String value")
								}
							}
							$cobld.AddAttribute($a)
						}
					}
					elseif($a.Is("active"))
					{
						if(($a.Value -eq $null) -or ($a.Value.Count -eq 0))
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting non empty value")
						}
						elseif($a.Value.Count -gt 1) 
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting single value")
						}
						elseif($attrUtil::GetSingleValue($a).GetType().Equals([bool]))
						{
							$cobld.AddAttribute("active", $attrUtil::GetBooleanValue($a))
						}
						else
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting Boolean value")
						}
					}
					elseif($a.Is("createDate"))
					{
						throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Try update non modifiable attribute")
					}
					elseif($a.Is("lastModified"))
					{
						throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Try update non modifiable attribute")
					}
					elseif($a.Is("passwordHistory"))
					{
						throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Try update non modifiable attribute")
					}
					elseif($a.Is("surName"))
					{
						if(($a.Value -eq $null) -or ($a.Value.Count -eq 0))
						{
							$cobld.AddAttribute("surName")
						}
						elseif($a.Value.Count -gt 1) 
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting single value")
						}
						elseif($attrUtil::GetSingleValue($a).GetType().Equals([string]))
						{
							$cobld.AddAttribute("surName",$attrUtil::GetStringValue($a))
						}
						else
						{
							throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Expecting String value")
						}
					}
					else
					{
						$cobld.AddAttribute($a)
					}
				}
				$now = (Get-Date).ToString()
				$cobld.AddAttribute("lastModified", $now)
				$Connector.Result.Uid = Set-ConnectorObjectCache $cobld.Build()
			}
			else
			{
				throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.UnknownUidException($Connector.Uid, $Connector.ObjectClass)
			}
		}
		"__GROUP__" {$Connector.Result.Uid = $Connector.Uid}
		"__ALL__" {Write-Error "ICF Framework MUST REJECT this"}
		"__TEST__" 
		{
			$Connector.Result.Uid = Exception-Test -Operation $Connector.Operation -ObjectClass $Connector.ObjectClass -Uid $Connector.Uid -Options $Connector.Options
		}
		"__SAMPLE__"
		{
			throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")
		}
		default 
		{
			throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")
		}
	}
}
elseif(($Connector.Operation -eq "ADD_ATTRIBUTE_VALUES") -or ($Connector.Operation -eq "REMOVE_ATTRIBUTE_VALUES"))
{
	throw New-Object System.NotSupportedException
}
else{
	throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("updateScript can not handle operation: $($Connector.Operation)")
}
}
catch #Rethrow the original exception
{
	throw
}