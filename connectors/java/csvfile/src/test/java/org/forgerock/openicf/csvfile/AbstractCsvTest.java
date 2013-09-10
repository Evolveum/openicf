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

import org.identityconnectors.common.logging.Log;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.lang.reflect.Method;

/**
 * @author Viliam Repan (lazyman)
 */
public class AbstractCsvTest {

    public static final File TEST_FILES_ROOT_FOLDER = new File("./src/test/resources/");

    private Log LOG;

    public AbstractCsvTest(Log LOG) {
        this.LOG = LOG;
    }

    @BeforeClass
    public final void beforeClass() throws Exception {
        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> START {0} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});

        customBeforeClass();
    }

    @AfterClass
    public final void afterClass() throws Exception {
        customAfterClass();

        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> FINISH {0} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @BeforeMethod
    public final void beforeMethod(Method method) throws Exception {
        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> START {0}.{1} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});

        customBeforeMethod(method);
    }

    @AfterMethod
    public final void afterMethod(Method method) throws Exception {
        customAfterMethod(method);

        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> END {0}.{1} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});
    }

    protected void customAfterMethod(Method method) throws Exception {

    }

    protected void customBeforeClass() throws Exception {

    }

    protected void customBeforeMethod(Method method) throws Exception {

    }

    protected void customAfterClass() throws Exception {

    }

}
