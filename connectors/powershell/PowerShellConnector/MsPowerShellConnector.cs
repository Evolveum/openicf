/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
using System;
using System.Collections;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using Org.IdentityConnectors.Framework.Common.Objects;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    public enum OperationType
    {
        AUTHENTICATE,
        CREATE,
        DELETE,
        GET_LATEST_SYNC_TOKEN,
        RESOLVE_USERNAME,
        SCHEMA,
        SEARCH,
        SYNC,
        TEST,
        RUNSCRIPTONCONNECTOR,
        RUNSCRIPTONRESOURCE,
        UPDATE,


        //ADD_ATTRIBUTE_VALUES,
        //REMOVE_ATTRIBUTE_VALUES
    };

    [ConnectorClass("connector_displayName",
                     typeof(MsPowerShellConfiguration),
                     MessageCatalogPaths = new[] { "Org.ForgeRock.OpenICF.Connectors.MsPowerShell.Messages" }
                     )]
    public class MsPowerShellConnector : PoolableConnector, TestOp, SearchOp<Filter>,
         CreateOp, UpdateOp, DeleteOp, SyncOp, AuthenticateOp, ResolveUsernameOp, SchemaOp, ScriptOnConnectorOp
    {
        protected static String Username = "Username";
        protected static String Password = "Password";
        protected static String Action = "Action";
        protected static String OperationName = "Operation";
        protected static String ObjectClassName = "ObjectClass";
        protected static String Uid = "Uid";
        protected static String Id = "Id";
        protected static String Attributes = "Attributes";
        protected static String Options = "Options";
        protected static String Connection = "Connection";
        protected static String SchemaBldr = "SchemaBuilder";
        protected static String Configuration = "Configuration";
        protected static String Logger = "Log";
        protected static String Token = "Token";
        protected static String Handler = "Handler";
        protected static String Query = "Query";
        protected static String Filter = "Filter";
        protected static String Result = "Result";
        protected static String ScriptArguments = "Arguments";
        protected static String GuardedString = "GuardedString";
        
        private MsPowerShellConfiguration _configuration;
        private MsPowerShellHost _psHost;
        private Schema _schema;
        private Visitors _visitor;
        private String _scriptPrefix = "Connector";
        private Hashtable _attrSubstitute;


        public enum Visitors
        {
            Map,
            Ldap,
            AdPsModule,
            NativeQuery
        };

        #region IDisposable Members

        public virtual void Dispose()
        {
            _psHost.Dispose();
        }

        #endregion

        #region Connector Members

        // implementation of Connector
        public virtual void Init(Configuration configuration)
        {
            Trace.TraceInformation("PowerShell Connector Init method");
            _configuration = (MsPowerShellConfiguration)configuration;
            _configuration.Validate();
            if (_configuration.UseInterpretersPool)
            {
                _psHost = new MsPowerShellHost(_configuration.GetRunspacePool());
            }
            else
            {
                _psHost = _configuration.PsModulesToImport.Length > 0 ? new MsPowerShellHost(_configuration.PsModulesToImport) : new MsPowerShellHost();
            }

            _attrSubstitute = new Hashtable();
            if (_configuration.SubstituteUidAndNameInQueryFilter)
            {
                _attrSubstitute.Add("__UID__", _configuration.UidAttributeName);
                _attrSubstitute.Add("__NAME__", _configuration.NameAttributeName);
            }

            if ((_configuration.VariablesPrefix != null) && (!"".Equals(_configuration.VariablesPrefix)))
            {
                _scriptPrefix = _configuration.VariablesPrefix;
            }

            if ("Map".Equals(_configuration.QueryFilterType))
            {
                _visitor = Visitors.Map;
            }
            else if ("Ldap".Equals(_configuration.QueryFilterType))
            {
                _visitor = Visitors.Ldap;
            }
            else if ("Native".Equals(_configuration.QueryFilterType))
            {
                _visitor = Visitors.NativeQuery;
            }
            else if ("AdPsModule".Equals(_configuration.QueryFilterType))
            {
                _visitor = Visitors.AdPsModule;
            }
        }

        #endregion

        #region PoolableConnector Members

        public void CheckAlive()
        {
        }

        #endregion

        #region TestOp Members

        public virtual void Test()
        {
            //Configuration.Validate();
            ParseScripts();

            Trace.TraceInformation("Invoke Test");
            try
            {
                ExecuteTest(_configuration.TestScriptFileName);
                Trace.TraceInformation("Test ok");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected void ExecuteTest(String scriptName)
        {
            ExecuteScript(GetScript(scriptName),
                CreateBinding(new Dictionary<String, Object>(), OperationType.TEST, null, null, null, null));
        }
        #endregion

        #region SchemaOp
        public Schema Schema()
        {
            Trace.TraceInformation("Invoke Schema");
            if (_schema != null)
            {
                return _schema;
            }

            try
            {
                _schema = ExecuteSchema(_configuration.SchemaScriptFileName);
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
            if (null == _schema)
            {
                throw new ConnectorException("Schema script must return with Schema object");
            }
            return _schema;
        }

        protected Schema ExecuteSchema(String scriptName)
        {
            var scb = new SchemaBuilder(SafeType<Connector>.Get(this));
            var arguments = new Dictionary<String, Object>
            {
                {SchemaBldr, scb}
            };

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.SCHEMA, null, null, null, null));
            return scb.Build();
        }

        #endregion

        #region ResolveUsernameOp
        public Uid ResolveUsername(ObjectClass objectClass, string username, OperationOptions options)
        {
            Trace.TraceInformation("Invoke ResolveUsername ObjectClass:{0}=>{1}", objectClass.GetObjectClassValue(), username);
            try
            {
                Object uidAfter = ExecuteResolveUsername(_configuration.ResolveUsernameScriptFileName, objectClass,
                    username, options);
                if (uidAfter is String)
                {
                    Trace.TraceInformation("{0}:{1} resolved", objectClass.GetObjectClassValue(), uidAfter);
                    return new Uid((String)uidAfter);
                }
                if (uidAfter is Uid)
                {
                    Trace.TraceInformation("{0}:{1} resolved", objectClass.GetObjectClassValue(), uidAfter);
                    return (Uid)uidAfter;
                }
                throw new ConnectorException("ResolveUsername script didn't return with the uid (__UID__) string value");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected Object ExecuteResolveUsername(String scriptName, ObjectClass objectClass, String username, OperationOptions options)
        {
            var result = new MsPowerShellUidHandler();
            var arguments = new Dictionary<String, Object>
            {
                {Result, result},
                {Username, username}
            };

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.RESOLVE_USERNAME, objectClass, null, null, options));
            return result.Uid;
        }

        #endregion

        #region AuthenticateOp Members

        public Uid Authenticate(ObjectClass objectClass, string username, GuardedString password, OperationOptions options)
        {
            Trace.TraceInformation("Invoke Authenticate ObjectClass:{0}=>{1}", objectClass.GetObjectClassValue(), username);
            try
            {
                Object uidAfter = ExecuteAuthenticate(_configuration.AuthenticateScriptFileName, objectClass, username,
                    password, options);
                if (uidAfter is String)
                {
                    Trace.TraceInformation("{0}:{1} authenticated", objectClass.GetObjectClassValue(), uidAfter);
                    return new Uid((String)uidAfter);
                }
                if (uidAfter is Uid)
                {
                    var u = uidAfter as Uid;
                    Trace.TraceInformation("{0}:{1} authenticated", objectClass.GetObjectClassValue(), u.GetUidValue());
                    return u;
                }
                throw new ConnectorException("Authenticate script didn't return with the uid (__UID__) string value");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected Object ExecuteAuthenticate(String scriptName, ObjectClass objectClass, String username,
            GuardedString password, OperationOptions options)
        {
            var result = new MsPowerShellUidHandler();
            var arguments = new Dictionary<String, Object>
            {
                {Result, result},
                {Username, username},
                {Password, SecurityUtil.Decrypt(password)}
            };

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.AUTHENTICATE, objectClass, null, null, options));
            return result.Uid;
        }

        #endregion

        #region SearchOp<IDictionary<String, Object>> Members

        // implementation of SearchSpiOp
        public virtual FilterTranslator<Filter> CreateFilterTranslator(ObjectClass objectClass, OperationOptions options)
        {
            return new MsPowerShellFilterTranslator2();
        }


        // implementation of SearchSpiOp
        public virtual void ExecuteQuery(ObjectClass objectClass, Filter query,
            ResultsHandler handler, OperationOptions options)
        {
            Trace.TraceInformation("Invoke ExecuteQuery ObjectClass:{0}", objectClass.GetObjectClassValue());

            try
            {
                ExecuteQuery(_configuration.SearchScriptFileName, objectClass, query, handler, options);
                Trace.TraceInformation("Search ok");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected void ExecuteQuery(String scriptName, ObjectClass objectClass, Filter query, ResultsHandler handler,
            OperationOptions options)
        {
            var arguments = new Dictionary<String, Object>
            {
                {Result, new MsPowerShellSearchResults(objectClass, handler)}
            };
            if (query != null)
            {
                switch (_visitor)
                {
                    case Visitors.Map:
                        arguments.Add(Query, query.Accept<Dictionary<String, Object>, Hashtable>(new MapFilterVisitor(), _attrSubstitute));
                        break;
                    case Visitors.AdPsModule:
                        arguments.Add(Query, query.Accept<String, Hashtable>(new AdPsModuleFilterVisitor(), _attrSubstitute));
                        break;
                    case Visitors.Ldap:
                        arguments.Add(Query, query.Accept<String, Hashtable>(new LdapFilterVisitor(), _attrSubstitute));
                        break;
                    case Visitors.NativeQuery:
                        arguments.Add(Query, query);
                        break;
                }
            }


            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.SEARCH, objectClass, null, null, options));
        }
        #endregion

        #region CreateOp Members
        // implementation of CreateSpiOp
        public virtual Uid Create(ObjectClass objectClass, ICollection<ConnectorAttribute> createAttributes, OperationOptions options)
        {
            Trace.TraceInformation("Invoke Create ObjectClass:{0}", objectClass.GetObjectClassValue());
            try
            {
                Uid uid = ExecuteCreate(_configuration.CreateScriptFileName, objectClass, createAttributes, options);
                if (uid == null)
                {
                    throw new ConnectorException("Create script didn't return with a valid uid (__UID__) string value");
                }
                Trace.TraceInformation("{0}:{1} created", objectClass.GetObjectClassValue(), uid.GetUidValue());
                return uid;
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected Uid ExecuteCreate(String scriptName, ObjectClass objectClass, ICollection<ConnectorAttribute> createAttributes, OperationOptions options)
        {
            var result = new MsPowerShellUidHandler();
            var arguments = new Dictionary<String, Object> { { Result, result } };

            if (ConnectorAttributeUtil.GetNameFromAttributes(createAttributes) != null)
                arguments.Add(Id, ConnectorAttributeUtil.GetNameFromAttributes(createAttributes).GetNameValue());

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.CREATE, objectClass, null, createAttributes, options));
            return result.Uid.GetUidValue() != null ? result.Uid : null;
        }

        #endregion

        #region UpdateOp Members
        public Uid Update(ObjectClass objectClass, Uid uid, ICollection<ConnectorAttribute> valuesToReplace, OperationOptions options)
        {
            Trace.TraceInformation("Invoke Update ObjectClass: {0}/{1}", objectClass.GetObjectClassValue(), uid.GetUidValue());
            try
            {
                Uid uidAfter = ExecuteUpdate(_configuration.UpdateScriptFileName, objectClass, uid, valuesToReplace, options);
                if (uidAfter == null)
                    throw new ConnectorException("Update script didn't return with a valid uid (__UID__) value");
                Trace.TraceInformation("{0}:{1} updated", objectClass.GetObjectClassValue(), uidAfter.GetUidValue());
                return uidAfter;
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }

        }

        protected Uid ExecuteUpdate(String scriptName, ObjectClass objectClass, Uid uid,
            ICollection<ConnectorAttribute> updateAttributes, OperationOptions options)
        {
            var result = new MsPowerShellUidHandler { Uid = uid };
            var arguments = new Dictionary<String, Object>
            {
                {Result, result},
            };

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.UPDATE, objectClass, uid, updateAttributes, options));
            return result.Uid.GetUidValue() != null ? result.Uid : null;
        }

        #endregion

        #region DeleteOp Members

        // implementation of DeleteSpiOp
        public virtual void Delete(ObjectClass objectClass, Uid uid, OperationOptions options)
        {
            Trace.TraceInformation("Invoke Delete ObjectClass:{0}/{1}", objectClass.GetObjectClassValue(), uid.GetUidValue());

            try
            {
                ExecuteDelete(_configuration.DeleteScriptFileName, objectClass, uid, options);
                Trace.TraceInformation("Delete ok");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected void ExecuteDelete(String scriptName, ObjectClass objectClass, Uid uid, OperationOptions options)
        {
            ExecuteScript(GetScript(scriptName), CreateBinding(new Dictionary<String, Object>(), OperationType.DELETE, objectClass, uid, null, options));
        }

        #endregion

        #region SyncOp members
        public virtual SyncToken GetLatestSyncToken(ObjectClass objectClass)
        {
            Trace.TraceInformation("Invoke GetLatestSyncToken ObjectClass:{0}", objectClass.GetObjectClassValue());
            SyncToken token = null;

            try
            {
                Object result = ExecuteGetLatestSyncToken(_configuration.SyncScriptFileName, objectClass);
                if (result is SyncToken)
                {
                    token = result as SyncToken;
                }
                else if (null != result)
                {
                    token = new SyncToken(result);
                }
                Trace.TraceInformation("GetLatestSyncToken ok");
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
            return token;
        }

        protected Object ExecuteGetLatestSyncToken(String scriptName, ObjectClass objectClass)
        {
            var result = new MsPowerShellSyncTokenHandler();
            var arguments = new Dictionary<String, Object> { { Result, result } };

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.GET_LATEST_SYNC_TOKEN, objectClass, null, null, null));
            return result.SyncToken;
        }

        public virtual void Sync(ObjectClass objectClass, SyncToken token,
            SyncResultsHandler handler, OperationOptions options)
        {
            Trace.TraceInformation("Invoke Sync ObjectClass:{0}->{1}", objectClass.GetObjectClassValue(), token);

            try
            {
                ExecuteSync(_configuration.SyncScriptFileName, objectClass, token, handler, options);
                Trace.TraceInformation("Sync ok");
            }
            catch (ConnectorException)
            {
                throw;
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected void ExecuteSync(String scriptName, ObjectClass objectClass, SyncToken token,
            SyncResultsHandler handler, OperationOptions options)
        {
            var arguments = new Dictionary<String, Object>
            {
                {Result, new MsPowerShellSyncResults(objectClass, handler)}
            };
            if (token != null) { arguments.Add(Token, token.Value); }

            ExecuteScript(GetScript(scriptName), CreateBinding(arguments, OperationType.SYNC, objectClass, null, null, options));
        }
        #endregion

        #region ScriptOnConnectorOp members

        public object RunScriptOnConnector(ScriptContext request, OperationOptions options)
        {
            Trace.TraceInformation("Invoke RunScriptOnConnector");
            if (!"PowerShell".Equals(request.ScriptLanguage,StringComparison.CurrentCultureIgnoreCase))
            {
                throw new ArgumentException("Script language must be PowerShell");
            }

            try
            {
                var result = ExecuteScriptOnConnector(request, options);
                Trace.TraceInformation("RunScriptOnConnector ok");
                return result;
            }
            catch (Exception e)
            {
                if (e.InnerException != null)
                {
                    throw e.InnerException;
                }
                throw;
            }
        }

        protected Object ExecuteScriptOnConnector(ScriptContext request, OperationOptions options)
        {
            var reqArguments = new Dictionary<String, Object>();
            foreach (var entry in request.ScriptArguments)
            {
                reqArguments.Add(entry.Key,entry.Value);
            }

            var arguments = new Dictionary<String, Object>
            {
                {ScriptArguments, reqArguments}
            };

            return ExecuteScript(request.ScriptText, CreateBinding(arguments, OperationType.RUNSCRIPTONCONNECTOR, null, null, null, options));
        }
        #endregion

        #region protected members

        protected Dictionary<String, Object> CreateBinding(Dictionary<String, Object> arguments, OperationType operationType,
            ObjectClass objectClass, Uid uid, ICollection<ConnectorAttribute> attributes,
            OperationOptions options)
        {
            var binding = new Dictionary<String, Object>();

            arguments.Add(Action, operationType);
            arguments.Add(OperationName, operationType);
            arguments.Add(Configuration, _configuration);


            if (uid != null)
            {
                arguments.Add(Uid, uid);
            }

            if (attributes != null)
            {
                arguments.Add(Attributes, attributes);
            }

            if (objectClass != null)
            {
                arguments.Add(ObjectClassName, objectClass);
            }

            if (options != null)
            {
                arguments.Add(Options, options);
            }

            binding.Add(_scriptPrefix, arguments);
            return binding;
        }

        protected Object ExecuteScript(String script, Dictionary<String, Object> arguments)
        {
            return _psHost.ExecuteScript(script, arguments);
        }

        #endregion

        #region private members

        private String GetScript(String fileName)
        {
            Object script;
            if (_configuration.ReloadScriptOnExecution)
            {
                script = loadScript(fileName);
            }
            else if (!_configuration.PropertyBag.TryGetValue(fileName, out script))
            {
                script = loadScript(fileName);
                _configuration.PropertyBag.TryAdd(fileName, script);
            }
            return (String) script;
        }

        private void ParseScripts()
        {
            foreach (var file in _configuration.GetValidScripts())
            {
                try
                {
                    var script = loadScript(file);
                    _psHost.ValidateScript(script);
                    Trace.TraceInformation("{0} script parsed successfully", file);
                    _configuration.PropertyBag.TryAdd(file, script);
                }
                catch (Exception ex)
                {
                    throw new ConnectorException(file + " script parse error: " + ex.Message);
                }
            }
        }

        //private String loadScript(String filename, Boolean useCache)
        private String loadScript(String filename)
        {
            //TODO: handle file encoding
            //TODO: cache scripts?
            var script = System.IO.File.ReadAllText(filename);
            return script;
        }

        #endregion

        
    }
}
