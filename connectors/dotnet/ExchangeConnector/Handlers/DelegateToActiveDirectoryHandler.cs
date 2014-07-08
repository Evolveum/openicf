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
            Trace.TraceInformation("Delegate...Create called");
            context.Uid = context.Connector.ActiveDirectoryConnector.Create(context.ObjectClass, context.Attributes, context.Options);
        }

        public void Update(UpdateOpContext context)
        {
            context.Connector.ActiveDirectoryConnector.Update(context.ObjectClass, context.Uid, context.Attributes, context.Options);
        }

        public void Delete(DeleteOpContext context)
        {
            context.Connector.ActiveDirectoryConnector.Delete(context.ObjectClass, context.Uid, context.Options);
        }

        public void ExecuteQuery(ExecuteQueryContext context)
        {
            context.Connector.ActiveDirectoryConnector.ExecuteQuery(context.ObjectClass, context.Query, context.ResultsHandler, context.Options);
        }

        public void Sync(SyncOpContext context)
        {
            context.Connector.ActiveDirectoryConnector.Sync(context.ObjectClass, context.SyncToken, context.SyncResultsHandler, context.Options);
        }

        public ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc)
        {
            return connector.ActiveDirectoryConnector.GetObjectClassInfo(oc);
        }

        public FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options)
        {
            return new ActiveDirectoryFilterTranslator();
        }

    }
}
