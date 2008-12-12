/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
using System.Collections.Generic;
using System.Diagnostics;
using System.Management.Automation.Runspaces;
using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using System.Collections;

namespace Org.IdentityConnectors.Exchange
{
    /// <summary>
    /// MS Exchange connector - build to have the same functionality as Exchange resource adapter
    /// 
    /// </summary>
    [ConnectorClass("connector_displayName",
                    typeof(ExchangeConfiguration),
                    MessageCatalogPath = "Org.IdentityConnectors.ActiveDirectory.Messages"
                   )]
    public class LegacyExchangeConnector : ActiveDirectoryConnector
    {
        private static readonly string CLASS = typeof(LegacyExchangeConnector).ToString();

        //hardcoded stuff
        internal const string ATT_RECIPIENT_TYPE = "RecipientType";
        internal const string ATT_EXTERNAL_MAIL = "ExternalEmailAddress";
        internal const string ATT_DATABASE = "Database";

        internal const string ATT_EXTERNAL_MAIL_AD_NAME = "targetAddress";
        internal const string ATT_DATABASE_AD_NAME = "homeMDB";

        internal static readonly string[,] ATT_MAPPING = new[,]
                                                             {
                                                                 {ATT_DATABASE, ATT_DATABASE_AD_NAME},
                                                                 {ATT_EXTERNAL_MAIL, ATT_EXTERNAL_MAIL_AD_NAME}
                                                             };

        private static readonly ConnectorAttributeInfo ATTINFO_RECIPIENT_TYPE =
            ConnectorAttributeInfoBuilder.Build(ATT_RECIPIENT_TYPE, typeof(string), true, true, true, false);

        private static readonly ConnectorAttributeInfo ATTINFO_EXTERNAL_MAIL =
            ConnectorAttributeInfoBuilder.Build(ATT_EXTERNAL_MAIL, typeof(string), false);

        private static readonly ConnectorAttributeInfo ATTINFO_DATABASE =
            ConnectorAttributeInfoBuilder.Build(ATT_DATABASE, typeof(string), false, true, true, false);

        private const string RCPT_TYPE_MAIL_BOX = "mailbox";
        private const string RCPT_TYPE_MAIL_USER = "mailuser";


        //local vars
        private ExchangeConfiguration _configuration = null;
        private RunSpaceInstance _runspace = null;
        private bool _disposed = false;


        #region CreateOp.Create implementation
        /// <summary>
        /// Implementation of CreateOp.Create
        /// </summary>
        /// <param name="oclass"></param>(oc
        /// <param name="attributes"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override Uid Create(ObjectClass oclass,
                                   ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            const string METHOD = "Create";
            Debug.WriteLine(METHOD + ":entry", CLASS);

            //get recipient type
            string rcptType = ExchangeUtils.GetAttValue(ATT_RECIPIENT_TYPE, attributes) as string;

            if (rcptType != RCPT_TYPE_MAIL_BOX && rcptType != RCPT_TYPE_MAIL_USER)
            {
                //AD account only, we do nothing
                return base.Create(oclass, attributes, options);
            }

            //first create the object in AD
            Uid uid = base.Create(oclass, FilterOut(attributes), options);

            //prepare the command
            CommandInfo cmdInfo = rcptType == RCPT_TYPE_MAIL_BOX ? CommandInfo.ENABLE_MAILBOX : CommandInfo.ENABLE_MAILUSER;
            Command cmd = ExchangeUtils.GetCommand(cmdInfo, attributes);

            try
            {
                //execute the command
                _runspace.InvokePipeline(cmd);
            }
            catch
            {
                Trace.TraceWarning("Rolling back AD create for UID: " + uid.GetUidValue());
                //rollback AD create
                try
                {
                    base.Delete(oclass, uid, options);
                }
                catch
                {
                    //ignore delete error
                    Trace.TraceWarning("Not able to rollback AD create for UID: " + uid.GetUidValue());
                }
                //rethrow original exception
                throw;
            }

            Debug.WriteLine(METHOD + ":exit", CLASS);
            return uid;
        }
        #endregion

        /// <summary>
        /// Implementation of UpdateOp.Update
        /// </summary>
        /// <param name="type"></param>
        /// <param name="oclass"></param>
        /// <param name="attributes"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override Uid Update(UpdateType type, ObjectClass oclass,
                                   ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            const string METHOD = "Update";
            Debug.WriteLine(METHOD + ":entry", CLASS);

            Assertions.NullCheck(type, "updatetype");
            Assertions.NullCheck(oclass, "oclass");
            Assertions.NullCheck(attributes, "attributes");

            //update in AD first
            Uid uid = base.Update(type, oclass, FilterOut(attributes), options);
            //get recipient type
            string rcptType = ExchangeUtils.GetAttValue(ATT_RECIPIENT_TYPE, attributes) as string;

            //update is possible for mailuser's external email only
            if (rcptType == RCPT_TYPE_MAIL_USER)
            {
                if (type == UpdateType.REPLACE)
                {
                    //get name attribute
                    string name = ExchangeUtils.GetAttValue(Name.NAME, attributes) as string;
                    if (name == null)
                    {
                        //we don't know name, but we need it - NOTE: searching for all the default attributes, we need only Name here, it can be improved
                        ConnectorObject co = ADSearchByUid(uid, oclass, null);
                        Assertions.NullCheck(co, "co");
                        //add to attributes
                        attributes.Add(co.Name);
                    }

                    Command cmd = ExchangeUtils.GetCommand(CommandInfo.SET_MAILUSER, attributes);
                    _runspace.InvokePipeline(cmd);
                }
                else
                {
                    throw new ConnectorException(string.Format("Update type [{0}] not supported", type));
                }
            }

            Debug.WriteLine(METHOD + ":exit", CLASS);
            return uid;
        }

        /// <summary>
        /// Tests if the connector is properly configured and ready
        /// </summary>
        public override void Test()
        {
            //validate the configuration first, this will check AD configuration too
            _configuration.Validate();
            //AD validation (includes configuration validation too)
            base.Test();
            //runspace check
            _runspace.Test();
        }

        /// <summary>
        /// Implementation of SynOp.Sync
        /// </summary>
        /// <param name="objClass"></param>
        /// <param name="token"></param>
        /// <param name="handler"></param>
        /// <param name="options"></param>
        public override void Sync(ObjectClass objClass, SyncToken token,
                                  SyncResultsHandler handler, OperationOptions options)
        {
            //TODO: implement Sync
            base.Sync(objClass, token, handler, options);
        }

        /// <summary>
        /// Implementation of SynOp.GetLatestSyncToken
        /// </summary>
        /// <returns></returns>
        public override SyncToken GetLatestSyncToken()
        {
            //TODO: Implement GetLatestSyncToken
            return base.GetLatestSyncToken();
        }

        /// <summary>
        /// Implementation of SearchOp.ExecuteQuery
        /// </summary>
        /// <param name="oclass"></param>
        /// <param name="query"></param>
        /// <param name="handler"></param>
        /// <param name="options"></param>
        public override void ExecuteQuery(ObjectClass oclass, string query,
                                          ResultsHandler handler, OperationOptions options)
        {
            ArrayList attsToGet = null;
            if (options != null && options.AttributesToGet != null)
            {
                attsToGet = new ArrayList(options.AttributesToGet);
            }
            //delegate to get the exchange attributes if requested            
            ResultsHandler filter = delegate(ConnectorObject cobject)
                {
                    ConnectorObject filtered = ExchangeUtils.ReplaceAttributes(cobject, attsToGet, ATT_MAPPING);
                    return handler(filtered);
                };

            ResultsHandler handler2use = handler;
            OperationOptions options2use = options;
            if (options != null && options.AttributesToGet != null)
            {
                if (attsToGet.Contains(ATT_DATABASE) || attsToGet.Contains(ATT_EXTERNAL_MAIL) ||
                    attsToGet.Contains(ATT_RECIPIENT_TYPE))
                {
                    //replace Exchange attributes with AD names
                    var newAttsToGet = ExchangeUtils.FilterReplace(attsToGet, ATT_MAPPING);
                    //we have to remove recipient type, as it is unknown to AD
                    newAttsToGet.Remove(ATT_RECIPIENT_TYPE);
                    //build new op options
                    var builder = new OperationOptionsBuilder(options);
                    builder.AttributesToGet = (string[]) newAttsToGet.ToArray(typeof(string));
                    options2use = builder.Build();
                    handler2use = filter;
                }
            }
            base.ExecuteQuery(oclass, query, handler2use, options2use);
        }

        /// <summary>
        /// Implementation of SearchOp.CreateFilterTranslator
        /// </summary>
        /// <param name="oclass"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        public override FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return new LegacyExchangeConnectorFilterTranslator();
        }

        /// <summary>
        /// Inits the connector with configuration injected
        /// </summary>
        /// <param name="configuration"></param>
        public override void Init(Configuration configuration)
        {
            base.Init(configuration);
            _configuration = (ExchangeConfiguration)configuration;
            _runspace = new RunSpaceInstance(RunSpaceInstance.SnapIn.Exchange);
        }


        /// <summary>
        /// Dispose resources
        /// </summary>
        public override void Dispose()
        {
            //lock is probably not necessary
            lock (this)
            {
                if (_disposed)
                {
                    return;
                }
                if (_runspace != null)
                {
                    _runspace.Dispose();
                }
                base.Dispose();
                _disposed = true;
            }

        }

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        protected override ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            //get the object class from base
            ObjectClassInfo oinfo = base.GetObjectClassInfo(oc);

            //add additional attributes for ACCOUNT
            if (oc == ObjectClass.ACCOUNT)
            {

                var oiBuilder = new ObjectClassInfoBuilder
                                                       {
                                                           IsContainer = oinfo.IsContainer,
                                                           ObjectType = oinfo.ObjectType
                                                       };
                oiBuilder.AddAllAttributeInfo(oinfo.ConnectorAttributeInfos);
                oiBuilder.AddAttributeInfo(ATTINFO_DATABASE);
                oiBuilder.AddAttributeInfo(ATTINFO_RECIPIENT_TYPE);
                oiBuilder.AddAttributeInfo(ATTINFO_EXTERNAL_MAIL);
            }

            //return
            return oinfo;
        }

        /// <summary>
        /// Attribute normalizer
        /// </summary>
        /// <param name="oclass">Object class</param>
        /// <param name="attribute">Attribute to be normalized</param>
        /// <returns>normalized attribute</returns>
        public override ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute)
        {
            //normalize the attribute using AD connector first
            attribute = base.NormalizeAttribute(oclass, attribute);
            //normalize external mail value
            if (attribute.Name == ATT_EXTERNAL_MAIL && attribute.Value != null)
            {
                IList<object> normAttributes = new List<object>();
                bool normalized = false;
                foreach (object val in attribute.Value)
                {
                    string strVal = val as string;
                    if (strVal != null)
                    {
                        string[] split = strVal.Split(':');
                        if (split.Length == 2)
                        {
                            //it contains delimiter, use the second part
                            normAttributes.Add(split[1]);
                            normalized = true;
                        } else
                        {
                            //put the original value
                            normAttributes.Add(val);
                        }
                    }
                }
                if (normalized)
                {
                    //build the attribute again
                    return ConnectorAttributeBuilder.Build(attribute.Name, normAttributes);                    
                }
                
            }
            //return the original attribute
            return attribute;            
        }

        
        /// <summary>
        /// helper method to filter out all attributes used in LegacyExchangeConnector only
        /// </summary>
        /// <param name="attributes"></param>
        /// <returns></returns>
        private static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes)
        {
            return ExchangeUtils.FilterOut(attributes, ATT_RECIPIENT_TYPE, ATT_DATABASE, ATT_EXTERNAL_MAIL);
        }

        /// <summary>
        /// helper method for searching object in AD by UID
        /// </summary>
        /// <param name="uid"></param>
        /// <param name="oclass"></param>
        /// <param name="options"></param>
        /// <returns></returns>
        private ConnectorObject ADSearchByUid(Uid uid, ObjectClass oclass, OperationOptions options)
        {
            Assertions.NullCheck(uid, "uid");
            Assertions.NullCheck(oclass, "oclass");
            if (options == null)
            {
                options = new OperationOptionsBuilder().Build();
            }

            ConnectorObject ret = null;
            Filter filter = FilterBuilder.EqualTo(uid);
            var translator =
                base.CreateFilterTranslator(oclass, options);
            IList<string> queries = translator.Translate(filter);

            if (queries.Count == 1)
            {
                ResultsHandler handler = delegate(ConnectorObject cobject)
                {
                    ret = cobject;
                    return false;
                };
                base.ExecuteQuery(oclass, queries[0], handler, options);
            }


            return ret;
        }

    }

    ///<summary>
    /// Filter translator which does MS Exchange specific things
    ///</summary>
    public class LegacyExchangeConnectorFilterTranslator : ActiveDirectoryFilterTranslator
    {
        protected override string[] GetLdapNamesForAttribute(ConnectorAttribute attr)
        {

            if (attr.Is(LegacyExchangeConnector.ATT_DATABASE))
            {
                return new string[] { LegacyExchangeConnector.ATT_DATABASE_AD_NAME };
            }
            if (attr.Is(LegacyExchangeConnector.ATT_EXTERNAL_MAIL))
            {
                return new string[] { LegacyExchangeConnector.ATT_EXTERNAL_MAIL_AD_NAME };
            }
            return base.GetLdapNamesForAttribute(attr);
        }
    }

}
