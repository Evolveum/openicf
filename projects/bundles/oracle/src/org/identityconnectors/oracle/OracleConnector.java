package org.identityconnectors.oracle;

import java.util.Set;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Implementation of Oracle DB Connector.
 * It delegates to real implementation of Oracle connector, but before create and update, it normalizes attributes.
 * We do not use {@link AttributeNormalizer} , because we want just to somehow create/update user with some default case, but we want to return back attributes
 * as stored natively in DB
 * @author kitko
 *
 */
@ConnectorClass(configurationClass=OracleConfiguration.class,
        displayNameKey = OracleMessages.MSG_CONNECTOR_DISPLAY,
        messageCatalogPaths={"org/identityconnectors/dbcommon/Messages","org/identityconnectors/oracle/Messages"})
public final class OracleConnector implements PoolableConnector, AuthenticateOp,
CreateOp, DeleteOp, UpdateOp, UpdateAttributeValuesOp,
SearchOp<Pair<String, FilterWhereBuilder>>, SchemaOp,TestOp{

	private OracleConnectorImpl connector;
	
	public OracleConnector(){
		this.connector = new OracleConnectorImpl();
	}

	public Uid addAttributeValues(ObjectClass objclass, Uid uid,
			Set<Attribute> valuesToAdd, OperationOptions options) {
    	valuesToAdd = new OracleAttributeNormalizer(getConfiguration().getCSSetup()).normalizeAttributes(objclass, UpdateOp.class, valuesToAdd);
		return connector.addAttributeValues(objclass, uid, valuesToAdd, options);
	}

	public Uid authenticate(ObjectClass objectClass, String username,
			GuardedString password, OperationOptions options) {
		return connector.authenticate(objectClass, username, password, options);
	}

	public void checkAlive() {
		connector.checkAlive();
	}

	public Uid create(ObjectClass oclass, Set<Attribute> attrs,
			OperationOptions options) {
        attrs = new OracleAttributeNormalizer(getConfiguration().getCSSetup()).normalizeAttributes(oclass, CreateOp.class, attrs);
		return connector.create(oclass, attrs, options);
	}

	public FilterTranslator<Pair<String, FilterWhereBuilder>> createFilterTranslator(
			ObjectClass oclass, OperationOptions options) {
		return connector.createFilterTranslator(oclass, options);
	}

	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
		connector.delete(objClass, uid, options);
	}

	public void dispose() {
		connector.dispose();
	}

	public void executeQuery(ObjectClass oclass,
			Pair<String, FilterWhereBuilder> pair, ResultsHandler handler,
			OperationOptions options) {
		connector.executeQuery(oclass, pair, handler, options);
	}

	public OracleConfiguration getConfiguration() {
		return connector.getConfiguration();
	}

	public void init(Configuration cfg) {
		connector.init(cfg);
	}

	public Uid removeAttributeValues(ObjectClass objclass, Uid uid,
			Set<Attribute> valuesToRemove, OperationOptions options) {
    	valuesToRemove = new OracleAttributeNormalizer(getConfiguration().getCSSetup()).normalizeAttributes(objclass, UpdateOp.class, valuesToRemove);
		return connector.removeAttributeValues(objclass, uid, valuesToRemove,
				options);
	}

	public Schema schema() {
		return connector.schema();
	}

	public void test() {
		connector.test();
	}

	public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs,
			OperationOptions options) {
    	attrs = new OracleAttributeNormalizer(getConfiguration().getCSSetup()).normalizeAttributes(objclass, UpdateOp.class, attrs);
		return connector.update(objclass, uid, attrs, options);
	}
	

}
