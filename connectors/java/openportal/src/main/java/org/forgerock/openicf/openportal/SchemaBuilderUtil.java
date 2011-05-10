/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.forgerock.openicf.openportal;

import java.util.EnumSet;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.Connector;

/**
 *
 * @author admin
 */

public class SchemaBuilderUtil {

    public Schema createBuilder(){
        SchemaBuilder schemaBuilder = new SchemaBuilder((Class<? extends Connector>) getClass());


        //User
        ObjectClassInfoBuilder objInfo = new ObjectClassInfoBuilder();
        objInfo.setType(ObjectClass.ACCOUNT_NAME);
        objInfo.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED, Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("screenName", String.class, EnumSet.of(Flags.REQUIRED, Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("firstName", String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("lastname", String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("uid", Long.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("emailAddress", String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("password", String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        schemaBuilder.defineObjectClass(objInfo.build());

        //Group
        objInfo = new ObjectClassInfoBuilder();
        objInfo.setType(ObjectClass.GROUP_NAME);
        objInfo.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.NOT_UPDATEABLE)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("companyId", Long.class, EnumSet.of(Flags.REQUIRED)));
        objInfo.addAttributeInfo(AttributeInfoBuilder.build("userGroupId", Long.class,EnumSet.of(Flags.REQUIRED)));
        schemaBuilder.defineObjectClass(objInfo.build());
        Schema schema = schemaBuilder.build();


        return schema;
    }
}
