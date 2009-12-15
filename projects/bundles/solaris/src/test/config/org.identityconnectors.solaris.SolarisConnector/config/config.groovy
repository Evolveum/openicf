/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */
import org.identityconnectors.contract.data.groovy.Lazy
import org.identityconnectors.contract.exceptions.ObjectNotFoundException
import org.identityconnectors.common.security.GuardedString

connector {
  port = port
  loginShellPrompt = "#"
  sudoAuthorization = false
  connectionType = connectionType
}

testsuite {
  // path to bundle jar - property is set by ant - leave it as it is
  bundleJar=System.getProperty("bundleJar")
  bundleName=System.getProperty("bundleName")
  bundleVersion=System.getProperty("bundleVersion")
  connectorName="org.identityconnectors.solaris.SolarisConnector"
  
  Authentication.__ACCOUNT__.username = Lazy.get("i0.Authentication.__ACCOUNT__.__NAME__")
  Authentication.__ACCOUNT__.wrong.password = new GuardedString("_nonExistingPassword_".toCharArray())
  
  Search.disable.caseinsensitive = true

  Validate.invalidConfig = [
    [port: null],
    [password: null],
    [loginUser: null],
    [loginUser: ""],
    [connectionType: 'boobar'],
    [sudoAuthorization: true, credentials: null]
  ]//Validate

  Test.invalidConfig = [
    [password: "nonsensePassword123456"], 
    [loginUser: "nonsenseUserName123456"]
  ]//Test

  Schema {
    oclasses = ['__ACCOUNT__', 'shell' , '__GROUP__']
    attributes {
      __GROUP__.oclasses = ['__NAME__' , 'gid', 'users']
      shell.oclasses = ['shell', '__NAME__']
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
    __NAME__.attribute.__GROUP__.oclasses = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: false,
      required: false,
      multiValue: false,
      returnedByDefault: true
    ]
    users.attribute.__GROUP__.oclasses = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: true,
      required: false,
      multiValue: true,
      returnedByDefault: true
    ]// attrTemplate
    
    shell.attribute.shell.oclasses = [
      type: String.class,
      readable: true,
      createable: true,
      updateable: false,
      required: false,
      multiValue: true,
      returnedByDefault: false
    ]
    __NAME__.attribute.shell.oclasses = Lazy.get("testsuite.Schema.attrRequiredTemplate")

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
      GetApiOp: ['__ACCOUNT__', 'shell', '__GROUP__'],
      SchemaApiOp: ['__ACCOUNT__', '__GROUP__'],
      ValidateApiOp: ['__ACCOUNT__', 'shell', '__GROUP__'],
      CreateApiOp: ['__ACCOUNT__', '__GROUP__'],
      SearchApiOp: ['__ACCOUNT__', 'shell', '__GROUP__'],
      DeleteApiOp: ['__ACCOUNT__', '__GROUP__'],
      ScriptOnConnectorApiOp: ['__ACCOUNT__', 'shell', '__GROUP__'],
      TestApiOp: ['__ACCOUNT__', 'shell', '__GROUP__'],
      UpdateApiOp: ['__ACCOUNT__', '__GROUP__']
    ]//operations
  } //Schema

}// testsuite

gid = new ObjectNotFoundException()
modified.gid = new ObjectNotFoundException()
users = ["root"]
modified.users = [] // empty list

inactive = new ObjectNotFoundException()
shell = new ObjectNotFoundException()
role = new ObjectNotFoundException()
force_change = new ObjectNotFoundException()
authorization = new ObjectNotFoundException()
profile = new ObjectNotFoundException()
time_last_login = new ObjectNotFoundException()
lock = new ObjectNotFoundException()
comment = new ObjectNotFoundException()
min = new ObjectNotFoundException()
max = new ObjectNotFoundException()
group = new ObjectNotFoundException()
dir = new ObjectNotFoundException()
uid = new ObjectNotFoundException()
warn = new ObjectNotFoundException()
expire = new ObjectNotFoundException()
secondary_group = new ObjectNotFoundException()

Tstring = Lazy.random("aaaaa")