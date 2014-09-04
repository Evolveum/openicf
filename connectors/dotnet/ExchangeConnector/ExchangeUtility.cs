// <copyright file="ExchangeUtility.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
// 
// The contents of this file are subject to the terms of the Common Development 
// and Distribution License("CDDL") (the "License").  You may not use this file 
// except in compliance with the License.
// 
// You can obtain a copy of the License at 
// http://IdentityConnectors.dev.java.net/legal/license.txt
// See the License for the specific language governing permissions and limitations 
// under the License. 
// 
// When distributing the Covered Code, include this CDDL Header Notice in each file
// and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// ====================
// </copyright>
// <author>Tomas Knappek</author>

namespace Org.IdentityConnectors.Exchange
{
    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Globalization;
    using System.IO;
    using System.Management.Automation.Runspaces;
    using System.Reflection;
    using Microsoft.Win32;
    using Org.IdentityConnectors.ActiveDirectory;
    using Org.IdentityConnectors.Common;
    using Org.IdentityConnectors.Framework.Common.Objects;
    using Org.IdentityConnectors.Framework.Spi;
    using System.Text.RegularExpressions;
    using System.Management.Automation;
    using System.Collections.ObjectModel;

    /// <summary>
    /// Description of ExchangeUtility.
    /// </summary>
    public sealed class ExchangeUtility
    {
        // tracing
        private static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        /// <summary>
        /// class name, used for logging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangeUtility).ToString();

        /// <summary>
        /// Embedded xml resource file containg the object class definitions
        /// </summary>
        private const string FileObjectClassDef = "Org.IdentityConnectors.Exchange.ObjectClasses.xml";


        /// <summary>
        /// Prevents a default instance of the <see cref="ExchangeUtility" /> class from being created. 
        /// </summary>
        private ExchangeUtility()
        {
        }

        /// <summary>
        /// Creates Exchange 2010 Assembly Resolver, <see cref="ResolveEventHandler"/>
        /// </summary>
        /// <param name="sender">The source of the event</param>
        /// <param name="args">A <see cref="System.ResolveEventArgs"/> that contains the event data</param>
        /// <returns>Assembly resolver that resolves Exchange 2010 assemblies</returns>
//        internal static Assembly AssemblyResolver2010(object sender, ResolveEventArgs args)
//        {
//            // Add path for the Exchange 2010 DLLs
//            if (args.Name.Contains("Microsoft.Exchange"))
//            {
//                string installPath = GetRegistryStringValue(Exchange2010RegKey, ExchangeRegValueName);
//                installPath += "\\bin\\" + args.Name.Split(',')[0] + ".dll";
//                return Assembly.LoadFrom(installPath);
//            }
//
//            return null;
//        }


        /// <summary>
        /// reads the object class info definitions from xml
        /// </summary>
        /// <returns>Dictionary of object classes</returns>
        //internal static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfo()
        //{
            //return CommonUtils.GetOCInfoFromFile(FileObjectClassDef);
        //}

        internal static Command GetCommand(PSExchangeConnector.CommandInfo cmdInfo, ExchangeConfiguration config)
        {
            return GetCommand(cmdInfo, null, null, config);
        }

        internal static Command GetCommand(PSExchangeConnector.CommandInfo cmdInfo, Uid uidAttribute, ExchangeConfiguration config)
        {
            return GetCommand(cmdInfo, null, uidAttribute, config);
        }

        internal static Command GetCommand(PSExchangeConnector.CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes, ExchangeConfiguration config)
        {
            return GetCommand(cmdInfo, attributes, ConnectorAttributeUtil.GetUidAttribute(attributes), config);
        }

        /// <summary>
        /// Creates command based on the commanf info, reading the calues from attributes
        /// </summary>
        /// <param name="cmdInfo">Command defition</param>
        /// <param name="attributes">Attribute values - UID in these is ignored! It should be passed as a separate parameter</param>
        /// <param name="config">Configuration object</param>
        /// <returns>
        /// Ready to execute Command
        /// </returns>
        /// <exception cref="ArgumentNullException">if some of the param is null</exception>
        internal static Command GetCommand(PSExchangeConnector.CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes, Uid uidAttribute, ExchangeConfiguration config)
        {
            bool nonEmptyCommand = false;

            Assertions.NullCheck(cmdInfo, "cmdInfo");
            
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "GetCommand: cmdInfo name = {0}", cmdInfo.Name);

            // create command
            Command cmd = new Command(cmdInfo.Name);

            // map name attribute, if mapping specified
            if (!string.IsNullOrEmpty(cmdInfo.NameParameter))
            {                
                object val = GetAttValue(Name.NAME, attributes);
                if (val != null)
                {
                    cmd.Parameters.Add(cmdInfo.NameParameter, val);
                }
            }

            if (!string.IsNullOrEmpty(cmdInfo.UidParameter))
            {
                if (uidAttribute != null && uidAttribute.GetUidValue() != null)
                {
                    cmd.Parameters.Add(cmdInfo.UidParameter, uidAttribute.GetUidValue());
                }
            }

            if (cmdInfo.UsesConfirm)
            {
                cmd.Parameters.Add("confirm", false);
            }

            if (cmdInfo.UsesDomainController)
            {
                cmd.Parameters.Add("DomainController", ActiveDirectoryUtils.GetDomainControllerName(config));
            }

            // TODO check this only for user-related operations
            bool emailAddressesPresent = GetAttValues(ExchangeConnectorAttributes.AttEmailAddresses, attributes) != null;
            bool primarySmtpAddressPresent = GetAttValues(ExchangeConnectorAttributes.AttPrimarySmtpAddress, attributes) != null;

            if (emailAddressesPresent && primarySmtpAddressPresent) {
                throw new ArgumentException(ExchangeConnectorAttributes.AttEmailAddresses + " and " + ExchangeConnectorAttributes.AttPrimarySmtpAddress + " cannot be both set.");
            }

            if (attributes != null) {

                foreach (string attName in cmdInfo.Parameters) {

                    object valueToSet = null;

                    ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);
                    if (attribute != null) {
                        if (attribute.Value != null && attribute.Value.Count > 1) {
                            List<string> stringValues = new List<string>();
                            foreach (object val in attribute.Value) {
                                stringValues.Add(val.ToString());
                            }
                            valueToSet = stringValues.ToArray();
                        } else {
                            valueToSet = ConnectorAttributeUtil.GetSingleValue(attribute);
                        }
                        //if (valueToSet != null) {
                        cmd.Parameters.Add(attName, valueToSet);
                        //}
                        nonEmptyCommand = true;
                    }
                }
            }

            if (nonEmptyCommand || !cmdInfo.Name.StartsWith("Set-")) {
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "GetCommand exit: cmdInfo name = {0}", cmdInfo.Name);
                return cmd;
            } else {
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "GetCommand exit: Set-style command {0} has no real parameters, skipping its execution", cmdInfo.Name);
                return null;
            }
        }

        /// <summary>
        /// Helper method: Gets attribute value from the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attributes">collection of attribute</param>
        /// <returns>Attribute value as object, null if not found</returns>     
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static object GetAttValue(string attName, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");

            if (attributes == null)
            {
                return null;
            }

            object value = null;
            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);

            if (attribute != null)
            {
                value = ConnectorAttributeUtil.GetSingleValue(attribute) ?? string.Empty;
            }

            return value;
        }

        /// <summary>
        /// Helper method: Sets attribute value in the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attValue">attribute value (if null, we will remove the attribute from the collection)</param>
        /// <param name="attributes">collection of attribute</param>
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static void SetAttValue(string attName, object attValue, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");
            Assertions.NullCheck(attributes, "attributes");

            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);
            if (attribute != null)
            {
                attributes.Remove(attribute);
            }
            if (attValue != null)
            {
                attributes.Add(ConnectorAttributeBuilder.Build(attName, new object[] { attValue }));
            }
        }

        /// <summary>
        /// Helper method: Gets attribute values from the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attributes">collection of attribute</param>
        /// <returns>Attribute value as collection of objects, null if not found</returns>     
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static IList<object> GetAttValues(string attName, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");

            if (attributes == null)
            {
                return null;
            }

            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);

            if (attribute != null)
            {
                return attribute.Value;
            }
            else
            {
                return null;
            }
        }


        /// <summary>
        /// Helper method for filtering the specified attributes from collection of attributes
        /// </summary>
        /// <param name="attributes">Collection of attributes</param>
        /// <param name="names">Attribute names to be filtered out</param>
        /// <returns>Filtered collection of attributes</returns>
        internal static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes, IList<string> names)
        {
            Assertions.NullCheck(attributes, "attributes");
            if (names == null || names.Count == 0)
            {
                return attributes;
            }
           
            ICollection<ConnectorAttribute> filtered = new List<ConnectorAttribute>();
            foreach (ConnectorAttribute attribute in attributes)
            {
                if (!names.Contains(attribute.Name))
                {
                    filtered.Add(attribute);
                }
            }
            return filtered;
        }

        /// <summary>
        /// Helper method - Replaces specified collection Items        
        /// </summary>        
        /// <param name="col">Input <see cref="ArrayList"/> to be searched for replacement</param>
        /// <param name="map">Replace mappings</param>
        /// <returns>Replaced <see cref="ArrayList"/></returns>        
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ICollection<string> FilterReplace(ICollection<string> col, IDictionary<string, string> map)
        {
            Assertions.NullCheck(col, "col");
            Assertions.NullCheck(map, "map");

            ICollection<string> newcol = CollectionUtil.NewList(col);
            foreach (KeyValuePair<string, string> pair in map)
            {
                if (newcol.Contains(pair.Key))
                {
                    newcol.Remove(pair.Key);
                    newcol.Add(pair.Value);
                }
            }            

            return newcol;
        }

        /// <summary>
        /// Finds the attributes in connector object and rename it according to input array of names, but only
        /// if the atribute name is in attributes to get
        /// </summary>
        /// <param name="cobject">ConnectorObject which attributes should be replaced</param>
        /// <param name="attsToGet">Attributes to get list</param>
        /// <param name="map">Replace mapping</param>
        /// <returns>ConnectorObject with replaced attributes</returns>        
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ConnectorObject ConvertAdAttributesToExchange(ConnectorObject cobject, ICollection<string> attsToGet)
        {
            Assertions.NullCheck(cobject, "cobject");

            var attributes = cobject.GetAttributes();
            var builder = new ConnectorObjectBuilder();

            bool emailAddressPolicyEnabled = true;
            foreach (ConnectorAttribute attribute in attributes)
            {
                string newName;
                if (attribute.Is(ExchangeConnectorAttributes.AttMsExchPoliciesExcludedADName))
                {
                    if (attribute.Value != null && attribute.Value.Contains("{26491cfc-9e50-4857-861b-0cb8df22b5d7}"))
                    {
                        emailAddressPolicyEnabled = false;
                    }
                }
                else if (ExchangeConnectorAttributes.AttMapFromAD.TryGetValue(attribute.Name, out newName))
                {
                    var newAttribute = RenameAttribute(attribute, newName);
                    builder.AddAttribute(newAttribute);
                }
                else
                {
                    builder.AddAttribute(attribute);
                }
            }

            builder.AddAttribute(ConnectorAttributeBuilder.Build(ExchangeConnectorAttributes.AttEmailAddressPolicyEnabled, emailAddressPolicyEnabled));

            copyAttribute(builder, cobject, ExchangeConnectorAttributes.AttPrimarySmtpAddressADName, ExchangeConnectorAttributes.AttPrimarySmtpAddress);

            // derive recipient type
            long? recipientTypeDetails =
                ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttMsExchRecipientTypeDetailsADName, cobject.GetAttributes()) as long?;
            String recipientType = null;
            switch (recipientTypeDetails)
            { // see http://blogs.technet.com/b/benw/archive/2007/04/05/exchange-2007-and-recipient-type-details.aspx

                case 1: recipientType = ExchangeConnectorAttributes.RcptTypeMailBox; break;
                case 128: recipientType = ExchangeConnectorAttributes.RcptTypeMailUser; break;

                case null:          // we are dealing with user accounts, so we can assume that an account without Exchange information is an ordinary User
                case 65536: recipientType = ExchangeConnectorAttributes.RcptTypeUser; break;
            }
            if (recipientType != null)
            {
                builder.AddAttribute(ConnectorAttributeBuilder.Build(ExchangeConnectorAttributes.AttRecipientType, new string[] { recipientType }));
            }
            else
            {
                LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Unknown recipientTypeDetails: {0} ({1})", recipientTypeDetails,
                    ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttMsExchRecipientTypeDetailsADName, cobject.GetAttributes()));
            }

            builder.ObjectClass = cobject.ObjectClass;
            builder.SetName(cobject.Name);
            builder.SetUid(cobject.Uid);
            return builder.Build();
        }

        private static void copyAttribute(ConnectorObjectBuilder builder, ConnectorObject cobject, string src, string dest)
        {
            ConnectorAttribute srcAttr = cobject.GetAttributeByName(src);
            if (srcAttr != null)
            {
                builder.AddAttribute(RenameAttribute(srcAttr, dest));
            }
        }

        /// <summary>
        /// Renames the connector attribute to new name
        /// </summary>
        /// <param name="cattribute">ConnectorAttribute to be renamed</param>
        /// <param name="newName">New attribute name</param>
        /// <returns>Renamed ConnectorAttribute</returns>
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ConnectorAttribute RenameAttribute(ConnectorAttribute cattribute, string newName)
        {
            Assertions.NullCheck(cattribute, "cattribute");
            Assertions.NullCheck(newName, "newName");

            var attBuilder = new ConnectorAttributeBuilder();
            attBuilder.AddValue(cattribute.Value);
            attBuilder.Name = newName;
            return attBuilder.Build();
        }

        /// <summary>
        /// Localized null check
        /// </summary>
        /// <param name="obj">Object to be check for null</param>
        /// <param name="param">Parameter name to be used in exception message</param>
        /// <param name="config">Configuration used for localization purposes</param>
        /// <exception cref="ArgumentNullException">If the passed object is null</exception>
        internal static void NullCheck(object obj, string param, Configuration config)
        {
            if (obj == null)
            {
                throw new ArgumentNullException(config.ConnectorMessages.Format(
                            "ex_argument_null", "The Argument [{0}] can't be null", param));
            }
        }

        /// <summary>
        /// Builds new Operation options and add the specified attribute names as 
        /// AttributesToGet (add to existing AttributesToGet)
        /// </summary>
        /// <param name="options">Existing Operation Options</param>
        /// <param name="attNames">attribute names to be add to AttributeToGet</param>
        /// <returns>New Operation Options</returns>
        internal static OperationOptions AddAttributeToOptions(OperationOptions options, params string[] attNames)
        {
            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder(options);
            List<string> attsToGet = new List<string>();
            if (options.AttributesToGet != null)
            {
                attsToGet.AddRange(options.AttributesToGet);
            }

            foreach (string attName in attNames)
            {
                attsToGet.Add(attName);
            }

            optionsBuilder.AttributesToGet = attsToGet.ToArray();
            return optionsBuilder.Build();
        }

        /// <summary>
        /// helper method to filter out all attributes used in ExchangeConnector only
        /// </summary>
        /// <param name="attributes">Connector attributes</param>
        /// <param name="cmdInfos">CommandInfo whose parameters will be used and filtered out from attributes</param>
        /// <returns>
        /// Filtered connector attributes
        /// </returns>
        internal static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes, params PSExchangeConnector.CommandInfo[] cmdInfos)
        {
            IList<string> attsToRemove = new List<string> { ExchangeConnectorAttributes.AttRecipientType };
            CollectionUtil.AddAll(attsToRemove, ExchangeConnectorAttributes.AttMap2AD.Keys);
            if (cmdInfos != null)
            {
                foreach (PSExchangeConnector.CommandInfo cmdInfo in cmdInfos)
                {
                    if (cmdInfo != null)
                    {
                        CollectionUtil.AddAll(attsToRemove, cmdInfo.Parameters);
                    }
                }
            }
            return ExchangeUtility.FilterOut(attributes, attsToRemove);
        }

        /// <summary>
        /// This method tries to get name and value from <see cref="PSMemberInfo"/> and
        /// creates <see cref="ConnectorAttribute"/> out of it
        /// </summary>
        /// <param name="info">PSMemberInfo to get the data from</param>
        /// <returns>Created ConnectorAttribute or null if not possible to create it</returns>
        internal static ConnectorAttribute GetAsAttribute(PSMemberInfo info)
        {
            Assertions.NullCheck(info, "param");
            if (info.Value != null)
            {
                string value = info.Value.ToString();

                // TODO: add type recognition, currently only string is supported
                if (value != info.Value.GetType().ToString() && !string.IsNullOrEmpty(value))
                {
                    return ConnectorAttributeBuilder.Build(info.Name, value);
                }
            }

            return null;
        }



    }
}
