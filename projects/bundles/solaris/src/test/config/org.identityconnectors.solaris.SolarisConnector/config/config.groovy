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

  Validate.invalidConfig = [
    [port: null],
    [password: null],
    [connectionType: 'boobar']
  ]//Validate

  Test.invalidConfig = [
    [password: "nonsensePassword123456"], 
    [userName: "nonsenseUserName123456"]
  ]//Test

  Schema {
    oclasses = ['__ACCOUNT__', '__GROUP__']
    attributes {
      __GROUP__.oclasses = ['__NAME__' /* TODO, not yet decided wether to use the adapter's 'groupName' or rather the Framework's constant. */, 'gid', 'users']
      __ACCOUNT__.oclasses = ['__NAME__', 'dir', 'shell', 'group', 'secondary_group',
        'uid', 'expire', 'inactive', 'comment', 'time_last_login',
        'authorization', 'profile', 'role', 'max', 'min', 'warn', 'lock', 
        '__PASSWORD__' /* TODO extra attribute that wasn't in the schema of Adatper -- is it OK? */, 
         'force_change'
      ] //__ACCOUNT__.oclasses
    }//attributes

    attrTemplate = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: true,
      required: false,
      multiValue: false,
      returnedByDefault: true
    ]// attrTemplate
  
    attrRequiredTemplate = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: true,
      required: true,
      multiValue: false,
      returnedByDefault: true
    ]

    attrPasswdTemplate = [
      type: org.identityconnectors.common.security.GuardedString.class,
      readable: false,
      createable: true,
      updateable: true,
      required: false,
      multiValue: false,
      returnedByDefault: false
    ]

    attrNotRequiredNotReturnedTemplate = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: true,
      required: false,
      multiValue: false,
      returnedByDefault: false
    ]

  

    gid.attribute.__GROUP__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    __NAME__.attribute.__GROUP__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    users.attribute.__GROUP__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")

    inactive.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    min.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    max.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    secondary_group.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    group.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    __NAME__.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    expire.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    warn.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    dir.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    comment.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    uid.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrNotRequiredNotReturnedTemplate")
    lock.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    time_last_login.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    authorization.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    profile.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    __PASSWORD__.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrPasswdTemplate")
    role.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    force_change.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")
    shell.attribute.__ACCOUNT__.oclasses = Lazy.get("testsuite.Schema.attrTemplate")

    operations = [
      AuthenticationApiOp: ['__ACCOUNT__'],
      GetApiOp: ['__ACCOUNT__', '__GROUP__'],
      SchemaApiOp: ['__ACCOUNT__', '__GROUP__'],
      ValidateApiOp: ['__ACCOUNT__', '__GROUP__'],
      CreateApiOp: ['__ACCOUNT__', '__GROUP__'],
      SearchApiOp: ['__ACCOUNT__', '__GROUP__'],
      DeleteApiOp: ['__ACCOUNT__', '__GROUP__'],
      ScriptOnConnectorApiOp: ['__ACCOUNT__', '__GROUP__'],
      TestApiOp: ['__ACCOUNT__', '__GROUP__'],
      UpdateApiOp: ['__ACCOUNT__', '__GROUP__']
    ]//operations
  } //Schema

}// testsuite

__ACCOUNT__.inactive = "0"
