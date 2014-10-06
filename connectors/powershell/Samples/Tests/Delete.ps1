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
    This is a sample Delete script
    
.DESCRIPTION
	Parameters:
	By default, the connector injects the "Connector" variable into the script.
	This prefix can be modified in configuration if needed.
	This variable has the following properties:
	- <prefix>.Configuration : Connector's configuration object
	- <prefix>.Operation: an OperationType corresponding to the action ("DELETE" here)
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Uid: The Uid of the object to delete
	- <prefix>.Options: a handler to the OperationOptions Map
	
.RETURNS
	Nothing.
	
.NOTES  
    File Name      : Delete.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2
    Copyright 2014 - ForgeRock AS    

.LINK  
    Script posted over:  
    http://openicf.forgerock.org

.EXAMPLE  
    Example 1     
#>

# Always put code in try/catch statement and make sure exceptions are re-thrown to connector
try
{
if ($Connector.Operation -eq "DELETE")
{
	switch ($Connector.ObjectClass.Type)
	{
		"__ACCOUNT__" 	{Remove-ConnectorObjectCache -ObjectClass $Connector.ObjectClass -Uid $Connector.Uid}
		"__GROUP__" 	{Remove-ConnectorObjectCache -ObjectClass $Connector.ObjectClass -Uid $Connector.Uid}
		"__ALL__" 		{throw "ICF Framework must reject this"}
		"__TEST__" 		{Exception-Test -Operation $Connector.Operation -ObjectClass $Connector.ObjectClass -Uid $Connector.Uid -Options $Connector.Options}
		"__SAMPLE__" 	{throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")}
		default 		{throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")}
	}
}
}
catch #Re-throw the original exception
{
	throw
}