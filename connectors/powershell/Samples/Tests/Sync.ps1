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
    This is a sample Sync script      
	
.DESCRIPTION
	By default, the connector injects the "Connector" variable into the script.
	This prefix can be modified in configuration if needed.
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Operation: an OperationType describing the action ("SEARCH" here)
	- <prefix>.Options: a handler to the OperationOptions Map
	- <prefix>.Token: The sync token value

.RETURNS
	if action = "GET_LATEST_SYNC_TOKEN", it must return an object representing the last known
    sync token for the corresponding ObjectClass
	
	if action = "SYNC":
    Call Connector.Results.Process(Hashtable) describing one update:
    Hashtable should look like the following:

   {
   "SyncToken" = <Object> token of the object that changed(could be Integer, Date, String) [!! could be null]
   "DeltaType" = <String> ("CREATE|UPDATE|CREATE_OR_UPDATE"|"DELETE"), the type of change that occurred
   "PreviousUid" = <String>, the Uid of the object before the change (This is for rename ops)
   "Uid"
   "Object" =  <Hashtable> The object that has changed
   }
  
.NOTES  
    File Name      : Sync.ps1  
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
	if ($Connector.Operation -eq "GET_LATEST_SYNC_TOKEN")
	{
		switch ($Connector.ObjectClass.Type)
		{
			"__ACCOUNT__" 	{$Connector.Result.SyncToken = 17}
			"__GROUP__" 	{$Connector.Result.SyncToken = 16}
			"__ALL__" 		{$Connector.Result.SyncToken = 17}
			"__TEST__" 		{$Connector.Result.SyncToken = 0}
			"__SAMPLE__" 
			{
				#$a = Get-Date
				#$token = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncToken($a.ToUniversalTime().ToString());
				#$Connector.Result.SyncToken = $token;
				$Connector.Result.SyncToken = "ANY OBJECT"
			}
			default 
			{
				throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.ObjectClass.Type) is not supported")
			}
		}
	}
	elseif ($Connector.Operation -eq "SYNC")
	{
		if (($Connector.ObjectClass.Type -eq "__ACCOUNT__") -or ($Connector.ObjectClass.Type -eq "__TEST__"))
		{
			$template = Get-ConnectorObjectTemplate
			switch($Connector.Token)
			{
				0 # CREATE
				{
					$object = @{"__NAME__" = "Foo"; "__UID__" = "001"}
					foreach($key in $template.Keys)
					{
						$object.Add($key, $template[$key])
					}
					$result = @{"SyncToken" = 1; "DeltaType" = "CREATE"; "Uid" = "001"; "Object" = $object}
					$Connector.Result.Process($result)
					$Connector.Result.Complete(1)
				}
				1 # UPDATE
				{
					$object = @{"__NAME__"= "Foo"; "__UID__" = "001"}
					foreach($key in $template.Keys)
					{
						$object.Add($key, $template[$key])
					}
					$result = @{"SyncToken" = 2; "DeltaType" = "UPDATE"; "Uid" = "001"; "Object" = $object}
					$Connector.Result.Process($result)
					$Connector.Result.Complete(2)
				}
				2 # CREATE_OR_UPDATE
				{
					$object = @{"__NAME__"= "Foo"; "__UID__" = "001"}
					foreach($key in $template.Keys)
					{
						$object.Add($key, $template[$key])
					}
					$result = @{"SyncToken" = 3; "DeltaType" = "CREATE_OR_UPDATE"; "Uid" = "001"; "Object" = $object} 
					$Connector.Result.Process($result)
					$Connector.Result.Complete(3)
				}
				3 # RENAME
				{
					$object = @{"__NAME__"= "Foo"; "__UID__" = "002"}
					foreach($key in $template.Keys)
					{
						$object.Add($key, $template[$key])
					}
					$result = @{"SyncToken" = 4; "DeltaType" = "UPDATE"; "PreviousUid" = "001"; "Uid" = "002"; "Object" = $object} 
					$Connector.Result.Process($result)
					$Connector.Result.Complete(4)
				}
				4 # DELETE
				{
					$object = @{"__NAME__"= "Foo"; "__UID__" = "002"}
					foreach($key in $template.Keys)
					{
						$object.Add($key, $template[$key])
					}
					$result = @{"SyncToken" = 5; "DeltaType" = "DELETE"; "Uid" = "002"; "Object" = $object} 
					$Connector.Result.Process($result)
					$Connector.Result.Complete(5)
				}
				{(5,6,7,8,9) -contains $_} # EMPTY/FILTERED CHANGE RANGE
				{
					Write-Verbose -verbose "Sync empty change range"
					$Connector.Result.Complete(10);
				}
				{(10..16) -contains $_}  # Multiple updates
				{
					$object1 = @{"__UID__" = "003"; "__NAME__" = "Foo"}
					$result1 = @{"SyncToken" = 10; "DeltaType" = "CREATE"; "Uid" = "003"; "Object" = $object1}
					
					$object2 = @{"__UID__" = "004"; "__NAME__" = "Foo"}
					$result2 = @{"SyncToken" = 12; "DeltaType" = "CREATE"; "Uid" = "004"; "Object" = $object2}
					
					$object3 = @{"__UID__" = "005"; "__NAME__" = "Foo"}
					$result3 = @{"SyncToken" = 14; "DeltaType" = "CREATE"; "Uid" = "005"; "Object" = $object3}
					
					$object4 = @{"__UID__" = "006"; "__NAME__" = "Foo"}
					$result4 = @{"SyncToken" = 16; "DeltaType" = "CREATE"; "Uid" = "006"; "Object" = $object4}
					
					
					foreach($result in @($result1, $result2, $result3, $result4))
					{
						$Connector.Result.Process($result)
					}
					$Connector.Result.Complete(17)
				}
				default
				{
					$Connector.Result.Complete(17)
				}
			}
		}
		elseif ($Connector.ObjectClass.Type -eq "__GROUP__")
		{
			$deltatype = [Org.IdentityConnectors.Framework.Common.Objects.SyncDeltaType]			
			for ($i=11; $i -le 16; $i = $i + 2)
			{					
				$cobld = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorObjectBuilder
				$cobld.setName("group1")
				$cobld.setUid("group1")
				$cobld.ObjectClass = [Org.IdentityConnectors.Framework.Common.Objects.ObjectClass]::GROUP
				$cobld.AddAttribute("description","This is group Foo");
				$cobld.AddAttribute("member",@("foo","bar"))
				
				$syncbld = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncDeltaBuilder
				$syncbld.DeltaType = $deltatype::CREATE_OR_UPDATE
				$syncbld.Uid =  New-Object Org.IdentityConnectors.Framework.Common.Objects.Uid("group1");
				$syncbld.Token = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncToken($i)
				$syncbld.Object = $cobld.Build()
				$Connector.Result.Process($syncbld.Build())
			}
			$Connector.Result.Complete(16)
		}
		elseif ($Connector.ObjectClass.Type -eq "__ALL__")
		{		
			$deltatype = [Org.IdentityConnectors.Framework.Common.Objects.SyncDeltaType]
			for ($i=10; $i -le 16; $i++)
			{
				$cobld = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorObjectBuilder
				if ($i % 2 -eq 0) {
	                $cobld.setUid("002")
					$cobld.setName("Foo")
					$cobld.ObjectClass = [Org.IdentityConnectors.Framework.Common.Objects.ObjectClass]::ACCOUNT
				} else {
	                $cobld.setUid("group1")
					$cobld.setName("group1")
					$cobld.ObjectClass = [Org.IdentityConnectors.Framework.Common.Objects.ObjectClass]::GROUP
	            }

				$cobld.AddAttribute("description","This is group Foo");
				$cobld.AddAttribute("member",@("foo","bar"))
						
				$syncbld = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncDeltaBuilder
				$syncbld.DeltaType = $deltatype::CREATE_OR_UPDATE
				$syncbld.Object = $cobld.Build()
				$syncbld.Token = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncToken($i)						
				$Connector.Result.Process($syncbld.Build())	
	        }
			# Test the DELETE without Connector object
			Write-Verbose -verbose "Processing entry without Connector object"
			$syncbld = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncDeltaBuilder
			$syncbld.DeltaType = $deltatype::DELETE
			$syncbld.Token = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncToken(17)
			$syncbld.Uid = "FooBar"
			$syncbld.ObjectClass = [Org.IdentityConnectors.Framework.Common.Objects.ObjectClass]::ACCOUNT
			$Connector.Result.Process($syncbld.Build())	
			
			$result = @{"SyncToken" = 18; "DeltaType" = "DELETE"; "Uid" = "Grp18"; "ObjectClass" = "__GROUP__"}
			$Connector.Result.Process($result)
			
	        $Connector.Result.Complete(19)
		}
		elseif ($Connector.ObjectClass.Type -eq "__SAMPLE__")
		{
			switch($Connector.Token)
			{
				0 # Generate Exceptions: No PreviousUid on Delete
				{
					$result = @{"SyncToken" = 21; "DeltaType" = "DELETE"; "Uid" = "002"; "PreviousUid" = "001"} 
					$Connector.Result.Process($result)
				}
				1 # Generate exceptions: Missing __NAME__ in Connector Object
				{
					$object = @{"sn" = "003"; "mail" = "foo@example.com"}
					$result = @{"SyncToken" = 22; "DeltaType" = "CREATE"; "Uid" = "003"; "Object" = $object}
					$Connector.Result.Process($result)
				}
				2 # Generate Exceptions: Bad Delta type
				{
					$object = @{"__NAME__" = "Foo"; "sn" = "003"; "mail" = "foo@example.com"}
					$result = @{"SyncToken" = 23; "DeltaType" = "Foo"; "Uid" = "003"; "Object" = $object}
					$Connector.Result.Process($result)
				}
				3 # Generate Exceptions: Missing Delta type
				{
					$object = @{"__NAME__" = "Foo"; "sn" = "003"; "mail" = "foo@example.com"}
					$result = @{"SyncToken" = 23; "Uid" = "003"; "Object" = $object}
					$Connector.Result.Process($result)
				}
			}
			$Connector.Result.Complete("SAMPLE")
		}
		else 
		{
			throw New-Object System.NotSupportedException("$($Connector.Operation) operation of type:$($Connector.objectClass.Type)")
		}
	}
}
catch #Rethrow the original exception
{
	throw
}