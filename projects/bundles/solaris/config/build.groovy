/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy

connector {
  port = 22
  rootShellPrompt = "#"
  sudoAuth = false
  connectionType = "ssh"
}

testsuite {
  // path to bundle jar - property is set by ant - leave it as it is
  bundleJar=System.getProperty("bundleJar")
  bundleName=System.getProperty("bundleName")
  bundleVersion=System.getProperty("bundleVersion")
  connectorName="org.identityconnectors.solaris.SolarisConnector"
}
