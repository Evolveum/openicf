// <copyright file="ExchangeConnectorAttributes.cs" company="Sun Microsystems, Inc.">
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
// <author>Pavol Mederly</author>

using Org.IdentityConnectors.Framework.Common.Objects;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    /// <summary>
    /// Attribute meta-information, separated to its own class to be more maintainable.
    /// </summary>
    class ExchangeConnectorAttributes
    {
        /// <summary>
        /// Recipient Type attribute name
        /// </summary>
        internal const string AttRecipientType = "RecipientType";

        /// <summary>
        /// msExchRecipientDisplayType attribute - necessary for deriving RecipientType directly from AD
        /// This AD attribute is visible to clients, therefore we prepare AttInfo for this attribute.
        /// </summary>
        internal const string AttMsExchRecipientDisplayTypeADName = "msExchRecipientDisplayType";

        /// <summary>
        /// msExchRecipientTypeDetails attribute - necessary for deriving RecipientType directly from AD
        /// This AD attribute is visible to clients, therefore we prepare AttInfo for this attribute.
        /// </summary>
        internal const string AttMsExchRecipientTypeDetailsADName = "msExchRecipientTypeDetails";

        /// <summary>
        /// External Mail Address attribute name
        /// The address where e-mail is redirected when the intended recipient is unavailable.
        /// It has an AD version, but it is hidden, because the mapping between Exchange and AD version is direct.
        /// </summary>
        internal const string AttExternalEmailAddress = "ExternalEmailAddress";
        internal const string AttExternalEmailAddressADName = "targetAddress";

        /// <summary>
        /// Email Addresses attribute name
        /// All proxy addresses
        /// </summary>

        internal const string AttEmailAddresses = "EmailAddresses";
        internal const string AttEmailAddressesADName = "proxyAddresses";

        /// <summary>
        /// Controlled by recipient policy?
        /// 
        /// Mapping is non-trivial but AD version of this attribute is pretty useless 
        /// in non-windows world, so we won't expose it to outside
        /// </summary>

        internal const string AttEmailAddressPolicyEnabled = "EmailAddressPolicyEnabled";
        internal const string AttMsExchPoliciesExcludedADName = "msExchPoliciesExcluded";

        /// <summary>
        /// Primary SMTP e-mail address
        /// Its counterpart (mail) is already in AD ObjectClasses.xml file.
        /// </summary>

        internal const string AttPrimarySmtpAddress = "PrimarySmtpAddress";
        internal const string AttPrimarySmtpAddressADName = "mail";

        internal const string AttAlias = "Alias";
        internal const string AttAliasADName = "mailNickname";

        internal const string AttHiddenFromAddressListsEnabled = "HiddenFromAddressListsEnabled";
        internal const string AttHiddenFromAddressListsEnabledADName = "msExchHideFromAddressLists";

        // This attribute can be obtained only using PowerShell connection
        // (i.e. not directly from AD), so we make it not returnable by default.
        // If needed, one can use homeMDB AD attribute that seems to contain
        // DN of the database, and derive common name (which is probably what is 
        // being put into Database attribute). We plan to deal with this
        // conversion in the future.
        internal const string AttDatabase = "Database";
        internal const string AttDatabaseADName = "homeMDB";

        // Please note that the mapping of DeliverToMailboxAndForward to deliverAndRedirect is the following:
        //   TRUE maps to TRUE
        //   FALSE maps to NOT-SET
        // (as of Exchange 2010)
        internal const string AttDeliverToMailboxAndForward = "DeliverToMailboxAndForward";
        internal const string AttDeliverToMailboxAndForwardADName = "deliverAndRedirect";

        // ForwardingSmtpAddress - for more detailed description see e.g. http://ficility.net/tag/forwardingsmtpaddress/
        // In the future we could implement here also ForwardingAddress/altRecipient attribute, but working with it
        // is more complex, as it points NOT to an SMTP address but to a recipient in AD (e.g. via its DN).
        internal const string AttForwardingSmtpAddress = "ForwardingSmtpAddress";
        internal const string AttForwardingSmtpAddressADName = "msExchGenericForwardingAddress";

        // AddressBookPolicy and OfflineAddressBook
        // Beware, in AD there are stored as DNs, e.g. CN=Test OAB,CN=Offline Address Lists,CN=Address Lists Container,CN=test,CN=Microsoft Exchange,CN=Services,CN=Configuration,DC=test,DC=xyz,DC=local
        // When extracting the values to be returned to client, we leave only the Common Name.
        internal const string AttAddressBookPolicy = "AddressBookPolicy";
        internal const string AttAddressBookPolicyADName = "msExchAddressBookPolicyLink";

        internal const string AttOfflineAddressBook = "OfflineAddressBook";
        internal const string AttOfflineAddressBookADName = "msExchUseOAB";

        /// <summary>
        /// Custom attribute name prefix
        /// </summary>
        internal const string AttPrefixCustomAttribute = "CustomAttribute";
        internal const string AttPrefixCustomAttributeADName = "extensionAttribute";

        internal static IList<string> AttCustomAttributes;
        internal static IList<string> AttCustomAttributesADNames;

        internal const int NumberOfCustomAttributes = 15;

        internal const string AttPrefixExtCustomAttribute = "ExtensionCustomAttribute";
        internal const string AttPrefixExtCustomAttributeADName = "msExchExtensionCustomAttribute";

        internal static IList<string> AttExtCustomAttributes;
        internal static IList<string> AttExtCustomAttributesADNames;

        internal const int NumberOfExtCustomAttributes = 5;

        /// <summary>
        /// Deleted atrribute name
        /// </summary>
        internal const string AttIsDeleted = "isDeleted";

        /// <summary>
        /// Attribute mapping constant
        /// 
        /// The interaction between AD and Exchange part of the connector is the following:
        /// - Exchange speaks in the language of Exchange PowerShell commands (i.e. uses names like "HiddenFromAddressListsEnabled")
        /// - AD speaks in the language of Exchange AD attributes (i.e. uses names like "msExchHideFromAddressLists")
        /// 
        /// This has significant implications. Namely,
        /// 
        /// (1) When getting data from the resource (Search and Sync operations), the conversion
        ///     from AD to Exchange attributes takes place. Most of conversions consist of only
        ///     renaming the respective attribute; however, there is a couple of more complicated
        ///     ones (e.g. msExchPoliciesExcluded to EmailAddressPolicyEnabled). 
        ///     See ConvertAdAttributesToExchange method.
        ///     
        /// (2) When converting search filter, one has to convert Exchange conditions
        ///     to their AD counterparts (where such a mapping exists).
        ///     See LegacyExchangeConnectorFilterTranslator class.
        ///     
        /// (3) Before executing a query, one has to deal with "attributesToGet" option.
        ///     Currently, the only thing that has to be done here is to remove attributes
        ///     that are not understood by AD connector (namely, Database attribute).
        ///     All other attributes are returned by default, so there's no need to include
        ///     them in attributesToGet option. (TODO fix this - because a client can
        ///     include any attribute in this option).
        /// 
        /// (4) GetAdAttributesToReturn: This is a list of AD attributes that AD connector
        ///     fetches from the AD and passes to a client (or to Exchange connector).
        ///     Here should be all Exchange-related AD attributes.
        ///     
        /// </summary>
        internal static readonly IDictionary<string, string> AttMap2AD = new Dictionary<string, string> 
        {
            { AttAlias, AttAliasADName },
            { AttEmailAddresses, AttEmailAddressesADName },
            { AttExternalEmailAddress, AttExternalEmailAddressADName },
            { AttHiddenFromAddressListsEnabled, AttHiddenFromAddressListsEnabledADName },
            { AttDeliverToMailboxAndForward, AttDeliverToMailboxAndForwardADName },
            { AttForwardingSmtpAddress, AttForwardingSmtpAddressADName },
            { AttDatabase, AttDatabaseADName }
        };

        // externally-visible Exchange attributes not mentioned in AttMap2AD
        internal static readonly ISet<string> OtherExchangeAttributes = 
            new HashSet<string> 
            { 
                AttPrimarySmtpAddress,
                AttRecipientType, 
                AttEmailAddressPolicyEnabled,
                AttAddressBookPolicy,
                AttOfflineAddressBook
            };

        // these attributes should be retrieved from AD (but not sent further); note that all AD attributes mentioned in AttMap2AD are retrieved by default
        internal static readonly ISet<string> HiddenAdAttributesToRetrieve =
            new HashSet<string> 
            { 
                AttMsExchPoliciesExcludedADName
            };

        // these attributes should be retrieved from AD (and sent further); note that all AD attributes mentioned in AttMap2AD are retrieved by default
        internal static readonly ISet<string> VisibleAdAttributesToRetrieve =
            new HashSet<string> 
            { 
                AttMsExchRecipientDisplayTypeADName,
                AttMsExchRecipientTypeDetailsADName,
                AttAddressBookPolicyADName,
                AttOfflineAddressBookADName
            };

        // these "manually defined" Exchange attributes should be part of the schema (here are all except custom attributes)
        /* internal static readonly ISet<ConnectorAttributeInfo> ManualExchangeAttInfosForSchema =
            new HashSet<ConnectorAttributeInfo> 
                {
                    AttInfoAlias,
                    AttInfoPrimarySmtpAddress, 
                    AttInfoEmailAddresses,
                    AttInfoExternalEmailAddress,
                    AttInfoHiddenFromAddressListsEnabled,
                    AttInfoRecipientType, 
                    AttInfoEmailAddressPolicyEnabled,
                    AttInfoDatabase,
                    AttInfoDeliverToMailboxAndForward,
                    AttInfoForwardingSmtpAddress
                };
        */
        // these AD attributes should be part of the schema
        /*internal static readonly ISet<ConnectorAttributeInfo> ExchangeRelatedADAttInfosForSchema =
            new HashSet<ConnectorAttributeInfo> 
                {
                    AttInfoADMsExchRecipientDisplayType, 
                    AttInfoADMsExchRecipientTypeDetails,
                    AttInfoADDatabase 
                };
        */
        internal static bool IsExchangeAttribute(String attrName)
        {
            return AttMap2AD.ContainsKey(attrName) || OtherExchangeAttributes.Contains(attrName);
        }

        internal static bool IsExchangeAttribute(ConnectorAttribute attr)
        {
            return IsExchangeAttribute(attr.Name);
        }

        /// <summary>
        /// Attribute mapping constant
        /// </summary>
        internal static readonly IDictionary<string, string> AttMapFromAD;

        static ExchangeConnectorAttributes()
        {
            // creating custom attributes
            AttCustomAttributes = new List<string>(NumberOfCustomAttributes);
            AttCustomAttributesADNames = new List<string>(NumberOfCustomAttributes);
            for (int i = 1; i <= NumberOfCustomAttributes; i++)
            {
                string name = AttPrefixCustomAttribute + i;
                string adName = AttPrefixCustomAttributeADName + i;
                AttCustomAttributes.Add(name);
                AttCustomAttributesADNames.Add(adName);
                AttMap2AD.Add(name, adName);
            }

            // creating ext custom attributes
            AttExtCustomAttributes = new List<string>(NumberOfExtCustomAttributes);
            AttExtCustomAttributesADNames = new List<string>(NumberOfExtCustomAttributes);
            for (int i = 1; i <= NumberOfExtCustomAttributes; i++) {
                string name = AttPrefixExtCustomAttribute + i;
                string adName = AttPrefixExtCustomAttributeADName + i;
                AttExtCustomAttributes.Add(name);
                AttExtCustomAttributesADNames.Add(adName);
                AttMap2AD.Add(name, adName);
            }

            // creating AttMapFromAD from AttMap2AD
            AttMapFromAD = new Dictionary<string, string>();
            foreach (var item in AttMap2AD)
            {
                AttMapFromAD.Add(item.Value, item.Key);
            }
        }

        /// <summary>
        /// Recipient type attribute for Mailbox
        /// </summary>
        internal const string RcptTypeMailBox = "UserMailbox";

        /// <summary>
        /// Recipient type attribute for MailUser
        /// </summary>
        internal const string RcptTypeMailUser = "MailUser";

        /// <summary>
        /// Recipient type attribute for AD only User
        /// </summary>
        internal const string RcptTypeUser = "User";

    }
}
