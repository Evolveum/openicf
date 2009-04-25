package org.identityconnectors.solaris.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { SolarisTest.class, SolarisTestAuthenticate.class,
        SolarisTestCreate.class })
public class SolarisUnitTestsSuite {
    // the class remains completely empty,
    // being used only as a holder for the above annotations
}