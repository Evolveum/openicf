﻿using Org.IdentityConnectors.ActiveDirectory;
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
            Command cmdNew = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetNewCommandName()), 
                context.Attributes, context.Connector.Configuration);

            context.Uid = _helper.InvokePipelineAndGetGuid(context.Connector, cmdNew);

            if (ExecuteSetAfterNew()) {
                Command cmdSet = ExchangeUtility.GetCommand(
                    new PSExchangeConnector.CommandInfo(GetSetCommandName()),
                    context.Attributes, context.Uid, context.Connector.Configuration);
                try {
                    _helper.InvokePipeline(context.Connector, cmdSet);
                } catch {
                    // TODO rollback
                    // rethrow original exception
                    throw;
                }
            }
        }

        public void Update(UpdateOpContext context)
        {
            Command cmdSet = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetSetCommandName()), 
                context.Attributes, context.Uid, context.Connector.Configuration);

            _helper.InvokePipeline(context.Connector, cmdSet);
        }

        public void Delete(DeleteOpContext context)
        {
            Command cmdRemove = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetRemoveCommandName()), 
                context.Uid, context.Connector.Configuration);

            _helper.InvokePipeline(context.Connector, cmdRemove);
        }

        public void ExecuteQuery(ExecuteQueryContext context)
        {
            Trace.TraceInformation("SimplePowerShellObjectHandler: Executing query: query={0}", context.Query);

            Command cmdGet = ExchangeUtility.GetCommand(
                new PSExchangeConnector.CommandInfo(GetGetCommandName()), context.Connector.Configuration);
            if (context.Query != null) {
                cmdGet.Parameters.Add("Identity", context.Query);
            }
            ICollection<PSObject> objects = _helper.InvokePipeline(context.Connector, cmdGet);
            Trace.TraceInformation("SimplePowerShellObjectHandler: Executing query: got {0} objects", objects.Count);
            foreach (PSObject psobject in objects) {
                if (psobject != null) {
                    context.ResultsHandler.Handle(_helper.CreateConnectorObject(context.Connector, psobject, context.ObjectClass));
                }
            }
        }

        public void Sync(SyncOpContext context)
        {
            throw new NotImplementedException("Sync is not implemented for this kind of objects.");
        }

        public ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc)
        {
            return connector.ActiveDirectoryConnector.GetObjectClassInfo(oc);       // TODO
        }

        public FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options)
        {
            return new IdentityFilterTranslator();
        }
    }
}
