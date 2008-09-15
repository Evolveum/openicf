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
using System.Reflection;
using System.Globalization;
using System.Collections.Generic;

using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;

namespace Org.IdentityConnectors.Framework.Api.Operations
{
    /**
     * Base interface for all API operations.
     */
    public interface APIOperation {
    }

    public interface AuthenticationApiOp : APIOperation {

        /**
         * Most basic authentication available.
         * 
         * @param username
         *            string that represents the account or user id.
         * @param password
         *            string that represents the password for the account or user.
         * @throws RuntimeException
         *             iff the credentials do not pass authentication otherwise
         *             nothing.
         */
        void Authenticate(string username, GuardedString password, OperationOptions options);
    }

    /// <summary>
    /// Operation to create connector objects based on the attributes provided.
    /// </summary>
    public interface CreateApiOp : APIOperation {

    	/// <summary>
    	/// Creates a user based on the ConnectorAttributes provide. The only
        /// required attribute is the ObjectClass and those required by the
        /// Connector. The API will validate the existence of the
        /// ObjectClass attribute and that there are no duplicate name'd
        /// attributes.
    	/// </summary>
    	/// <param name="attrs">ConnectorAttribtes to create the object.</param>
    	/// <returns>Unique id for the created object.</returns>
        Uid Create(ObjectClass oclass, ICollection<ConnectorAttribute> attrs, OperationOptions options);
    }

    /// <summary>
    /// Deletes an object with the specified Uid and ObjectClass on the
    /// resource.
    /// </summary>
    public interface DeleteApiOp : APIOperation {
        /// <summary>
        /// Delete the object that the specified Uid identifies (if any).
        /// </summary>
        /// <param name="objectClass">The type of object to delete.</param>
        /// <param name="uid">The unique identitfier for the object to delete.</param>
        /// <exception cref="">Throws UnknowUid if the object does not exist.</exception>
        void Delete(ObjectClass objectClass, Uid uid, OperationOptions options);
    }

    /**
     * Get a particular {@link ConnectorObject} based on the {@link Uid}.
     */
    public interface GetApiOp : APIOperation {

        /**
         * Get a particular {@link ConnectorObject} based on the {@link Uid}.
         * 
         * @param uid
         *            the unique id of the object that to get.
         * @return {@link ConnectorObject} based on the {@link Uid} provided.
         */
        ConnectorObject GetObject(ObjectClass objClass, Uid uid, OperationOptions options);
    }

    /**
     * Get the schema from the {@link Connector}.
     */
    public interface SchemaApiOp : APIOperation {
        /**
         * Retrieve the basic schema of this {@link Connector}.
         */
        Schema Schema();
    }

    
    public interface SearchApiOp : APIOperation {

        /**
         * Search the resource for all objects that match the filter.
         * 
         * @param filter
         *            Reduces the number of entries to only those that match the
         *            {@link Filter} provided.
         * @param handler
         *            class responsible for working with the objects returned from
         *            the search.
         * @throws RuntimeException
         *             iff there is problem during the processing of the results.
         */
        void Search(ObjectClass oclass, Filter filter, ResultsHandler handler, OperationOptions options);
    }
    
    /**
     * Runs a script in the same JVM or .Net Runtime as the <code>Connector</code>.
     * That is, if you are using a <b>local</b> framework, the script will be
     * run in your JVM. If you are connected to a <b>remote</b> framework, the
     * script will be run in the remote JVM or .Net Runtime.
     * <p>
     * This API allows an application to run a script in the context
     * of any connector.  (A connector need not implement any particular interface
     * in order to enable this.)  The <b>minimum contract</b> to which each connector
     * <b>must</b> adhere is as follows:
     * <ol>
     *    <li>Script will run in the same classloader/execution environment
     *    as the connector, so the script will have access to all the classes
     *    to which the connector has access.
     *    </li>
     *    <li>Script will have access to a <code>"connector"</code> variable 
     *    that is equivalent to an initialized instance of a connector. 
     *    Thus, at a minimum the script will be able to access 
     *    {@link Connector#getConfiguration() the configuration of the connector}.
     *    </li>
     *    <li>Script will have access to any 
     *    {@link ScriptContext#getScriptArguments() script-arguments}
     *    passed in by the application.
     *    </li>
     * </ol>
     * <p>
     * A connector that implements {@link ScriptOnConnectorOp} 
     * may provide more variables than what is described above. 
     * A connector also may perform special processing
     * for {@link OperationOptions} specific to that connector.
     * Consult the javadoc of each particular connector to find out what
     * additional capabilities, if any, that connector exposes for use in scripts. 
     * <p>
     * <b>NOTE:</b> A caller who wants to execute scripts on a connector
     * should assume that <em>a script must not use any method of the connector 
     * beyond the minimum contract described above</em>,
     * unless the connector explicitly documents that method as 
     * "for use by connector script".  The primary function of a connector 
     * is to implement the SPI in the context of the Connector framework.  
     * In general, no caller should invoke Connector methods directly
     * --whether by a script or by other means.
     */
    public interface ScriptOnConnectorApiOp : APIOperation {
        
        /**
         * Runs the script.  
         * @param request - The script and arguments to run.
         * @param options - Additional options that control how the script is
         *  run. The framework does not currently recognize any options
         *  but specific connectors might. Consult the documentation
         *  for each connector to identify supported options.
         * @return The result of the script. The return type must be
         * a type that the framework supports for serialization.
         * @see ObjectSerializerFactory for a list of supported return types.
         */
        Object RunScriptOnConnector(ScriptContext request,
                OperationOptions options);
    }
    /**
     * Runs a script on the target resource that a connector manages.
     * This API operation is supported only for a connector that implements
     * {@link ScriptOnResourceOp}. 
     * <p>
     * The contract here at the API level is intentionally very loose.  
     * Each connector decides what script languages it supports, 
     * what running a script <b>on</b> a target resource actually means, 
     * and what script options (if any) that connector supports.
     * Refer to the javadoc of each particular connector for more information.
     */
    public interface ScriptOnResourceApiOp : APIOperation {
        
        /**
         * Runs a script on a specific target resource.  
         * @param request The script and arguments to run.
         * @param options Additional options which control how the script is
         *  run. Please refer to the connector documentation for supported 
         *  options.
         * @return The result of the script. The return type must be
         * a type that the connector framework supports for serialization.
         * See {@link ObjectSerializerFactory} for a list of supported return types.
         */
        Object RunScriptOnResource(ScriptContext request,
                OperationOptions options);
    }
    /**
     * Receive synchronization events from the resource. This will be supported by
     * connectors that implement {@link SyncOp}.
     * <p>
     * TODO: define quality of service level. For example, on JMS sync, when
     * synchronizing from a queue, the connector should return one SyncDelta
     * per-call to {@link #sync(ObjectClass, SyncToken, SyncResultsHandler)}. Each
     * call to {@link #sync(ObjectClass, SyncToken, SyncResultsHandler)} should
     * delete the previous entry and return the next. That would guarantee that no
     * items get dropped.
     * 
     * @see SyncOp
     */
    public interface SyncApiOp : APIOperation {
        /**
         * Perform a synchronization.
         * 
         * @param objClass
         *            The object class to synchronize. Must not be null.
         * @param token
         *            The token representing the last token from the previous sync.
         *            Should be null if this is the first sync for the given
         *            resource.
         * @param handler
         *            The result handler Must not be null.
         * @param options
         *            additional options that impact the way this operation is run.
         *            May be null.
         */
        void Sync(ObjectClass objClass, SyncToken token,
                SyncResultsHandler handler,
                OperationOptions options);
        /**
         * Returns the token corresponding to the latest sync delta.
         * This is to support applications that may wish to sync starting
         * "now". 
         * @return The latest token or null if there is no sync data.
         */
        SyncToken GetLatestSyncToken();
    }
    
    ///
    /// Determines the type of update to perform.
    ///
    public enum UpdateApiType {
      /**
       * Replace each attribute value with the one provided.
       */
      REPLACE,
      /**
       * Added the values provided to the existing attribute values on the
       * native target.
       */
      ADD,
      /**
       * Remove the attribute values from the existing target values.
       */
      DELETE
    }
    
    public interface UpdateApiOp : APIOperation {
        /**
         * Update the object specified. The type is used to determine if the updates
         * are additive, subtractive, or replacement of values provided.
         * 
         * @param type
         *            determines the type of update to expect.
         * @param obj
         *            information to find the object and the attributes to perform
         *            the type of update against.
         * @return the {@link Uid} of the updated object in case the update changes
         *         the formation of the unique identifier.
         */
        Uid Update(UpdateApiType type, ObjectClass objectclass, 
                   ICollection<ConnectorAttribute> attrs, OperationOptions options);

    }
 
    public interface ValidateApiOp : APIOperation {
        /**
         * Tests connectivity and validity of the {@link Configuration}.
         * 
         * @throws RuntimeException
         *             iff the {@link Configuration} is not valid or a
         *             {@link Connection} to the resource could not be established.
         */
        void Validate();
    }

    public interface TestApiOp : APIOperation {
        /**
         * Tests connectivity and validity of the {@link Configuration}.
         * 
         * @throws RuntimeException
         *             iff the {@link Configuration} is not valid or a
         *             {@link Connection} to the resource could not be established.
         */
        void Test();
    }
}
