using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    internal interface ObjectClassHandler
    {
        void Create(CreateOpContext context);
        void Update(UpdateOpContext context);
        void Delete(DeleteOpContext context);
        void ExecuteQuery(ExecuteQueryContext context);
        void Sync(SyncOpContext context);
        ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc);

        FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options);
    }
}
