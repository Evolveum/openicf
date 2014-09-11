using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Management.Automation;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using SortKey = Org.IdentityConnectors.Framework.Common.Objects.SortKey;

namespace MsPowerShellTestModule
{
    [Cmdlet(VerbsCommon.New, "ConnectorObjectCache")]
    public class CreateConnectorObject : PSCmdlet
    {
        [Parameter(Position = 0, Mandatory = true)]
        public ConnectorObject ConnectorObject;

        protected override void ProcessRecord()
        {
            var uid = ObjectCacheLibrary.Instance.Create(ConnectorObject);
            WriteObject(uid);
        }
    }

    [Cmdlet(VerbsCommon.Get, "ConnectorObjectCache")]
    public class GetConnectorObject : PSCmdlet
    {
        [Parameter(Position = 0, Mandatory = true)]
        public ObjectClass ObjectClass;

        [Parameter(Position = 1, Mandatory = true)]
        public Uid Uid;

        protected override void ProcessRecord()
        {
            var pair = ObjectCacheLibrary.Instance.Get(ObjectClass, Uid);
            WriteObject(pair);
        }
    }

    [Cmdlet(VerbsCommon.Set, "ConnectorObjectCache")]
    public class UpdateConnectorObject : PSCmdlet
    {
        [Parameter(Position = 0, Mandatory = true)]
        public ConnectorObject ConnectorObject;

        protected override void ProcessRecord()
        {
            var uid = ObjectCacheLibrary.Instance.Update(ConnectorObject);
            WriteObject(uid);
        }
    }

    [Cmdlet(VerbsCommon.Search, "ConnectorObjectCache")]
    public class SearchConnectorObject : PSCmdlet
    {
        [Parameter(Mandatory = true)]
        public ObjectClass ObjectClass;

        [Parameter]
        public Filter Query;

        [Parameter]
        public SortKey[] SortKeys;

        protected override void ProcessRecord()
        {
            var co = ObjectCacheLibrary.Instance.Search(ObjectClass, Query, SortKeys);
            if (co != null)
                foreach (var obj in co)
                {
                    WriteObject(obj);
                }
        }
    }


    [Cmdlet(VerbsCommon.Remove, "ConnectorObjectCache")]
    public class DeleteConnectorObject : PSCmdlet
    {
        [Parameter(Mandatory = true)]
        public ObjectClass ObjectClass;

        [Parameter(Mandatory = true)] 
        public Uid Uid;

        protected override void ProcessRecord()
        {
            ObjectCacheLibrary.Instance.Delete(ObjectClass, Uid);
        }
    }

    public class ObjectCacheLibrary
    {
        private static readonly IComparer<object> ValueComparator = new ComparatorAnonymousInnerClassHelper();

        private readonly
            ConcurrentDictionary<ObjectClass, ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>>> _store =
                new ConcurrentDictionary<ObjectClass, ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>>>();

        private static volatile ObjectCacheLibrary _instance;
        private static object syncRoot = new Object();

        public static ObjectCacheLibrary Instance
        {
            get
            {
                if (_instance == null)
                {
                    lock (syncRoot)
                    {
                        if (_instance == null)
                            _instance = new ObjectCacheLibrary();
                    }
                }

                return _instance;
            }
        }

        private ObjectCacheLibrary()
        {
            _store.TryAdd(ObjectClass.ACCOUNT, new ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>>());
            _store.TryAdd(ObjectClass.GROUP, new ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>>());
        }

        private static int CompareValues(object v1, object v2)
        {
            if (v1 is string && v2 is string)
            {
                var s1 = (string)v1;
                var s2 = (string)v2;

                return String.Compare(s1, s2, StringComparison.InvariantCultureIgnoreCase);
            }

            double n1;
            double n2;
            if (
                Double.TryParse(Convert.ToString(v1, CultureInfo.InvariantCulture), NumberStyles.Any,
                    NumberFormatInfo.InvariantInfo, out n1) &&
                Double.TryParse(Convert.ToString(v2, CultureInfo.InvariantCulture), NumberStyles.Any,
                    NumberFormatInfo.InvariantInfo, out n2))
            {
                return Math.Sign(n1.CompareTo(n2));
            }
            if (v1 is Boolean && v2 is Boolean)
            {
                var b1 = (Boolean)v1;
                var b2 = (Boolean)v2;
                return b1.CompareTo(b2);
            }
            return String.Compare(v1.GetType().FullName, v2.GetType().FullName, StringComparison.Ordinal);
        }

        private ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> GetStore(ObjectClass type)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> result;
            if (_store.TryGetValue(type, out result))
            {
                return result;
            }
            throw new InvalidAttributeValueException("Unsupported ObjectClass" + type);
        }

        internal virtual Uid Create(ConnectorObject connectorObject)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> storage = GetStore(connectorObject.ObjectClass);
            Pair<ConnectorObject, DateTime> item = Pair<ConnectorObject, DateTime>.Of(connectorObject, DateTime.Now);
            if (storage.ContainsKey(connectorObject.Uid.GetUidValue()))
            {
                throw (new AlreadyExistsException()).InitUid(connectorObject.Uid);
            }
            storage.TryAdd(connectorObject.Uid.GetUidValue(), item);
            return new Uid(connectorObject.Uid.GetUidValue(), Convert.ToString(item.Second.ToLongTimeString()));
        }

        internal virtual SortedSet<ConnectorObject> Search(ObjectClass objectClass, Filter query, SortKey[] sortKeys)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> storage = GetStore(objectClass);

            SortKey[] s;
            if (null == sortKeys || !sortKeys.Any())
            {
                s = new[] { new SortKey(Name.NAME, true) };
            }
            else
            {
                s = sortKeys;
            }

            // Rebuild the full result set.
            var resultSet = new SortedSet<ConnectorObject>(new ResourceComparator(s));

            foreach (var co in storage.Values)
            {
                if (null == query || query.Accept(co.First))
                {
                    resultSet.Add(co.First);
                }
            }
            return resultSet;
        }

        internal virtual Uid Update(ConnectorObject connectorObject)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> storage = GetStore(connectorObject.ObjectClass);

            Pair<ConnectorObject, DateTime> old;

            if (storage.TryGetValue(connectorObject.Uid.GetUidValue(), out old))
            {
                if (null != connectorObject.Uid.Revision &&
                    !connectorObject.Uid.Revision.Equals(Convert.ToString(old.Second.ToLongTimeString())))
                {
                    throw new PreconditionFailedException();
                }
                if (null == connectorObject.Uid.Revision)
                {
                    throw new PreconditionRequiredException();
                }
                Pair<ConnectorObject, DateTime> item = Pair<ConnectorObject, DateTime>.Of(connectorObject, DateTime.Now);
                if (!storage.TryUpdate(connectorObject.Uid.GetUidValue(), item, old))
                {
                    throw new PreconditionFailedException();
                }
                return new Uid(connectorObject.Uid.GetUidValue(), Convert.ToString(item.Second.ToLongTimeString()));
            }
            throw new UnknownUidException(connectorObject.Uid, connectorObject.ObjectClass);
        }

        internal virtual void Delete(ObjectClass objectClass, Uid uid)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> storage = GetStore(objectClass);
            Pair<ConnectorObject, DateTime> removedValue;
            if (!storage.TryRemove(uid.GetUidValue(), out removedValue))
            {
                throw new UnknownUidException(uid, objectClass);
            }
        }

        internal virtual Pair<ConnectorObject, DateTime> Get(ObjectClass objectClass, Uid uid)
        {
            ConcurrentDictionary<string, Pair<ConnectorObject, DateTime>> storage = GetStore(objectClass);
            Pair<ConnectorObject, DateTime> entry;
            if (!storage.TryGetValue(uid.GetUidValue(), out entry))
            {
                throw new UnknownUidException(uid, objectClass);
            }
            return entry;
        }


        private class ComparatorAnonymousInnerClassHelper : IComparer<object>
        {
            public virtual int Compare(object o1, object o2)
            {
                return CompareValues(o1, o2);
            }
        }

        private sealed class ResourceComparator : IComparer<ConnectorObject>
        {
            private readonly IList<SortKey> _sortKeys;


            internal ResourceComparator(IList<SortKey> sortKeys)
            {
                _sortKeys = sortKeys;
            }

            public int Compare(ConnectorObject r1, ConnectorObject r2)
            {
                //var sel = _sortKeys.Select(sortKey => Compare0(r1, r2, sortKey));
                //var f = sel.FirstOrDefault(result => result != 0);
                //return f;

                foreach (var sortkey in _sortKeys)
                {
                    var result = Compare0(r1, r2, sortkey);
                    if (result != 0)
                    {
                        return result;
                    }
                }
                return 0;
            }


            private int Compare0(ConnectorObject r1, ConnectorObject r2, SortKey sortKey)
            {
                IList<object> vs1 = GetValuesSorted(r1, sortKey.Field);
                IList<object> vs2 = GetValuesSorted(r2, sortKey.Field);
                if (vs1.Count == 0 && vs2.Count == 0)
                {
                    return 0;
                }
                if (vs1.Count == 0)
                {
                    // Sort resources with missing attributes last.
                    return 1;
                }
                if (vs2.Count == 0)
                {
                    // Sort resources with missing attributes last.
                    return -1;
                }
                object v1 = vs1[0];
                object v2 = vs2[0];
                return sortKey.IsAscendingOrder() ? CompareValues(v1, v2) : -CompareValues(v1, v2);
            }


            private IList<object> GetValuesSorted(ConnectorObject resource, string field)
            {
                ConnectorAttribute value = ConnectorAttributeUtil.Find(field, resource.GetAttributes());
                if (value == null || value.Value == null || value.Value.Count == 0)
                {
                    return CollectionUtil.NullAsEmpty<object>(null);
                }
                if (value.Value.Count > 1)
                {
                    var results = new List<object>(value.Value);
                    results.Sort(ValueComparator);
                    return results;
                }
                return value.Value;
            }
        }
    }
}