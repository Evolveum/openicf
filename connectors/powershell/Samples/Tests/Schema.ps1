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
    This is a sample Schema script
	
.DESCRIPTION

.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: an OperationType corresponding to the action ("SCHEMA" here)
	- <prefix>.SchemaBuilder: an instance of org.identityconnectors.framework.common.objects.SchemaBuilder 
	that must be used to define the schema.
	
.RETURNS 
	Nothing. Connector will finalize the schema build.
	
.NOTES  
    File Name      : Schema.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2
    Copyright 2014 - ForgeRock AS    
	
.LINK  
    Script posted over:  
    http://openicf.forgerock.org

.EXAMPLE  
#>

# Always put code in try/catch statement and make sure exceptions are rethrown to connector
try
{

	$AttributeInfoBuilder = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder]
	$OperationOptionInfoBuilder = [Org.IdentityConnectors.Framework.Common.Objects.OperationOptionInfoBuilder]
					
	$SafeType = [Org.IdentityConnectors.Common.SafeType[Org.IdentityConnectors.Framework.Spi.Operations.SPIOperation]]
	$AuthenticateOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.AuthenticateOp])	
	$CreateOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.CreateOp])
	$DeleteOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.DeleteOp])
	$ResolveUsernameOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.ResolveUsernameOp])
	$SchemaOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.SchemaOp])
	$ScriptOnConnectorOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.ScriptOnConnectorOp])
	$ScriptOnResourceOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.ScriptOnConnectorOp])
	$SearchOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.SearchOp``1])
	$SyncOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.SyncOp])
	$TestOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.TestOp])
	$UpdateAttributeValuesOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.UpdateAttributeValuesOp])
	$UpdateOp = $SafeType::ForRawType([Org.IdentityConnectors.Framework.Spi.Operations.UpdateOp])	
	

if ($Connector.Operation -eq "SCHEMA")
 {
 	###########################
 	# __ACCOUNT__ object class
	###########################
	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__ACCOUNT__"
	$ocib.AddAttributeInfo([Org.IdentityConnectors.Framework.Common.Objects.OperationalAttributeInfos]::PASSWORD)
	$ocib.AddAttributeInfo([Org.IdentityConnectors.Framework.Common.Objects.PredefinedAttributeInfos]::DESCRIPTION)
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("groups", [string]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("userName", [string]).SetRequired($TRUE).Build())
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("email").SetRequired($TRUE).SetMultiValued($TRUE).Build())	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("__ENABLE__", [boolean]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("createDate").SetCreatable($FALSE).SetUpdateable($FALSE).Build())
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("lastModified", [long]).SetCreatable($FALSE).SetUpdateable($FALSE).SetReturnedByDefault($FALSE).Build())
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("passwordHistory", [string]).SetMultiValued($TRUE).SetReadable($FALSE).SetUpdateable($FALSE).SetReturnedByDefault($FALSE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("firstname"))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("sn"))
	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
	
	###########################
 	# __GROUP__ object class
	###########################
	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__GROUP__"

	$ocib.AddAttributeInfo([Org.IdentityConnectors.Framework.Common.Objects.PredefinedAttributeInfos]::DESCRIPTION)
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("cn", [string]).SetRequired($TRUE).SetMultiValued($TRUE).Build())	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("member", [string]).SetRequired($TRUE).SetMultiValued($TRUE).Build())	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build(), ($CreateOp, $DeleteOp, $SearchOp, $UpdateAttributeValuesOp, $UpdateOp))
	
	###########################
 	# __TEST__ object class
	###########################
	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__TEST__"
	
	# All supported attribute types
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeString",[string]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeStringMultivalue",[string]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributelongp",[long]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributelongpMultivalue",[long]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeLong",[long]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeLongMultivalue",[long]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributechar",[char]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributecharMultivalue",[char]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeCharacter",[char]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeCharacterMultivalue",[char]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributedoublep",[double]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributedoublepMultivalue",[double]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeDouble",[double]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeDoubleMultivalue",[double]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributefloatp",[float]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributefloatpMultivalue",[float]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeFloat",[float]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeFloatMultivalue",[float]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeint",[int]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeintMultivalue",[int]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeInteger",[int]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeIntegerMultivalue",[int]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributebooleanp",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributebooleanpMultivalue",[bool]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeBoolean",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeBooleanMultivalue",[bool]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributebytep",[byte]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributebytepMultivalue",[byte]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeByte",[byte]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeByteMultivalued",[byte]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeByteArray",[byte[]]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeByteArrayMultivalue",[byte[]]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeBigDecimal",[Org.IdentityConnectors.Framework.Common.Objects.BigDecimal]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeBigDecimalMultivalue",[Org.IdentityConnectors.Framework.Common.Objects.BigDecimal]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeBigInteger",[Org.IdentityConnectors.Framework.Common.Objects.BigInteger]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeBigIntegerMultivalue",[Org.IdentityConnectors.Framework.Common.Objects.BigInteger]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeGuardedByteArray",[Org.IdentityConnectors.Common.Security.GuardedByteArray]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeGuardedByteArrayMultivalue",[Org.IdentityConnectors.Common.Security.GuardedByteArray]).SetMultiValued($TRUE).Build())
	
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeGuardedString",[Org.IdentityConnectors.Common.Security.GuardedString]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeGuardedStringMultivalue",[Org.IdentityConnectors.Common.Security.GuardedString]).SetMultiValued($TRUE).Build())

	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("attributeDictionary",[System.Collections.Generic.Dictionary[object, object]]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Define("attributeDictionaryMultivalue",[System.Collections.Generic.Dictionary[object, object]]).SetMultiValued($TRUE).Build())
			
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build(), ($CreateOp, $DeleteOp, $SearchOp, $UpdateAttributeValuesOp, $UpdateOp))
	
	###########################
 	# Operation Options
	###########################

	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::Build("notify"), ($CreateOp, $DeleteOp, $UpdateAttributeValuesOp, $UpdateOp))
	
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::Build("force", [boolean]), ($DeleteOp))

	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildPagedResultsCookie(), $SearchOp)
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildPagedResultsOffset(), $SearchOp)
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildPageSize(), $SearchOp)
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildSortKeys(), $SearchOp)
	
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildRunAsUser())
	$Connector.SchemaBuilder.DefineOperationOption($OperationOptionInfoBuilder::BuildRunWithPassword())

 }
 }
 catch #Rethrow the original exception
 {
 	throw
 }