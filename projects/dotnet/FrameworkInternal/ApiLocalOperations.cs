/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
using System;

using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Pooling;
using Org.IdentityConnectors.Common.Proxy;
using Org.IdentityConnectors.Common.Script;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Serializer;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System.Reflection;
using System.Collections.Generic;

namespace Org.IdentityConnectors.Framework.Impl.Api.Local.Operations
{    
    #region APIOperationRunner
    /**
     * NOTE: internal class, public only for unit tests
     * Base class for API operation runners.
     */
    public abstract class APIOperationRunner {
        
        /**
         * Context that has all the information required to execute an operation.
         */
        private readonly OperationalContext _context;
    
        
        /**
         * Creates the API operation so it can called multiple times.
         */
        public APIOperationRunner(OperationalContext context) {
            _context = context;
            //TODO: verify this
            // get the APIOperation that this class implements..
            //List<Class<? extends APIOperation>> apiOps = getInterfaces(this
                    //.getClass(), APIOperation.class);
            // there should be only one..
            //if (apiOps.size() > 1) {
            //    final String MSG = "Must only implement one operation.";
            //    throw new IllegalStateException(MSG);
            //}
        }
        
        /**
         * Get the current operational context.
         */
        public OperationalContext GetOperationalContext() {
            return _context;
        }
        
    }
    #endregion
    
    #region ConnectorAPIOperationRunner
    /**
     * NOTE: internal class, public only for unit tests
     * Subclass of APIOperationRunner for operations that require a connector.
     */
    public abstract class ConnectorAPIOperationRunner : APIOperationRunner {
    
        /**
         * The connector instance
         */
        private readonly Connector _connector;
        
        /**
         * Creates the API operation so it can called multiple times.
         */
        public ConnectorAPIOperationRunner(ConnectorOperationalContext context,
                                           Connector connector) : base(context) {
            _connector = connector;
        }
                
        public Connector GetConnector() {
            return _connector;
        }
        
        public ObjectNormalizerFacade GetNormalizer(ObjectClass oclass) {
            AttributeNormalizer norm = null;
            Connector connector = GetConnector();
            if ( connector is AttributeNormalizer ) {
                norm = (AttributeNormalizer)connector;
            }
            return new ObjectNormalizerFacade(oclass,norm);
        }
    }
    #endregion

    #region ConnectorAPIOperationRunnerProxy
    /**
     * Proxy for APIOperationRunner that takes care of setting up underlying
     * connector and creating the implementation of APIOperationRunner.
     * The implementation of APIOperationRunner gets created whenever the
     * actual method is invoked.
     */
    internal class ConnectorAPIOperationRunnerProxy : InvocationHandler {
    
        
        /**
         * The operational context
         */
        private readonly ConnectorOperationalContext _context;
        
        /**
         * The implementation constructor. The instance is lazily created upon
         * invocation
         */
        private readonly ConstructorInfo _runnerImplConstructor;
        
        /**
         * Create an APIOperationRunnerProxy
         * @param context The operational context
         * @param runnerImplConstructor The implementation constructor. Implementation
         * must define a two-argument constructor(OperationalContext,Connector)
         */
        public ConnectorAPIOperationRunnerProxy(ConnectorOperationalContext context,
                ConstructorInfo runnerImplConstructor) {
            _context = context;
            _runnerImplConstructor = runnerImplConstructor;
        }
        
        public Object Invoke(Object proxy, MethodInfo method, object[] args)
        {
            //do not proxy equals, hashCode, toString
            if (method.DeclaringType.Equals(typeof(object))) {
                return method.Invoke(this, args);
            }
            object ret = null;
            Connector connector = null;
            ObjectPool<PoolableConnector> pool = _context.GetPool();
            // get the connector class..
            SafeType<Connector> connectorClazz = _context.GetConnectorClass();
            try {
                // pooling is implemented get one..
                if (pool != null) {
                    connector = pool.BorrowObject();
                }
                else {
                    // get a new instance of the connector..
                    connector = connectorClazz.CreateInstance();
                    // initialize the connector..
                    connector.Init(_context.GetConfiguration());
                }
                APIOperationRunner runner = 
                    (APIOperationRunner)_runnerImplConstructor.Invoke(new object[]{
                                                      _context,
                                                      connector});
                ret = method.Invoke(runner, args);
                // call out to the operation..
            } catch (TargetInvocationException e) {
                Exception root = e.InnerException;
                throw root;
            } finally {
                // make sure dispose of the connector properly
                if (connector != null) {
                    // determine if there was a pool..
                    if (pool != null) {
                        try {
                            //try to return it to the pool even though an
                            //exception may have happened that leaves it in
                            //a bad state. The contract of checkAlive
                            //is that it will tell you if the connector is
                            //still valid and so we leave it up to the pool
                            //and connector to work it out.
                            pool.ReturnObject((PoolableConnector)connector);
                        } catch (Exception e) {
                            //don't let pool exceptions propogate or mask
                            //other exceptions. do log it though.
                            TraceUtil.TraceException(null,e);
                        }
                    }
                    //not pooled - just dispose
                    else {
                        //dispose it not supposed to throw, but just in case,
                        //catch the exception and log it so we know about it
                        //but don't let the exception prevent additional
                        //cleanup that needs to happen
                        try {
                            connector.Dispose();
                        } catch (Exception e) {
                            //log this though
                            TraceUtil.TraceException(null,e);
                        }                    
                    }
                }                
            }
            return ret;
        }    
    }
    #endregion

    #region ConnectorOperationalContext
    /**
     * NOTE: internal class, public only for unit tests
     * Simple structure to pass more variables through the constructor of
     * {@link APIOperationRunner}.
     */
    public class ConnectorOperationalContext : OperationalContext {
                  
        private readonly ObjectPool<PoolableConnector> _pool;
        
        public ConnectorOperationalContext(LocalConnectorInfoImpl connectorInfo,
                APIConfigurationImpl apiConfiguration,
                ObjectPool<PoolableConnector> pool)
            :base(connectorInfo,apiConfiguration) {
            _pool = pool;
        }
    
        public ObjectPool<PoolableConnector> GetPool() {
            return _pool;
        }
    
    
        public SafeType<Connector> GetConnectorClass() {
            return GetConnectorInfo().ConnectorClass;
        }
    
    }
    #endregion
    
    #region AuthenticationImpl
    internal class AuthenticationImpl : ConnectorAPIOperationRunner,
            AuthenticationApiOp {
        /**
         * Pass the configuration etc to the abstract class.
         */
        public AuthenticationImpl(ConnectorOperationalContext context,
                Connector connector) 
                :base(context,connector) {
        }
    
        /**
         * Authenticate using the basic credentials.
         * 
         * @see Authentication#authenticate(String, String)
         */
        public Uid Authenticate(String username, GuardedString password, OperationOptions options) {
            Assertions.NullCheck(username, "username");
            Assertions.NullCheck(password, "password");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            return ((AuthenticateOp) GetConnector()).Authenticate(username, password,options);
        }
    }
    #endregion
    
    #region CreateImpl
    internal class CreateImpl : ConnectorAPIOperationRunner,
            CreateApiOp {
    
        /**
         * Initializes the operation works.
         */
        public CreateImpl(ConnectorOperationalContext context,
                Connector connector) 
                :base(context,connector) {
        }
    
        /**
         * Calls the create method on the Connector side.
         * 
         * @see CreateApiOp#create(Set)
         */
        public Uid Create(ObjectClass oclass, ICollection<ConnectorAttribute> attributes, OperationOptions options) {
            Assertions.NullCheck(oclass, "oclass");
            Assertions.NullCheck(attributes, "attributes");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
        	HashSet<string> dups = new HashSet<string>();
        	foreach (ConnectorAttribute attr in attributes) {
        		if (dups.Contains(attr.Name)) {
        			throw new ArgumentException("Duplicate attribute name exists: " + attr.Name);
        		}
        		dups.Add(attr.Name);
        	}
        	if (oclass == null) {
        		throw new ArgumentException("Required attribute ObjectClass not found!");
        	}
            Connector connector = GetConnector();
            ObjectNormalizerFacade normalizer = GetNormalizer(oclass);
            ICollection<ConnectorAttribute> normalizedAttributes =
                normalizer.NormalizeAttributes(attributes);
            // create the object..
            Uid ret = ((CreateOp) connector).Create(oclass,attributes,options);
            return (Uid)normalizer.NormalizeAttribute(ret);
        }
    }
    #endregion
    
    #region DeleteImpl
    internal class DeleteImpl : ConnectorAPIOperationRunner ,
            DeleteApiOp {
    
        /**
         * Initializes the operation works.
         */
        public DeleteImpl(ConnectorOperationalContext context,
                Connector connector) 
            :base(context,connector) {
        }
        /**
         * Calls the delete method on the Connector side.
         * 
         * @see com.sun.openconnectors.framework.api.operations.CreateApiOp#create(java.util.Set)
         */
        public void Delete(ObjectClass objClass, Uid uid, OperationOptions options) {
            Assertions.NullCheck(objClass, "objClass");
            Assertions.NullCheck(uid, "uid");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            Connector connector = GetConnector();
            ObjectNormalizerFacade normalizer = GetNormalizer(objClass);
            // delete the object..
            ((DeleteOp) connector).Delete(objClass, 
                                          (Uid)normalizer.NormalizeAttribute(uid), 
                                          options);
        }
    }
    #endregion
    
    #region AttributesToGetResultsHandler
    public abstract class AttributesToGetResultsHandler {
        // =======================================================================
        // Fields
        // =======================================================================
         private readonly string[] _attrsToGet;

        // =======================================================================
        // Constructors
        // =======================================================================
        /**
         * Keep the attribute to get..
         */
        public AttributesToGetResultsHandler(string[] attrsToGet) {
            Assertions.NullCheck(attrsToGet, "attrsToGet");
            _attrsToGet = attrsToGet;
        }
    
        /**
         * Simple method that clones the object and remove the attribute thats are
         * not in the {@link OperationOptions#OP_ATTRIBUTES_TO_GET} set.
         * 
         * @param attrsToGet
         *            case insensitive set of attribute names.
         */
        public ICollection<ConnectorAttribute> ReduceToAttrsToGet(
            ICollection<ConnectorAttribute> attrs) {
            ICollection<ConnectorAttribute> ret = new HashSet<ConnectorAttribute>();
            IDictionary<string, ConnectorAttribute> map = ConnectorAttributeUtil.ToMap(attrs);
            foreach (string attrName in _attrsToGet) {
                ConnectorAttribute attr = CollectionUtil.GetValue(map, attrName, null);
                // TODO: Should we throw if the attribute is not yet it was
                // requested?? Or do we ignore because the API maybe asking
                // for what the resource doesn't have??
                if (attr != null) {
                    ret.Add(attr);
                }
            }
            return ret;
        }
        public ConnectorObject ReduceToAttrsToGet(ConnectorObject obj) {
            // clone the object and reduce the attributes only the set of
            // attributes.
            ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
            bld.SetUid(obj.Uid);
            bld.SetName(obj.Name);
            bld.ObjectClass = obj.ObjectClass;
            ICollection<ConnectorAttribute> objAttrs = obj.GetAttributes();
            ICollection<ConnectorAttribute> attrs = ReduceToAttrsToGet(objAttrs);
            bld.AddAttributes(attrs);
            return bld.Build();
        }    
    }
    #endregion
    
    #region SearchAttributesToGetResultsHandler
    public sealed class SearchAttributesToGetResultsHandler : 
        AttributesToGetResultsHandler {
    
        // =======================================================================
        // Fields
        // =======================================================================
        private readonly ResultsHandler _handler;
        
        // =======================================================================
        // Constructors
        // =======================================================================
        public SearchAttributesToGetResultsHandler(
            ResultsHandler handler, string[] attrsToGet) : base(attrsToGet) {
            Assertions.NullCheck(handler, "handler");
            this._handler = handler;
        }
    
        public bool Handle(ConnectorObject obj) {
            // clone the object and reduce the attributes only the set of
            // attributes.
            return _handler(ReduceToAttrsToGet(obj));
        }    
    }
    #endregion
    
    #region SearchAttributesToGetResultsHandler
    public sealed class SyncAttributesToGetResultsHandler : 
        AttributesToGetResultsHandler {
    
        // =======================================================================
        // Fields
        // =======================================================================
        private readonly SyncResultsHandler _handler;
        
        // =======================================================================
        // Constructors
        // =======================================================================
        public SyncAttributesToGetResultsHandler(
            SyncResultsHandler handler, string[] attrsToGet) : base(attrsToGet) {
            Assertions.NullCheck(handler, "handler");
            this._handler = handler;
        }
    
        public bool Handle(SyncDelta delta) {
            SyncDeltaBuilder bld = new SyncDeltaBuilder();
            bld.Uid = delta.Uid;
            bld.Token = delta.Token;
            bld.DeltaType = delta.DeltaType;
            if ( delta.Object != null ) {
                bld.Object=ReduceToAttrsToGet(delta.Object);
            }
            return _handler(bld.Build());
        }
    }
    #endregion

    #region DuplicateFilteringResultsHandler
    public sealed class DuplicateFilteringResultsHandler  {
    
        // =======================================================================
        // Fields
        // =======================================================================
        private readonly ResultsHandler _handler;
        private readonly HashSet<String> _visitedUIDs = new HashSet<String>();
        
        private bool _stillHandling;
        
        // =======================================================================
        // Constructors
        // =======================================================================
        /**
         * Filter chain for producers.
         * 
         * @param producer
         *            Producer to filter.
         *            
         */
        public DuplicateFilteringResultsHandler(ResultsHandler handler) {
            // there must be a producer..
            if (handler == null) {
                throw new ArgumentException("Handler must not be null!");
            }
            this._handler = handler;
        }
    
        public bool Handle(ConnectorObject obj) {
            String uid =
                obj.Uid.GetUidValue();
            if (!_visitedUIDs.Add(uid)) {
                //we've already seen this - don't pass it
                //throw
                return true;
            }
            _stillHandling = _handler(obj);
            return _stillHandling;
        }
        
        public bool IsStillHandling {
            get {
                return _stillHandling;
            }
        }
    
    }
    #endregion
    
    #region FilteredResultsHandler
    public sealed class FilteredResultsHandler  {
    
        // =======================================================================
        // Fields
        // =======================================================================
        readonly ResultsHandler handler;
        readonly Filter filter;
    
        // =======================================================================
        // Constructors
        // =======================================================================
        /**
         * Filter chain for producers.
         * 
         * @param producer
         *            Producer to filter.
         * @param filter
         *            Filter to use to accept objects.
         */
        public FilteredResultsHandler(ResultsHandler handler, Filter filter) {
            // there must be a producer..
            if (handler == null) {
                throw new ArgumentException("Producer must not be null!");
            }
            this.handler = handler;
            // use a default pass through filter..
            this.filter = filter == null ? new PassThruFilter() : filter;
        }
    
        public bool Handle(ConnectorObject obj) {
            if ( filter.Accept(obj) ) {
                return handler(obj);
            }
            else {
                return true;
            }
        }
    
        /**
         * Use a pass thru filter to use if a null filter is provided.
         */
        class PassThruFilter : Filter {
            public bool Accept(ConnectorObject obj) {
                return true;
            }
        }
    
    }
    #endregion
    
    #region GetImpl
    /**
     * Uses {@link SearchOp} to find the object that is referenced by the
     * {@link Uid} provided.
     */
    public class GetImpl : GetApiOp {
    
        readonly SearchApiOp op;
    
        private class ResultAdapter {
            private IList<ConnectorObject> _list = new List<ConnectorObject>();
            public bool Handle(ConnectorObject obj) {
                _list.Add(obj);
                return false;
            }
            public ConnectorObject GetResult() {
                return _list.Count == 0 ? null : _list[0];
            }
        }
        
        public GetImpl(SearchApiOp search) {
            this.op = search;
        }
    
        public ConnectorObject GetObject(ObjectClass objClass, Uid uid, OperationOptions options) {
            Assertions.NullCheck(objClass, "objClass");
            Assertions.NullCheck(uid, "uid");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            Filter filter = FilterBuilder.EqualTo(uid);
            ResultAdapter adapter = new ResultAdapter();
            op.Search(objClass,filter,new ResultsHandler(adapter.Handle),options);
            return adapter.GetResult();
        }
    }
    #endregion
    
    #region OperationalContext
    /**
     * NOTE: internal class, public only for unit tests
     * OperationalContext - base class for operations that do not
     * require a connection.
     */
    public class OperationalContext {
        
        /**
         * ConnectorInfo
         */
        private readonly LocalConnectorInfoImpl connectorInfo;
        
        /**
         * Contains the {@link Connector} {@link Configuration}.
         */
        private readonly APIConfigurationImpl apiConfiguration;
        
    
        public OperationalContext(LocalConnectorInfoImpl connectorInfo,
                APIConfigurationImpl apiConfiguration) {
            this.connectorInfo = connectorInfo;
            this.apiConfiguration = apiConfiguration;
        }
    
        public Configuration GetConfiguration() {
            return CSharpClassProperties.CreateBean((ConfigurationPropertiesImpl)this.apiConfiguration.ConfigurationProperties,
                    connectorInfo.ConnectorConfigurationClass);
        }
        
        protected LocalConnectorInfoImpl GetConnectorInfo() {
            return connectorInfo;
        }
    }
    #endregion
    
    #region NormalizingResultsHandler
    public class NormalizingResultsHandler {
        
        private readonly ResultsHandler _target;
        private readonly ObjectNormalizerFacade _normalizer;
        
        public NormalizingResultsHandler(ResultsHandler target,
                ObjectNormalizerFacade normalizer) {
            Assertions.NullCheck(target, "target");
            Assertions.NullCheck(normalizer, "normalizer");
            _target = target;
            _normalizer = normalizer;
        }
        
    
        public bool Handle(ConnectorObject obj) {
            ConnectorObject normalized = _normalizer.NormalizeObject(obj);
            return _target(normalized);
        }
    
    }
    #endregion 
    
    #region NormalizingSyncResultsHandler
    public class NormalizingSyncResultsHandler {
        
        private readonly SyncResultsHandler _target;
        private readonly ObjectNormalizerFacade _normalizer;
        
        public NormalizingSyncResultsHandler(SyncResultsHandler target,
                ObjectNormalizerFacade normalizer) {
            Assertions.NullCheck(target, "target");
            Assertions.NullCheck(normalizer, "normalizer");
            _target = target;
            _normalizer = normalizer;
        }
        
    
        public bool Handle(SyncDelta delta) {
            SyncDelta normalized = _normalizer.NormalizeSyncDelta(delta);
            return _target(normalized);
        }
    
    }
    #endregion
    
    #region ObjectNormalizerFacade
    public sealed class ObjectNormalizerFacade {
        /**
         * The (non-null) object class
         */
        private readonly ObjectClass _objectClass;
        /**
         * The (possibly null) attribute normalizer
         */
        private readonly AttributeNormalizer _normalizer;
        
        /**
         * Create a new ObjectNormalizer
         * @param objectClass The object class
         * @param normalizer The normalizer. May be null.
         */
        public ObjectNormalizerFacade(ObjectClass objectClass,
                AttributeNormalizer normalizer) {
            Assertions.NullCheck(objectClass, "objectClass");
            _objectClass = objectClass;
            _normalizer  = normalizer;
        }
        
        /**
         * Returns the normalized value of the attribute.
         * If no normalizer is specified, returns the original
         * attribute.
         * @param attribute The attribute to normalize.
         * @return The normalized attribute
         */
        public ConnectorAttribute NormalizeAttribute(ConnectorAttribute attribute) {
            if ( attribute == null ) {
                return null;
            }
            else if (_normalizer != null) {
                return _normalizer.NormalizeAttribute(_objectClass, attribute);
            }
            else {
                return attribute;
            }
        }
        
        /**
         * Returns the normalized set of attributes or null
         * if the original set is null.
         * @param attributes The original attributes.
         * @return The normalized attributes or null if
         * the original set is null.
         */
        public ICollection<ConnectorAttribute> NormalizeAttributes(ICollection<ConnectorAttribute> attributes) {
            if ( attributes == null ) {
                return null;
            }
            ICollection<ConnectorAttribute> temp = new HashSet<ConnectorAttribute>();
            foreach (ConnectorAttribute attribute in attributes ) {
                temp.Add(NormalizeAttribute(attribute));
            }
            return CollectionUtil.AsReadOnlySet(temp);
        }
        
        /**
         * Returns the normalized object.
         * @param orig The original object
         * @return The normalized object.
         */
        public ConnectorObject NormalizeObject(ConnectorObject orig) {
            return new ConnectorObject(orig.ObjectClass,
                                       NormalizeAttributes(orig.GetAttributes()));
        }
        
        /**
         * Returns the normalized sync delta.
         * @param delta The original delta.
         * @return The normalized delta.
         */
        public SyncDelta NormalizeSyncDelta(SyncDelta delta) {
            SyncDeltaBuilder builder = new
                SyncDeltaBuilder(delta);
            if ( delta.Object != null ) {
                builder.Object=NormalizeObject(delta.Object);
            }
            return builder.Build();
        }
        
        /**
         * Returns a filter consisting of the original with
         * all attributes normalized.
         * @param filter The original.
         * @return The normalized filter.
         */
        public Filter NormalizeFilter(Filter filter) {
            if ( filter is ContainsFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new ContainsFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if (filter is EndsWithFilter) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new EndsWithFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is EqualsFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new EqualsFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is GreaterThanFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new GreaterThanFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is GreaterThanOrEqualFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new GreaterThanOrEqualFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is LessThanFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new LessThanFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is LessThanOrEqualFilter ) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new LessThanOrEqualFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if (filter is StartsWithFilter) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new StartsWithFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if (filter is ContainsAllValuesFilter) {
                AttributeFilter afilter =
                    (AttributeFilter)filter;
                return new ContainsAllValuesFilter(NormalizeAttribute(afilter.GetAttribute()));
            }
            else if ( filter is NotFilter ) {
                NotFilter notFilter =
                    (NotFilter)filter;
                return new NotFilter(NormalizeFilter(notFilter.Filter));
            }
            else if ( filter is AndFilter ) {
                AndFilter andFilter =
                    (AndFilter)filter;
                return new AndFilter(NormalizeFilter(andFilter.Left),
                                     NormalizeFilter(andFilter.Right));
            }
            else if ( filter is OrFilter ) {
                OrFilter orFilter =
                    (OrFilter)filter;
                return new OrFilter(NormalizeFilter(orFilter.Left),
                                    NormalizeFilter(orFilter.Right));
            }
            else {
                return filter;
            }
        }
        
            
    }        
    #endregion
    
    #region SchemaImpl
    internal class SchemaImpl : ConnectorAPIOperationRunner , SchemaApiOp {
        /**
         * Initializes the operation works.
         */
        public SchemaImpl(ConnectorOperationalContext context,
                Connector connector) 
            :base(context,connector) {
        }
    
        /**
         * Retrieve the schema from the {@link Connector}.
         * 
         * @see com.sun.openconnectors.framework.api.operations.SchemaApiOp#schema()
         */
        public Schema Schema() {
            return ((SchemaOp)GetConnector()).Schema();
        }
    }
    #endregion
    
    #region ScriptOnConnectorImpl
    public class ScriptOnConnectorImpl : ConnectorAPIOperationRunner,
            ScriptOnConnectorApiOp {
        
        public ScriptOnConnectorImpl(ConnectorOperationalContext context,
                                     Connector connector) : 
            base(context,connector) {
        }
    
        public Object RunScriptOnConnector(ScriptContext request,
                OperationOptions options) {
            Assertions.NullCheck(request, "request");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            Object rv;
            if ( GetConnector() is ScriptOnConnectorOp ) {
                rv = ((ScriptOnConnectorOp)GetConnector()).RunScriptOnConnector(request, options);
            }
            else {
                String language = request.ScriptLanguage;
                Assembly assembly = GetConnector().GetType().Assembly;
                
                ScriptExecutor executor = 
                    ScriptExecutorFactory.NewInstance(language).NewScriptExecutor(
                            BuildReferenceList(assembly),
                            request.ScriptText,
                            false);
                IDictionary<String,Object> scriptArgs = 
                    new Dictionary<String,Object>(request.ScriptArguments);
                scriptArgs["connector"]=GetConnector(); //add the connector instance itself
                rv = executor.Execute(scriptArgs); 
            }
            return SerializerUtil.CloneObject(rv);
        }
        
        private Assembly [] BuildReferenceList(Assembly assembly)
        {
            List<Assembly> list = new List<Assembly>();
            BuildReferenceList2(assembly,list,new HashSet<string>());
            return list.ToArray();
        }
        
        private void BuildReferenceList2(Assembly assembly,
                                         List<Assembly> list,
                                         HashSet<string> visited) {
            bool notThere = visited.Add(assembly.GetName().FullName);
            if (notThere)
            {
                list.Add(assembly);
                foreach (AssemblyName referenced in assembly.GetReferencedAssemblies()) {
                    Assembly assembly2 = Assembly.Load(referenced);
                    BuildReferenceList2(assembly2,list,visited);
                }
            }
        }
    
    }
    #endregion
    
    #region ScriptOnResourceImpl
    public class ScriptOnResourceImpl : ConnectorAPIOperationRunner,
            ScriptOnResourceApiOp {
        
        public ScriptOnResourceImpl(ConnectorOperationalContext context,
                                    Connector connector) :
            base(context,connector) {
        }
    
        public Object RunScriptOnResource(ScriptContext request,
                OperationOptions options) {
            Assertions.NullCheck(request, "request");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            Object rv
               = ((ScriptOnResourceOp)GetConnector()).RunScriptOnResource(request, options);
            return SerializerUtil.CloneObject(rv);
        }
    
    }
    #endregion
    
    #region SearchImpl
    internal class SearchImpl : ConnectorAPIOperationRunner , SearchApiOp {
    
        /**
         * Initializes the operation works.
         */
        public SearchImpl(ConnectorOperationalContext context,
                Connector connector) 
            :base(context,connector) {
        }
    
        /**
         * Call the SPI search routines to return the results to the
         * {@link ResultsHandler}.
         * 
         * @see SearchApiOp#search(Filter, long, long, SearchApiOp.ResultsHandler)
         */
        public void Search(ObjectClass oclass, Filter originalFilter, ResultsHandler handler, OperationOptions options) {
            Assertions.NullCheck(oclass, "oclass");
            Assertions.NullCheck(handler, "handler");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            ObjectNormalizerFacade normalizer =
                GetNormalizer(oclass);
            //chain a normalizing handler (must come before
            //filter handler)
            handler = 
                new NormalizingResultsHandler(handler,normalizer).Handle;
            Filter normalizedFilter =
                normalizer.NormalizeFilter(originalFilter);
            
            //get the IList interface that this type implements
            Type interfaceType = ReflectionUtil.FindInHierarchyOf
                (typeof(SearchOp<>),GetConnector().GetType());
            Type [] val = interfaceType.GetGenericArguments();
            if (val.Length != 1) {
                throw new Exception("Unexpected type: "+interfaceType);
            }
            Type queryType = val[0];
            Type searcherRawType = typeof(RawSearcherImpl<>);
            Type searcherType = 
                searcherRawType.MakeGenericType(queryType);
            RawSearcher searcher = (RawSearcher)Activator.CreateInstance(searcherType);
            // add filtering handler
            handler = new FilteredResultsHandler(handler, normalizedFilter).Handle;
            // add attributes to get handler
            string[] attrsToGet = options.AttributesToGet;
            if (attrsToGet != null && attrsToGet.Length > 0) {
                handler = new SearchAttributesToGetResultsHandler(
                    handler, attrsToGet).Handle;
            }
            searcher.RawSearch(GetConnector(),oclass,normalizedFilter,handler,options);
        }
    }
    #endregion
    
    #region RawSearcher
    internal interface RawSearcher  {
        /**
         * Public because it is used by TestHelpers. Raw,
         * SPI-level search. 
         * @param search The underlying implementation of search
         *               (generally the connector itself)
         * @param oclass The object class
         * @param filter The filter
         * @param handler The handler
         * @param options The options
         */
        void RawSearch(Object search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options); 
    }
    #endregion
    
    #region RawSearcherImpl
    internal class RawSearcherImpl<T> : RawSearcher where T : class {
        public void RawSearch(Object search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) {
            RawSearch((SearchOp<T>)search,oclass,filter,handler,options);
        }
        
        /**
         * Public because it is used by TestHelpers. Raw,
         * SPI-level search. 
         * @param search The underlying implementation of search
         *               (generally the connector itself)
         * @param oclass The object class
         * @param filter The filter
         * @param handler The handler
         * @param options The options
         */
        public static void RawSearch(SearchOp<T> search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) {
            
            FilterTranslator<T> translator =
                search.CreateFilterTranslator(oclass, options);
            IList<T> queries =
                (IList<T>)translator.Translate(filter);
            if ( queries.Count == 0) {
                search.ExecuteQuery(oclass, 
                        null, handler, options);
            }
            else {
                //eliminate dups if more than one
                bool eliminateDups =
                    queries.Count > 1;
                DuplicateFilteringResultsHandler dups = null;
                if (eliminateDups) {
                    dups = new DuplicateFilteringResultsHandler(handler);
                    handler = dups.Handle;
                }
                foreach( T query in queries ) {
                    search.ExecuteQuery(oclass, 
                            query, handler, options);
                    //don't run any more queries if the consumer
                    //has stopped
                    if (dups != null ) {
                        if (!dups.IsStillHandling) {
                            break;
                        }
                    }
                }
            }             
        }
    
    }
    #endregion

    #region SyncImpl
    public class SyncImpl : ConnectorAPIOperationRunner,
            SyncApiOp {
        
        public SyncImpl(ConnectorOperationalContext context,
                Connector connector) 
                : base(context,connector) {
        }
    
        public void Sync(ObjectClass objClass, SyncToken token,
                SyncResultsHandler handler,
                OperationOptions options) {
            //token is allowed to be null, objClass and handler must not be null
            Assertions.NullCheck(objClass, "objClass");
            Assertions.NullCheck(handler, "handler");
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
            // add a handler in the chain to remove attributes
            string[] attrsToGet = options.AttributesToGet;
            if (attrsToGet != null && attrsToGet.Length > 0) {
                handler = new SyncAttributesToGetResultsHandler(
                    handler, attrsToGet).Handle;
            }
            //chain a normalizing results handler
            ObjectNormalizerFacade normalizer = 
                GetNormalizer(objClass);
            handler = new NormalizingSyncResultsHandler(handler,normalizer).Handle;
            ((SyncOp)GetConnector()).Sync(objClass, token, handler, options);
        }
        public SyncToken GetLatestSyncToken() 
        {
            return ((SyncOp) GetConnector()).GetLatestSyncToken();        
        }    
    }
    #endregion
    
    #region TestImpl
    /**
     * Provides a method for the API to call the SPI's test method on the
     * connector. The test method is intended to determine if the {@link Connector}
     * is ready to perform the various operations it supports.
     * 
     * @author Will Droste
     * 
     */
    internal class TestImpl : ConnectorAPIOperationRunner , TestApiOp {
    
        public TestImpl(ConnectorOperationalContext context, Connector connector) 
            :base(context,connector) {
        }
    
        /**
         * {@inheritDoc}
         */
        public void Test() {
            ((TestOp) GetConnector()).Test();
        }
    
    }
    #endregion
    
    #region UpdateImpl
    /**
     * NOTE: internal class, public only for unit tests
     * Handles both version of update this include simple replace and the advance
     * update.
     */
    public class UpdateImpl : ConnectorAPIOperationRunner , UpdateApiOp {
        /**
         * Static map between API/SPI update types.
         */
        private static readonly IDictionary<UpdateApiType, UpdateType> CONV_TYPE = 
            new Dictionary<UpdateApiType, UpdateType>();
        /**
         * All the operational attributes that can not be added or deleted.
         */
        static readonly HashSet<String> OPERATIONAL_ATTRIBUTE_NAMES = new HashSet<String>();
        
        const String OPERATIONAL_ATTRIBUTE_ERR = 
            "Operational attribute '{0}' can not be added or deleted only replaced.";
        
        static UpdateImpl() {
            CONV_TYPE[UpdateApiType.ADD]= UpdateType.ADD;
            CONV_TYPE[UpdateApiType.DELETE]= UpdateType.DELETE;
            CONV_TYPE[UpdateApiType.REPLACE]= UpdateType.REPLACE;
            OPERATIONAL_ATTRIBUTE_NAMES.Add(Name.NAME);
            CollectionUtil.AddAll(OPERATIONAL_ATTRIBUTE_NAMES,
                                  OperationalAttributes.OPERATIONAL_ATTRIBUTE_NAMES);
        }
    
        /**
         * Determines which type of update a connector supports and then uses that
         * handler.
         */
        public UpdateImpl(ConnectorOperationalContext context,
                Connector connector) 
                :base(context,connector){
        }
        /**
         * Create a new instance of the handler for the type of update the connector
         * can support and run it.
         * 
         * @see UpdateApiOp#update(UpdateApiOp.Type, ConnectorObject)
         */
        public Uid Update(UpdateApiType type, ObjectClass objclass, 
                          ICollection<ConnectorAttribute> attributes,
                          OperationOptions options) {
            // validate all the parameters..
            ValidateInput(type, objclass, attributes);
            //convert null into empty
            if ( options == null ) {
                options = new OperationOptionsBuilder().Build();
            }
    
            Uid ret = null;
            Connector c = GetConnector();
            ObjectNormalizerFacade normalizer =
                GetNormalizer(objclass);
            ICollection<ConnectorAttribute> normalizedAttributes =
                normalizer.NormalizeAttributes(attributes);
            if (c is AdvancedUpdateOp) {
                // easy way its an advance update
                ret = ((AdvancedUpdateOp) c).Update(CONV_TYPE[type], objclass, normalizedAttributes, options);
            } else if (c is UpdateOp) {
                // check that this connector supports Search..
                if (ReflectionUtil.FindInHierarchyOf(typeof(SearchOp<>),c.GetType()) == null) {
                    string MSG = "Connector must support: " + typeof(SearchOp<>);
                    throw new ConfigurationException(MSG);
                }
                // get the connector object from the resource...
                Uid uid = ConnectorAttributeUtil.GetUidAttribute(normalizedAttributes);
                ConnectorObject o = GetConnectorObject(objclass, uid, options);
                if (o == null) {
                    throw new UnknownUidException(uid, objclass);
                }
                // merge the update data..
                ICollection<ConnectorAttribute> mergeAttrs = Merge(type, normalizedAttributes, o.GetAttributes());
                // update the object..
                ret = ((UpdateOp) c).Update(objclass, mergeAttrs, options);
            }
            return (Uid)normalizer.NormalizeAttribute(ret);
        }
    
        /**
         * Merges two connector objects into a single updated object.
         */
        public ICollection<ConnectorAttribute> Merge(UpdateApiType type, 
                ICollection<ConnectorAttribute> updateAttrs,
                ICollection<ConnectorAttribute> baseAttrs) {
            // return the merged attributes
            ICollection<ConnectorAttribute> ret = new HashSet<ConnectorAttribute>();
            IDictionary<String, ConnectorAttribute> baseAttrMap = ConnectorAttributeUtil.ToMap(baseAttrs);
            // run through attributes of the current object..
            foreach (ConnectorAttribute updateAttr in updateAttrs) {
                // ignore uid because its immutable..
                if (updateAttr is Uid) {
                    continue;
                }
                // get the name of the update attributes
                string name = updateAttr.Name;
                ConnectorAttribute baseAttr = CollectionUtil.GetValue(baseAttrMap, name, null);
                ICollection<object> values;
                ConnectorAttribute modifiedAttr; 
                if (UpdateApiType.ADD.Equals(type)) {
                	if (baseAttr == null) {
                		modifiedAttr = updateAttr;
                	} else {
	                    // create a new list with the base attribute to add to..
	                    values = CollectionUtil.NewList(baseAttr.Value);
	                    CollectionUtil.AddAll(values,updateAttr.Value);
	                    modifiedAttr = ConnectorAttributeBuilder.Build(name, values);
                	}
                } else if (UpdateApiType.DELETE.Equals(type)) {
                	if (baseAttr == null) {
                		// nothing to actually do the attribute does not exist
                		continue;
                	} else {
	                    // create a list with the base attribute to remove from..
	                    values = CollectionUtil.NewList(baseAttr.Value);
	                    foreach (Object val in updateAttr.Value) {
	                        values.Remove(val);
	                    }
	                    // if the values are empty send a null to the connector..
	                    if (values.Count == 0) {
	                        modifiedAttr = ConnectorAttributeBuilder.Build(name);
	                    } else {
	                        modifiedAttr = ConnectorAttributeBuilder.Build(name, values);
	                    }
                	}
                } else if (UpdateApiType.REPLACE.Equals(type)) {
                	modifiedAttr = updateAttr;
                } else {
                	throw new ArgumentException("Unknown Type: " + type);
                }
                ret.Add(modifiedAttr);
            }
            // add the rest of the base attributes that were not update attrs
            IDictionary<String, ConnectorAttribute> updateAttrMap = 
            	ConnectorAttributeUtil.ToMap(updateAttrs);
            foreach (ConnectorAttribute a in baseAttrs) {
            	if (!updateAttrMap.ContainsKey(a.Name)) {
            		ret.Add(a);
            	}
            }
           	// always add the UID..
           	ret.Add(updateAttrMap[Uid.NAME]);
            return ret;
        }


        /// <summary>
        /// Get the ConnectorObject that is merged to create the change set
        /// for the SPI simple update.
        /// </summary>
        ConnectorObject GetConnectorObject(ObjectClass oclass, Uid uid, OperationOptions options) {
            // attempt to get the connector object..
            GetApiOp get = new GetImpl(new SearchImpl((ConnectorOperationalContext)GetOperationalContext(),GetConnector()));
            return get.GetObject(oclass, uid, options);
        }
        /// <summary>
        /// Validate all the input to determine if request can be handled.
        /// </summary>
        public static void ValidateInput(UpdateApiType type, ObjectClass objclass, 
                          ICollection<ConnectorAttribute> attrs) {
            Assertions.NullCheck(type, "type");
            Assertions.NullCheck(objclass, "objclass");
            Assertions.NullCheck(attrs, "attrs");
            // check to make sure there's a uid..
            if (ConnectorAttributeUtil.GetUidAttribute(attrs) == null) {
                throw new ArgumentException(
                        "Parameter 'attrs' must contain a 'Uid'!");
            }
            // check for things only valid during ADD/DELETE
            if (UpdateApiType.ADD.Equals(type) || UpdateApiType.DELETE.Equals(type)) {
                foreach (ConnectorAttribute attr in attrs) {
                    Assertions.NullCheck(attr, "attr");
                    // make sure that none of the values are null..
                    if (attr.Value == null) {
                        throw new ArgumentException(
                                "Can not ADD or DELETE 'null' value.");
                    }
                    // make sure that if this an delete/add that it doesn't include
                    // certain attributes because it doesn't make any since..
                    string name = attr.Name;
                    if (OPERATIONAL_ATTRIBUTE_NAMES.Contains(name)) {
                        String msg = String.Format(OPERATIONAL_ATTRIBUTE_ERR, name);
                        throw new ArgumentException(msg);
                    }
                }
            }
        }
    }
    #endregion
    
    #region ValidateImpl
    internal class ValidateImpl : APIOperationRunner, ValidateApiOp {
    
        public ValidateImpl(OperationalContext context) 
            :base(context) {
        }
    
        /**
         * {@inheritDoc}
         */
        public void Validate() {
            GetOperationalContext().GetConfiguration().Validate();
        }
    }
    #endregion
    
}
