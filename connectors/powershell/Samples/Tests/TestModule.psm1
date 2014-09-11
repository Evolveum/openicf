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
	
	
Function Exception-Test {
Param(
	$operation, 
	$objectClass,
	$uid,
	$options
	)
	
	$sstring = ConvertTo-SecureString -AsPlainText  -String "Passw0rd" -Force
	$password = New-Object Org.IdentityConnectors.Common.Security.GuardedString($sstring)
	
	if ($options -ne $null)
	{
		if ($options.RunAsUser -ne $null){
			if ($options.RunWithPassword -eq $null) {
				throw New-Object System.ArgumentException("Missing Run As Password")
			} elseif ($options.RunAsUser -eq "valid-session" -and $options.RunWithPassword -eq $null){
				#Use valid session ID
			} elseif ($options.RunAsUser -ne "admin" -or ! $password.Equals($options.RunWithPassword)){
				throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorSecurityException("Invalid Run As Credentials");
			}
		}
	}
	
	if ("TEST1" -eq $uid.GetUidValue()) {
		if ("CREATE" -eq $operation) {
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.AlreadyExistsException("Object already exist")
			#throw new AlreadyExistsException(
			#   	 "Object with Uid '${uid.uidValue}' and ObjectClass '${objectClass.objectClassValue}' already exists!").initUid(uid);
		} else {
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.UnknownUidException($uid, $objectClass)
			# throw new UnknownUidException(uid, objectClass);
		}
	} elseif ("TEST2" -eq $uid.GetUidValue()) {
		#ICF 1.4 Exception
		if ("DELETE" -eq $operation) {
			return $uid;
		} else {
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException
		}
	} elseif ("TEST3" -eq $uid.GetUidValue()) {
		#ICF 1.1 Exception
		if ("DELETE" -eq $operation) {
			return $uid;
		} else {
			throw New-Object System.ArgumentException
		}
	} elseif ("TEST4" -eq $uid.GetUidValue()) {
		if ("CREATE" -eq $operation) {
			throw [Org.IdentityConnectors.Framework.Common.Exceptions.RetryableException]::Wrap("Created but some attributes are not set", $uid)
			#throw RetryableException.wrap("Created but some attributes are not set, call update with new 'uid'!", uid);
		} else {
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.PreconditionFailedException
		}
	} elseif ("TEST5" -eq $uid.GetUidValue()) {
			if ("CREATE" -eq $operation) {
				return $uid;
			} else {
				throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.PreconditionRequiredException
			}
	} elseif ("TIMEOUT" -eq $uid.GetUidValue()) {
			Start-Sleep 30 
	} elseif ("TESTEX_CE" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConfigurationException(New-Object System.ArgumentException("Test Failed"));
	} elseif ("TESTEX_CB" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectionBrokenException("Example Message");
	} elseif ("TESTEX_CF" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectionFailedException("Example Message");
	} elseif ("TESTEX_C" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("Example Message");
	} elseif ("TESTEX_CIO" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorIOException("Example Message");
	} elseif ("TESTEX_OT" -eq $uid.GetUidValue()) {
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.OperationTimeoutException("Example Message");
	} elseif ("TESTEX_NPE" -eq $uid.GetUidValue()) {
		throw New-Object NullPointerException("Example Message");
	}
	return $uid;
}


Function Get-ConnectorObjectTemplate {

#[int]	32-bit signed integer
#[long]	64-bit signed integer
#[string]	Fixed-length string of Unicode characters
#[char]	A Unicode 16-bit character
#[byte]	An 8-bit unsigned character
#[bool]	Boolean True/False value
#[decimal]	An 128-bit decimal value
#[single]	Single-precision 32-bit floating point number
#[double]	Double-precision 64-bit floating point number
#[xml]	Xml object
#[array]	An array of values
#[hashtable]	Hashtable object

	$value = @{}
	$value["attributeString"] = "retipipiter"
	$value["attributeStringMultivalue"] = ("value1", "value2")
	$value["attributelongp"] = 111 -as [long]
	$value["attributelongpMultivalue"] = ((121 -as [long]), (131 -as [long]))
	$value["attributeLong"] = 14 -as [long]
	$value["attributeLongMultivalue"] = ((15 -as [long]), (16 -as [long]))
	$value["attributechar"] = 'a' -as [char]
	$value["attributecharMultivalue"] = (('b' -as [char]), ('c' -as [char]))
	$value["attributeCharacter"] = 'd' -as [char]
	$value["attributeCharacterMultivalue"] = (('e' -as [char]), ('f' -as [char]))
	$value["attributedoublep"] = 17 -as [double]
	$value["attributedoublepMultivalue"] = ((18 -as [double]), (19 -as [double])) 
	$value["attributeDouble"] = 17 -as [double]
	$value["attributeDoubleMultivalue"] = ((18 -as [double]), (19 -as [double]))
	$value["attributefloatp"] = 20 -as [float]
	$value["attributefloatpMultivalue"] = ((21 -as [float]), (22 -as [float]))
	$value["attributeFloat"] = 23 -as [float]
	$value["attributeFloatMultivalue"] = ((24 -as [float]), (25 -as [float]))
	$value["attributeint"] = 26 -as [int]
	$value["attributeintMultivalue"] = ((27 -as [int]), (28 -as [int]))
	$value["attributeInteger"] = 29 -as [int]
	$value["attributeIntegerMultivalue"] = ((30 -as [int]), (31 -as [int]))
	$value["attributebooleanp"] = $true -as [bool]
	$value["attributebooleanpMultivalue"] = (($true -as [bool]), ($false -as [bool]))
	$value["attributeBoolean"] = $true -as [Boolean]
	$value["attributeBooleanMultivalue"] = (($true -as [Boolean]), ($false -as [Boolean]))
	$value["attributebytep"] = 48 -as [byte]
	$value["attributebytepMultivalue"] = ((49 -as [byte]), (50 -as [byte]))
	$value["attributeByte"] = 51 -as [Byte]
	$value["attributeByteMultivalue"] = ((52 -as [Byte]), (53 -as [Byte]))
	$value["attributeByteArray"] = ((1 -as [byte]), (2 -as [byte])) -as [byte[]]
	$a = ((1 -as [byte]), (2 -as [byte])) -as [byte[]]
	$b = ((3 -as [byte]), (4 -as [byte])) -as [byte[]]
	$value["attributeByteArrayMultivalue"] = ($a, $b)
	
	$bigInt1 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigInteger("1")
	$bigInt0 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigInteger("0")
	$bigInt10 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigInteger("10")
	
	$value["attributeBigInteger"] = $bigInt1
	$value["attributeBigIntegerMultivalue"] = ($bigInt0, $bigInt10)
	
	$bigDec1 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigDecimal($bigInt1,0)
	$bigDec0 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigDecimal($bigInt0,0)
	$bigDec10 = New-Object Org.IdentityConnectors.Framework.Common.Objects.BigDecimal($bigInt10,0)
	
	$value["attributeBigDecimal"] = $bigDec1 -as [Org.IdentityConnectors.Framework.Common.Objects.BigDecimal]
	$value["attributeBigDecimalMultivalue"] = (($bigDec0 -as [Org.IdentityConnectors.Framework.Common.Objects.BigDecimal]), ($bigDec0 -as [Org.IdentityConnectors.Framework.Common.Objects.BigDecimal]))
	
	$gba = New-Object Org.IdentityConnectors.Common.Security.GuardedByteArray
	[System.Text.Encoding]::UTF8.GetBytes("array") | ForEach-Object {$gba.AppendByte($_)}
	$gba1 = New-Object Org.IdentityConnectors.Common.Security.GuardedByteArray
	[System.Text.Encoding]::UTF8.GetBytes("item1") | ForEach-Object {$gba.AppendByte($_)}
	$gba2 = New-Object Org.IdentityConnectors.Common.Security.GuardedByteArray
	[System.Text.Encoding]::UTF8.GetBytes("item1") | ForEach-Object {$gba.AppendByte($_)}
	
	$value["attributeGuardedByteArray"] = $gba
	$value["attributeGuardedByteArrayMultivalue"] = ($gba1, $gba2)
	
	$ssecret = ConvertTo-SecureString -String "secret" -AsPlainText -Force
	$ssecret1 = ConvertTo-SecureString -String "secret1" -AsPlainText -Force
	$ssecret2 = ConvertTo-SecureString -String "secret2" -AsPlainText -Force
	$value["attributeGuardedString"] = New-Object Org.IdentityConnectors.Common.Security.GuardedString($ssecretcret)
	
	$gs1 = New-Object Org.IdentityConnectors.Common.Security.GuardedString($ssecret1)
	$gs2 = New-Object Org.IdentityConnectors.Common.Security.GuardedString($ssecret2)
	
	$value["attributeGuardedStringMultivalue"] = ($gs1, $gs2)
	
	$value["attributeMap"] = @{"string" = "String";
								"number" = 42;
								"trueOrFalse" = $true;
								"nullValue" = $null;
								"collection" = @("item1", "item2");
								"object" = @{"key1" = "value1"; "key2" = "value2"}
								}
								
	$value["attributeMapMultivalue"] = @(@{"string" = "String";
								"number" = 42;
								"trueOrFalse" = $true;
								"nullValue" = $null;
								"collection" = @("item1", "item2");
								"object" = @{"key1" = "value1"; "key2" = "value2"}
								},
								@{"string" = "String";
								"number" = 43;
								"trueOrFalse" = $true;
								"nullValue" = $null;
								"collection" = @("item1", "item2");
								"object" = @{"key1" = "value1"; "key2" = "value2"}
								})
	
	return $value
}
	
export-modulemember -function Exception-Test, Get-ConnectorObjectTemplate