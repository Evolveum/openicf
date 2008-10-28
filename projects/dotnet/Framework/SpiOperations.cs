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
using System.Collections.Generic;

using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;

namespace Org.IdentityConnectors.Framework.Spi.Operations
{
    /**
     * Used as a parameter to specify the type of update to perform.
     */
    public enum UpdateType {
        /**
         * Replace each attribute value with the one provided. If the value is
         * <code>null</code> then remove the attribute on set it to
         * <code>null</code> as applicable.
         */
        REPLACE,
        /**
         * Adds the values provided to the existing attribute values on the
         * native target.
         */
        ADD,
        /**
         * Remove the attribute values from the existing target values.
         */
        DELETE
    }

    /**
     * The implementation of this method is expected to handle the following types
     * of update.
     * 
     * @author Will Droste
     * @version $Revision $
     * @since 1.0
     */
    public interface AdvancedUpdateOp : SPIOperation {
    
    
        /**
         * The {@link Connector} developer is responsible for updating the object
         * provided based on the type provided. If the operation can not be
         * accomplished with the information provided throw a type of
         * {@link RuntimeException} that best describes the problem.
         * 
         * @param type
         *            determines the type of update to expect.
         * @param obj
         *            information to find the object and the attributes to perform
         *            the type of update against.
         * @return the {@link Uid} of the updated object in case the update changes
         *         the formation of the unique identifier.
         */
        Uid Update(UpdateType type, ObjectClass objclass, ICollection<ConnectorAttribute> attrs, OperationOptions options);
    }

    /**
     * Authenticate an object based on their unique identifier and password.
     */
    public interface AuthenticateOp : SPIOperation {
    
        /**
         * Simple authentication with two parameters presumed to be user name and
         * password. The {@link Connector} developer is expected to attempt to
         * authenticate these credentials natively. If the authentication fails the
         * developer should throw a type of {@link RuntimeException} either
         * {@link IllegalArgumentException} or if a native exception is available
         * and if its of type {@link RuntimeException} simple throw it. If the
         * native exception is not a {@link RuntimeException} wrap it in one and
         * throw it. This will provide the most detail for logging problem and
         * failed attempts.
         * <p>
         * The developer is of course encourage to try and throw the most
         * informative exception as possible. In that regards there are several
         * exceptions provided in the exceptions package. For instance one of the
         * most common is {@link InvalidPassword}.
         * 
         * @param username
         *            the name based credential for authentication.
         * @param password
         *            the password based credential for authentication.
         * @throws RuntimeException
         *             iff native authentication fails. If a native exception if
         *             available attempt to throw it.
         */
        Uid Authenticate(String username, GuardedString password, OperationOptions options);
    }
    
    /**
     * The {@link Connector} developer is responsible for taking the attributes
     * given (which always includes the {@link ObjectClass}) and create an object
     * and its {@link Uid}. The {@link Connector} developer must return the
     * {@link Uid} so that the caller can refer to the created object.
     * <p>
     * The {@link Connector} developer should make a best effort to create the
     * object otherwise throw an informative {@link RuntimeException} telling the
     * caller why the operation could not be completed. It reasonable to use
     * defaults for required {@link Attribute}s as long as they are documented.
     * 
     * @author Will Droste
     * @version $Revision $
     * @since 1.0
     */
    public interface CreateOp : SPIOperation {
        /**
         * The {@link Connector} developer is responsible for taking the attributes
         * given (which always includes the {@link ObjectClass}) and create an
         * object and its {@link Uid}. The {@link Connector} developer must return
         * the {@link Uid} so that the caller can refer to the created object.
         * 
         * @param name
         *            specifies the name of the object to create.
         * @param attrs
         *            includes all the attributes necessary to create the resource
         *            object including the {@link ObjectClass} attribute.
         * @return the unique id for the object that is created. For instance in
         *         LDAP this would be the 'dn', for a database this would be the
         *         primary key, and for 'ActiveDirectory' this would be the GUID.
         */
        Uid Create(ObjectClass oclass, ICollection<ConnectorAttribute> attrs, OperationOptions options);
    }
    
    /// <summary>
    /// Deletes an object with the specified Uid and ObjectClass on the
    /// resource.
    /// </summary>
    public interface DeleteOp : SPIOperation {
        /// <summary>
        /// Delete the object that the specified Uid identifies (if any).
        /// </summary>
        /// <param name="objectClass">The type of object to delete.</param>
        /// <param name="uid">The unique identitfier for the object to delete.</param>
        /// <exception cref="">Throws UnknowUid if the object does not exist.</exception>
        void Delete(ObjectClass objClass, Uid uid, OperationOptions options);
    }
        
    public interface SchemaOp : SPIOperation {

        /**
         * Determines what types of objects this {@link Connector} supports. This
         * method is considered an operation since determining supported objects may
         * require configuration information and allows this determination to be
         * dynamic.
         * 
         * @return basic schema supported by this {@link Connector}.
         */
        Schema Schema();
    }
    /**
      * Operation that runs a script in the environment of the connector.
     * (Compare to {@link ScriptOnResourceOp}, which runs a script
     * on the target resource that the connector manages.)
     * A connector that intends to <i>provide to scripts
     * more than is required by the basic contract</i>
     * specified in the javadoc for {@link ScriptOnConnectorApiOp} 
     * should implement this interface.
     * <p>
     * Each connector that implements this interface must support 
     * <em>at least</em> the behavior specified by {@link ScriptOnConnectorApiOp}. 
     * A connector also may expose additional variables for use by scripts
     * and may respond to specific {@link OperationOptions options}. 
     * Each connector that implements this interface 
     * must describe in its javadoc as available "for use by connector scripts"
     * any such additional variables or supported options.
     */
    public interface ScriptOnConnectorOp : SPIOperation {
        
        /**
         * Runs the script request.  
         * @param request The script and arguments to run.
         * @param options Additional options that control how the script is
         *  run. 
         * @return The result of the script. The return type must be
         * a type that the framework supports for serialization.
         * See {@link ObjectSerializerFactory} for a list of supported types.
         */
        Object RunScriptOnConnector(ScriptContext request,
                OperationOptions options);
    }
    /**
     * Operation that runs a script directly on a target resource.
     * (Compare to {@link ScriptOnConnectorOp}, which runs a script
     * in the context of a particular connector.)
     * <p>
     * A connector that intends to support 
     * {@link ScriptOnResourceApiOp}
     * should implement this interface.  Each connector that implements
     * this interface must document which script languages the connector supports,
     * as well as any supported {@link OperationOptions}.
      */
     public interface ScriptOnResourceOp : SPIOperation {
        /**
         * Run the specified script <i>on the target resource</i>
         * that this connector manages.  
         * @param request The script and arguments to run.
         * @param options Additional options that control 
         *                  how the script is run.
         * @return The result of the script. The return type must be
         * a type that the framework supports for serialization.
         * See {@link ObjectSerializerFactory} for a list of supported types.
         */
        Object RunScriptOnResource(ScriptContext request,
                OperationOptions options);
    }
    
    /**
     * Implement this interface to allow the Connector to search for resource
     * objects.
     */
    public interface SearchOp<T> : SPIOperation where T : class {
        /**
         * Creates a filter translator that will translate
         * a specified filter to the native filter. The
         * translated filters will be subsequently passed to
         * {@link #search(ObjectClass, Object, ResultsHandler)}
         * @param oclass The object class for the search. Will never be null.
         * @param options
         *            additional options that impact the way this operation is run.
         *            If the caller passes null, the framework will convert this into
         *            an empty set of options, so SPI need not worry
         *            about this ever being null.
         * @return A filter translator.
         */
        FilterTranslator<T> CreateFilterTranslator(ObjectClass oclass, OperationOptions options);
        /**
         * This will be called by ConnectorFacade, once for each native query produced
         * by the FilterTranslator. If there is more than one query the results will
         * automatically be merged together and duplicates eliminated. NOTE
         * that this implies an in-memory data structure that holds a set of
         * Uids, so memory usage in the event of multiple queries will be O(N)
         * where N is the number of results. That is why it is important that
         * the FilterTranslator implement OR if possible.
         * @param oclass The object class for the search. Will never be null.
         * @param query The native query to run. A value of null means 'return everything for the given object class'.
         * @param handler
         *            Results should be returned to this handler
         * @param options
         *            additional options that impact the way this operation is run.
         *            If the caller passes null, the framework will convert this into
         *            an empty set of options, so SPI need not worry
         *            about this ever being null.
         */
        void ExecuteQuery(ObjectClass oclass, T query, ResultsHandler handler, OperationOptions options);
    }
    /**
     * Receive synchronization events from the resource.
     * 
     * @see SyncApiOp
     */
    public interface SyncOp : SPIOperation {
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
         *            If the caller passes null, the framework will convert this into
         *            an empty set of options, so SPI need not worry
         *            about this ever being null.
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
    
    /**
     * The developer of a Connector should implement this interface 
     * if the Connector will allow an authorized caller to update
     * (i.e., modify or replace) objects on the target resource.
     */
    public interface UpdateOp : SPIOperation {
        /// <summary>
        /// Update a particular object based on the ObjectClass and change set provided.
        /// </summary>
        /// <param name="objectclass">Type of object to change.</param>
        /// <param name="changeSet">Deltas for the object which include the Uid so the 
        /// object can be found.</param>
        /// <returns>a new Uid if the deltra prompt a change the Uid otherwise the orginal
        /// one passed in.</returns>
        Uid Update(ObjectClass objectclass, ICollection<ConnectorAttribute> attrs, OperationOptions options);
    }

    public interface TestOp : SPIOperation {

        /**
         * Tests connectivity and validity of the {@link Configuration}.
         * 
         * @throws RuntimeException
         *             iff the {@link Configuration} is not valid or a
         *             {@link Connection} to the resource could not be established.
         */
        void Test();
    }
    
	/**
     * Tagging interface for the {@link Connector} SPI.
     */
    public interface SPIOperation {
    
    }
}
