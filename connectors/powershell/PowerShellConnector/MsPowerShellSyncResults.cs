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
using System.Collections.ObjectModel;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    class MsPowerShellSyncResults
    {
        private readonly ObjectClass _objectClass;
        private readonly SyncResultsHandler _handler;

        public MsPowerShellSyncResults(ObjectClass objectClass, SyncResultsHandler handler)
        {
            _objectClass = objectClass;
            _handler = handler;
        }

        /// <summary>
        /// Complete() should be called when a token needs to be passed as a watermark,
        /// without any special object to sync.
        /// </summary>
        /// <param name="newToken"></param>
        public void Complete(Object newToken)
        {
            if (newToken is SyncToken)
                ((SyncTokenResultsHandler)_handler).HandleResult(newToken as SyncToken);
            else if (newToken != null) 
                ((SyncTokenResultsHandler)_handler).HandleResult(new SyncToken(newToken));
        }

        /// <summary>
        /// Process/handle the SyncDelta result
        /// </summary>
        /// <param name="result"></param>
        /// <returns></returns>
        public Object Process(SyncDelta result)
        {
            return _handler.Handle(result);
        }

        /// <summary>
        /// Process the Hashtable result and convert it to a SyncDelta object
        /// ready to be processed by the sync handler 
        /// </summary>
        /// <remarks>
        /// The result Hashtable must follow a specific format and contain the following key/value:
        /// 
        /// "Token": (Object) token object (could be Integer, Date, String), [!! could be null]
        /// "Operation": (String) ("CREATE|UPDATE|CREATE_OR_UPDATE"|"DELETE"),
        /// "Uid": (String) uid  (uid of the entry),
        /// "PreviousUid": (String) previous uid (This is for rename ops),
        /// "Object": Hashtable(String,List) of attributes name/values describing the object
        /// </remarks>
        /// <param name="result"></param>
        /// <returns></returns>
        public Object Process(Hashtable result)
        {
            var syncbld = new SyncDeltaBuilder();
            var cobld = new ConnectorObjectBuilder();
            Uid uid;

            // SyncToken
            // Mandatory here
            if (result.ContainsKey("SyncToken"))
            {
                syncbld.Token = result["SyncToken"] == null ? new SyncToken(0L) : new SyncToken(result["SyncToken"]);
            }
            else
                throw new ArgumentException("SyncToken is missing in Sync result");

            // SyncDelta
            // Mandatory here
            if (result.ContainsKey("DeltaType") && result["DeltaType"] != null && !"".Equals(result["DeltaType"]))
            {
                var op = result["DeltaType"];
                if ("CREATE".Equals(op))
                    syncbld.DeltaType = SyncDeltaType.CREATE;
                else if ("UPDATE".Equals(op))
                    syncbld.DeltaType = SyncDeltaType.UPDATE;
                else if ("DELETE".Equals(op))
                    syncbld.DeltaType = SyncDeltaType.DELETE;
                else if ("CREATE_OR_UPDATE".Equals(op))
                    syncbld.DeltaType = SyncDeltaType.CREATE_OR_UPDATE;
                else
                    throw new ArgumentException("Unrecognized DeltaType in Sync result");
            }
            else
                throw new ArgumentException("DeltaType is missing in Sync result");

            // Uid 
            // Mandatory
            if (result.ContainsKey("Uid") && result["Uid"] != null && !"".Equals(result["uid"]))
            {
                uid = new Uid((String) result["Uid"]);
                syncbld.Uid = uid;
                cobld.SetUid(uid);
            }
            else
                throw new ArgumentException("Uid is missing in Sync result");


            // PreviousUid 
            // Not valid if DELETE
            if (result.ContainsKey("PreviousUid") && result["PreviousUid"] != null && !"".Equals(result["uid"]))
                    syncbld.PreviousUid = new Uid((String)result["PreviousUid"]);

            if (syncbld.PreviousUid != null && syncbld.DeltaType == SyncDeltaType.DELETE)
                throw new ArgumentException("PreviousUid can only be specified for Create or Update.");

            // Connector object
            // Mandatory
            if (result.ContainsKey("Object") && result["Object"] is Hashtable)
            {
                var attrs = result["Object"] as Hashtable;

                if (!attrs.ContainsKey(Name.NAME))
                    throw new ArgumentException("The Object must contain a Name");

                foreach (DictionaryEntry attr in attrs)
                {
                    var attrName = attr.Key as String;
                    var attrValue = attr.Value;

                    if (Name.NAME.Equals(attrName))
                        cobld.SetName(attrValue as String);
                    else if (Uid.NAME.Equals((attrName)))
                    {
                        if (!uid.GetUidValue().Equals(attrValue))
                            throw new ArgumentException("Uid from Object is different than Uid from Sync result");
                    }
                    else if (OperationalAttributes.ENABLE_NAME.Equals((attrName)))
                        cobld.AddAttribute(ConnectorAttributeBuilder.BuildEnabled(attr.Value is bool && (bool) attr.Value));
                    else
                    {
                        if (attrValue == null)
                        {
                            cobld.AddAttribute(ConnectorAttributeBuilder.Build(attrName));
                        }
                        else if (attrValue.GetType() == typeof(Object[]) || attrValue.GetType() == typeof(System.Collections.ICollection))
                        {
                            var list = new Collection<object>();
                            foreach (var val in (ICollection)attrValue)
                            {
                                list.Add(FrameworkUtil.IsSupportedAttributeType(val.GetType()) ? val : val.ToString());
                            }
                            cobld.AddAttribute(ConnectorAttributeBuilder.Build(attrName, list));
                        }
                        else
                        {
                            cobld.AddAttribute(ConnectorAttributeBuilder.Build(attrName, attrValue));
                        }
                    }
                }
                cobld.ObjectClass = _objectClass;
                syncbld.Object = cobld.Build();
            }
            else if (SyncDeltaType.DELETE != syncbld.DeltaType)
                throw new ArgumentException("Object is missing in Sync result");

            return _handler.Handle(syncbld.Build());
        }
    }
}
