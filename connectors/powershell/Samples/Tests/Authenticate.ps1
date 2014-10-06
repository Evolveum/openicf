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
    This is a sample Authenticate script
	
.DESCRIPTION

.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: an OperationType correponding to the action ("AUTHENTICATE" here)
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Username: Usename String
	- <prefix>.Password: clear text Password String
	
.RETURNS
	Must return the user unique ID (__UID__).
	To do so, set the <prefix>.Result.Uid property
	
.NOTES  
    File Name      : Authenticate.ps1  
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
switch ($Connector.ObjectClass.Type)
{
	"__ACCOUNT__" {throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")}
	"__GROUP__" {throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")}
	"__ALL__" {Write-Error "ICF Framework MUST REJECT this"}
	"__TEST__" {
		switch ($Connector.Username)
		{
			"TEST1" {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorSecurityException}
			"TEST2" {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidCredentialException}
			"TEST3" {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidPasswordException}
			"TEST4" {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.PermissionDeniedException}
			"TEST5" {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.PasswordExpiredException}
			"TESTOK1" 
			{
				if ( $Connector.Password -eq "Passw0rd")
				{
					$Connector.Result.Uid = $Connector.Username
				}
				else
				{
					throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidPasswordException
				}
			}
			"TESTOK2"
			{
				if ( $Connector.Password -eq "")
				{
					$Connector.Result.Uid = New-Object Org.IdentityConnectors.Framework.Common.Objects.Uid($Connector.Username)
				}
				else 
				{
					throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("The password must be empty in this test")
				}
			}
			default {throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.UnknownUidException}
		}
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
catch #Re-throw the original exception
{
	throw
}