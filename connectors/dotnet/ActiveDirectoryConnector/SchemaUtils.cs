using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;

namespace Org.IdentityConnectors.ActiveDirectory
{
    public class SchemaUtils {

        // tracing (using ActiveDirectoryConnector's name!)
        internal static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        public delegate ICollection<ObjectClass> GetSupportedObjectClassesDelegate();
        public delegate ObjectClassInfo GetObjectClassInfoDelegate(ObjectClass oc);
        public delegate IList<SafeType<SPIOperation>> GetSupportedOperationsDelegate(ObjectClass oc);
        public delegate IList<SafeType<SPIOperation>> GetUnSupportedOperationsDelegate(ObjectClass oc);

        public static Schema BuildSchema(Connector connector,
            GetSupportedObjectClassesDelegate getSupportedObjectClassesDelegate,
            GetObjectClassInfoDelegate getObjectClassInfoDelegate,
            GetSupportedOperationsDelegate getSupportedOperationsDelegate,
            GetUnSupportedOperationsDelegate getUnSupportedOperationsDelegate) {

            SchemaBuilder schemaBuilder = new SchemaBuilder(SafeType<Connector>.Get(connector));

            //iterate through supported object classes
            foreach (ObjectClass oc in getSupportedObjectClassesDelegate()) {
                ObjectClassInfo ocInfo = getObjectClassInfoDelegate(oc);
                Assertions.NullCheck(ocInfo, "ocInfo");

                //add object class to schema
                schemaBuilder.DefineObjectClass(ocInfo);

                //add supported operations
                IList<SafeType<SPIOperation>> supportedOps = getSupportedOperationsDelegate(oc);
                if (supportedOps != null) {
                    foreach (SafeType<SPIOperation> op in supportedOps) {
                        schemaBuilder.AddSupportedObjectClass(op, ocInfo);
                    }
                }

                //remove unsupported operatons
                IList<SafeType<SPIOperation>> unSupportedOps = getUnSupportedOperationsDelegate(oc);
                if (unSupportedOps != null) {
                    foreach (SafeType<SPIOperation> op in unSupportedOps) {
                        schemaBuilder.RemoveSupportedObjectClass(op, ocInfo);
                    }
                }
            }
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Finished retrieving schema");
            return schemaBuilder.Build();
        }

        public static IDictionary<ObjectClass, ICollection<string>> GetAttributesReturnedByDefault(
            GetSupportedObjectClassesDelegate getSupportedObjectClassesDelegate,
            GetObjectClassInfoDelegate getObjectClassInfoDelegate) {
            var attributesReturnedByDefault = new Dictionary<ObjectClass, ICollection<string>>();

            //iterate through supported object classes
            foreach (ObjectClass oc in getSupportedObjectClassesDelegate()) {
                ObjectClassInfo ocInfo = getObjectClassInfoDelegate(oc);
                Assertions.NullCheck(ocInfo, "ocInfo");

                //populate the list of default attributes to get
                attributesReturnedByDefault.Add(oc, new HashSet<string>());
                foreach (ConnectorAttributeInfo caInfo in ocInfo.ConnectorAttributeInfos) {
                    if (caInfo.IsReturnedByDefault) {
                        attributesReturnedByDefault[oc].Add(caInfo.Name);
                    }
                }
            }
            return attributesReturnedByDefault;
        }
    }
}
