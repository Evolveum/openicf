using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    class DelegateToActiveDirectoryHandler : ObjectClassHandler
    {
        public void Create(CreateOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            context.Uid = adconn.Create(context.ObjectClass, context.Attributes, context.Options);
        }

        public void Update(UpdateOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            adconn.Update(context.UpdateType, context.ObjectClass, context.Uid, context.Attributes, context.Options);
        }

        public void Delete(DeleteOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            adconn.Delete(context.ObjectClass, context.Uid, context.Options);
        }

        public void ExecuteQuery(ExecuteQueryContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            adconn.ExecuteQuery(context.ObjectClass, context.Query, context.ResultsHandler, context.Options);
        }

        public void Sync(SyncOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            adconn.Sync(context.ObjectClass, context.SyncToken, context.SyncResultsHandler, context.Options);
        }

        public ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc)
        {
            return connector.GetObjectClassInfoGeneric(oc);
        }

        public FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options)
        {
            return new ActiveDirectoryFilterTranslator();
        }

    }
}
