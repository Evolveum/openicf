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
    This is a sample Test script     
	
.DESCRIPTION
	Parameters:
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: An OperationType String correponding to the action ("TEST" here)
	
.RETURNS 
	Nothing.
	Throw any exception to make the test fail.
	
.NOTES  
    File Name      : Test.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2
    Copyright 2014 - ForgeRock AS    
.LINK  
    Script posted over:  
    http://openicf.forgerock.org

#>

# Always put code in try/catch statement and make sure exceptions are rethrown to connector
try
{
 Write-Verbose -verbose "This is Test Script"
 if ($Connector.Operation -eq "TEST")
 {
	throw New-Object System.MissingFieldException("Test Failed")
}
 else
 {
 	$ex = New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectionFailedException
 	throw $ex
 }
}
catch #Re-throw the original exception
{
	throw
}
