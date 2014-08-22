using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    abstract class SimplePowerShellObjectHandler : ObjectClassHandler
    {
        private MiscHelper _helper = new MiscHelper();

        abstract internal string GetObjectClassName();

        virtual internal string GetNewCommandName() {
            return "New-" + GetObjectClassName();
        }

        virtual internal string GetSetCommandName() {
            return "Set-" + GetObjectClassName();
        }

        virtual internal string GetRemoveCommandName() {
            return "Remove-" + GetObjectClassName();
        }

        virtual internal string GetGetCommandName() {
            return "Get-" + GetObjectClassName();
        }

        virtual internal bool ExecuteSetAfterNew() {
            return false;
        }

        public void Create(CreateOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;

            Command cmdNew = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetNewCommandName()), 
                context.Attributes, exconn.Configuration);

            context.Uid = _helper.InvokePipelineAndGetGuid(exconn, cmdNew);

            if (ExecuteSetAfterNew()) {
                Command cmdSet = ExchangeUtility.GetCommand(
                    new PSExchangeConnector.CommandInfo(GetSetCommandName()),
                    context.Attributes, context.Uid, exconn.Configuration);
                try {
                    _helper.InvokePipeline(exconn, cmdSet);
                } catch {
                    // TODO rollback
                    // rethrow original exception
                    throw;
                }
            }
        }

        public void Update(UpdateOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;

            Command cmdSet = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetSetCommandName()), 
                context.Attributes, context.Uid, exconn.Configuration);

            try {
                _helper.InvokePipeline(exconn, cmdSet);
            } catch (ObjectNotFoundException e) {
                throw new UnknownUidException("Object with UID " + context.Uid.GetUidValue() + " couldn't be modified", e);
            }
        }

        public void Delete(DeleteOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;

            Command cmdRemove = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetRemoveCommandName()), 
                context.Uid, exconn.Configuration);

            try {
                _helper.InvokePipeline(exconn, cmdRemove);
            } catch (ObjectNotFoundException e) {
                throw new UnknownUidException("Object with UID " + context.Uid.GetUidValue() + " couldn't be deleted", e);
            }
        }

        public void ExecuteQuery(ExecuteQueryContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;

            Trace.TraceInformation("SimplePowerShellObjectHandler: Executing query: query={0}", context.Query);

            Command cmdGet = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetGetCommandName()), exconn.Configuration);
            if (context.Query != null) {
                cmdGet.Parameters.Add("Identity", context.Query);
            }
            ICollection<PSObject> objects;
            try {
                objects = _helper.InvokePipeline(exconn, cmdGet);
            } catch (ObjectNotFoundException e) {
                Trace.TraceInformation("SimplePowerShellObjectHandler: Executing query: got 'ObjectNotFound' exception ({0}), assuming suitable objects do not exist", e);
                return;
            }
            Trace.TraceInformation("SimplePowerShellObjectHandler: Executing query: got {0} objects", objects.Count);
            foreach (PSObject psobject in objects) {
                if (psobject != null) {
                    context.ResultsHandler.Handle(_helper.CreateConnectorObject(exconn, psobject, context.ObjectClass));
                }
            }
        }

        public void Sync(SyncOpContext context)
        {
            throw new NotImplementedException("Sync is not implemented for this kind of objects.");
        }

        public ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc)
        {
            return connector.GetObjectClassInfoGeneric(oc);
        }

        public FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options)
        {
            return new IdentityFilterTranslator();
        }
    }
}
