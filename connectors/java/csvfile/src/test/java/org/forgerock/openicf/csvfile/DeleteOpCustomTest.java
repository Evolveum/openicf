/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * Portions Copyrighted 2011 Viliam Repan (lazyman)
 *
 * $Id$
 */
package org.forgerock.openicf.csvfile;

import org.forgerock.openicf.csvfile.util.TestUtils;
import org.forgerock.openicf.csvfile.util.Utils;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.annotations.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Viliam Repan (lazyman)
 */
public class DeleteOpCustomTest {

    private String TEST_FOLDER = "../deleteOp/";

    @Test
    public void correctDeleteAfterCreate() throws Exception {
        File beforeDeleteFile = TestUtils.getTestFile(TEST_FOLDER + "before-delete.csv");
        File backup = TestUtils.getTestFile(TEST_FOLDER + "before-delete-backup.csv");
        Utils.copyAndReplace(backup, beforeDeleteFile);

        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setFilePath(TestUtils.getTestFile(TEST_FOLDER + "before-delete.csv"));
        config.setUniqueAttribute("id");

        CSVFileConnector connector = new CSVFileConnector();
        connector.init(config);

        Set<Attribute> attributes = new HashSet<Attribute>();
        String uidValue = "nvix05";
        attributes.add(new Name(uidValue));
        attributes.add(AttributeBuilder.build("firstname", "Nivan"));
        attributes.add(AttributeBuilder.build("lastname", "Nnoris05"));
        Uid uid = connector.create(ObjectClass.ACCOUNT, attributes, null);
        assertNotNull(uid);
        assertEquals(uidValue, uid.getUidValue());

        String result = TestUtils.compareFiles(config.getFilePath(), TestUtils.getTestFile(TEST_FOLDER + "expected-after-create.csv"));
        assertNull("File updated incorrectly (create): " + result, result);

        connector.delete(ObjectClass.ACCOUNT, uid, null);

        connector.dispose();
        connector = null;

        result = TestUtils.compareFiles(config.getFilePath(), backup);
        assertNull("File updated incorrectly (delete): " + result, result);

        beforeDeleteFile.delete();
    }

    @Test
    public void correctDeleteAfterDoubleCreate() throws Exception {
        File configFile = TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create.csv");
        File backup = TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create-backup.csv");
        Utils.copyAndReplace(backup, configFile);

        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setFilePath(TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create.csv"));
        config.setUniqueAttribute("id");

        CSVFileConnector connector = new CSVFileConnector();
        connector.init(config);

        Set<Attribute> attributes = new HashSet<Attribute>();
        String uidValue = "nvix05";
        attributes.add(new Name(uidValue));
        attributes.add(AttributeBuilder.build("firstname", "Nivan"));
        attributes.add(AttributeBuilder.build("lastname", "Nnoris05"));
        Uid uidFirst = connector.create(ObjectClass.ACCOUNT, attributes, null);
        assertNotNull(uidFirst);
        assertEquals(uidValue, uidFirst.getUidValue());

        attributes = new HashSet<Attribute>();
        uidValue = "nvix06";
        attributes.add(new Name(uidValue));
        attributes.add(AttributeBuilder.build("firstname", "Nivan06"));
        attributes.add(AttributeBuilder.build("lastname", "Nnoris06"));
        Uid uidSecond = connector.create(ObjectClass.ACCOUNT, attributes, null);
        assertNotNull(uidSecond);
        assertEquals(uidValue, uidSecond.getUidValue());

        String result = TestUtils.compareFiles(config.getFilePath(), TestUtils.getTestFile(TEST_FOLDER + "expected-double-create.csv"));
        assertNull("File updated incorrectly (create): " + result, result);

        connector.delete(ObjectClass.ACCOUNT, uidFirst, null);

        connector.dispose();
        connector = null;

        result = TestUtils.compareFiles(config.getFilePath(), TestUtils.getTestFile(TEST_FOLDER + "expected-double-create-delete.csv"));
        assertNull("File updated incorrectly (delete): " + result, result);

        configFile.delete();
    }

    @Test
    public void correctDeleteSecondAfterDoubleCreate() throws Exception {
        File configFile = TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create.csv");
        File backup = TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create-backup.csv");
        Utils.copyAndReplace(backup, configFile);

        CSVFileConfiguration config = new CSVFileConfiguration();
        config.setFilePath(TestUtils.getTestFile(TEST_FOLDER + "delete-after-double-create.csv"));
        config.setUniqueAttribute("id");

        CSVFileConnector connector = new CSVFileConnector();
        connector.init(config);

        Set<Attribute> attributes = new HashSet<Attribute>();
        String uidValue = "nvix05";
        attributes.add(new Name(uidValue));
        attributes.add(AttributeBuilder.build("firstname", "Nivan"));
        attributes.add(AttributeBuilder.build("lastname", "Nnoris05"));
        Uid uidFirst = connector.create(ObjectClass.ACCOUNT, attributes, null);
        assertNotNull(uidFirst);
        assertEquals(uidValue, uidFirst.getUidValue());

        attributes = new HashSet<Attribute>();
        uidValue = "nvix06";
        attributes.add(new Name(uidValue));
        attributes.add(AttributeBuilder.build("firstname", "Nivan06"));
        attributes.add(AttributeBuilder.build("lastname", "Nnoris06"));
        Uid uidSecond = connector.create(ObjectClass.ACCOUNT, attributes, null);
        assertNotNull(uidSecond);
        assertEquals(uidValue, uidSecond.getUidValue());

        String result = TestUtils.compareFiles(config.getFilePath(), TestUtils.getTestFile(TEST_FOLDER + "expected-double-create.csv"));
        assertNull("File updated incorrectly (create): " + result, result);

        connector.delete(ObjectClass.ACCOUNT, uidSecond, null);

        connector.dispose();
        connector = null;

        result = TestUtils.compareFiles(config.getFilePath(), TestUtils.getTestFile(TEST_FOLDER + "expected-double-create-delete-second.csv"));
        assertNull("File updated incorrectly (delete): " + result, result);

        configFile.delete();
    }
}
