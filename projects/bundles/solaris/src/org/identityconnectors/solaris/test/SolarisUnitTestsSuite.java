package org.identityconnectors.solaris.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * summary class containing all unit tests.
 * @author david
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { SolarisTest.class, SolarisConfigurationTest.class,
        SolarisConnectionTest.class, SolarisTestAuthenticate.class,
        SolarisTestCreate.class })
public class SolarisUnitTestsSuite {
    // the class remains completely empty,
    // being used only as a holder for the above annotations
}