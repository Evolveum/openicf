scriptedrest-connector-1.1.0.0-SNAPSHOT

OpenDJ samples
==============

1 - Few things to know:
-----------------------

- The samples scripts provided work with OpenDJ 2.6 REST interface
- The Example.ldif file should be loaded in DJ for the samples to work
- By default, REST interface is OFF in DJ. You need to enable it. See:
http://opendj.forgerock.org/opendj-server/doc/admin-guide/index.html#setup-rest2ldap-connection-handler
http://opendj.forgerock.org/opendj-server/doc/admin-guide/index.html#chap-rest-operations

Commands to run from OpenDJ bin/ folder:
$ ./dsconfig set-connection-handler-prop --hostname <hostname> --port 4444 --bindDN "cn=Directory Manager" --bindPassword <password> --handler-name "HTTP Connection Handler" --set enabled:true --no-prompt --trustAll
$ ./dsconfig set-log-publisher-prop --hostname <hostname> --port 4444 --bindDN "cn=Directory Manager" --bindPassword <password> --publisher-name "File-Based HTTP Access Logger" --set enabled:true --no-prompt --trustAll


2 - Edit the REST/LDAP mapping file
-----------------------------------

The main config file is config/http-config.json
This file defines how HTTP/REST requests will be mapped to the LDAP directory space.

For our samples to work, you need to change the value of "searchBaseDN" value (line 23) to:

        "searchBaseDN"         : "dc=example,dc=com",





3- Change the REST interface listen port
----------------------------------------


The OpenDJ REST interface will, by default, listen on port 8080. You can change this behaviour using command line.

=====================================================================================================================

./dsconfig 


>>>> Specify OpenDJ LDAP connection parameters

Directory server hostname or IP address [manik]: 

Directory server administration port number [4444]: 

Administrator user bind DN [cn=Directory Manager]: 

Password for user 'cn=Directory Manager': 


>>>> OpenDJ configuration console main menu

What do you want to configure?

    1)   Access Control Handler               21)  Log Publisher
    2)   Access Log Filtering Criteria        22)  Log Retention Policy
    3)   Account Status Notification Handler  23)  Log Rotation Policy
    4)   Administration Connector             24)  Matching Rule
    5)   Alert Handler                        25)  Monitor Provider
    6)   Attribute Syntax                     26)  Password Generator
    7)   Backend                              27)  Password Policy
    8)   Certificate Mapper                   28)  Password Storage Scheme
    9)   Connection Handler                   29)  Password Validator
    10)  Crypto Manager                       30)  Plugin
    11)  Debug Target                         31)  Plugin Root
    12)  Entry Cache                          32)  Replication Domain
    13)  Extended Operation Handler           33)  Replication Server
    14)  External Changelog Domain            34)  Root DN
    15)  Global Configuration                 35)  Root DSE Backend
    16)  Group Implementation                 36)  SASL Mechanism Handler
    17)  Identity Mapper                      37)  Synchronization Provider
    18)  Key Manager Provider                 38)  Trust Manager Provider
    19)  Local DB Index                       39)  Virtual Attribute
    20)  Local DB VLV Index                   40)  Work Queue

    q)   quit

Enter choice: 9


>>>> Connection Handler management menu

What would you like to do?

    1)  List existing Connection Handlers
    2)  Create a new Connection Handler
    3)  View and edit an existing Connection Handler
    4)  Delete an existing Connection Handler

    b)  back
    q)  quit

Enter choice [b]: 3

>>>> Select the Connection Handler from the following list:

    1)  HTTP Connection Handler
    2)  JMX Connection Handler
    3)  LDAP Connection Handler
    4)  LDAPS Connection Handler
    5)  LDIF Connection Handler
    6)  SNMP Connection Handler

    c)  cancel
    q)  quit

Enter choice [c]: 1


>>>> Configure the properties of the HTTP Connection Handler

         Property                           Value(s)
         ----------------------------------------------------------------------
    1)   allowed-client                     All clients with addresses that do
                                            not match an address on the deny
                                            list are allowed. If there is no
                                            deny list, then all clients are
                                            allowed.
    2)   authentication-required            true
    3)   config-file                        config/http-config.json
    4)   denied-client                      If an allow list is specified, then
                                            only clients with addresses on the
                                            allow list are allowed. Otherwise,
                                            all clients are allowed.
    5)   enabled                            true
    6)   keep-stats                         true
    7)   key-manager-provider               -
    8)   listen-address                     0.0.0.0
    9)   listen-port                        8080
    10)  max-concurrent-ops-per-connection  Let the server decide.
    11)  ssl-cert-nickname                  server-cert
    12)  ssl-cipher-suite                   Uses the default set of SSL cipher
                                            suites provided by the server's
                                            JVM.
    13)  ssl-client-auth-policy             optional
    14)  ssl-protocol                       Uses the default set of SSL
                                            protocols provided by the server's
                                            JVM.
    15)  trust-manager-provider             -
    16)  use-ssl                            false

    ?)   help
    f)   finish - apply any changes to the HTTP Connection Handler
    c)   cancel
    q)   quit

Enter choice [f]: 9

>>>> Configuring the "listen-port" property

    Specifies the port number on which the HTTP Connection Handler will listen
    for connections from clients.

    Only a single port number may be provided.

    Syntax:  1 <= INTEGER <= 65535

Do you want to modify the "listen-port" property?

    1)  Keep the value: 8080
    2)  Change the value

    ?)  help
    q)  quit

Enter choice [1]: 2


Enter a value for the "listen-port" property: 8089

Press RETURN to continue 


>>>> Configure the properties of the HTTP Connection Handler

         Property                           Value(s)
         ----------------------------------------------------------------------
    1)   allowed-client                     All clients with addresses that do
                                            not match an address on the deny
                                            list are allowed. If there is no
                                            deny list, then all clients are
                                            allowed.
    2)   authentication-required            true
    3)   config-file                        config/http-config.json
    4)   denied-client                      If an allow list is specified, then
                                            only clients with addresses on the
                                            allow list are allowed. Otherwise,
                                            all clients are allowed.
    5)   enabled                            true
    6)   keep-stats                         true
    7)   key-manager-provider               -
    8)   listen-address                     0.0.0.0
    9)   listen-port                        8089
    10)  max-concurrent-ops-per-connection  Let the server decide.
    11)  ssl-cert-nickname                  server-cert
    12)  ssl-cipher-suite                   Uses the default set of SSL cipher
                                            suites provided by the server's
                                            JVM.
    13)  ssl-client-auth-policy             optional
    14)  ssl-protocol                       Uses the default set of SSL
                                            protocols provided by the server's
                                            JVM.
    15)  trust-manager-provider             -
    16)  use-ssl                            false

    ?)   help
    f)   finish - apply any changes to the HTTP Connection Handler
    c)   cancel
    q)   quit

Enter choice [f]: 

The HTTP Connection Handler was modified successfully

Press RETURN to continue 


>>>> Connection Handler management menu

What would you like to do?

    1)  List existing Connection Handlers
    2)  Create a new Connection Handler
    3)  View and edit an existing Connection Handler
    4)  Delete an existing Connection Handler

    b)  back
    q)  quit

Enter choice [b]: q

===================================================================================================================

Do not forget to restart OpenDJ...

You can then test that the OpenDJ REST interface is up:


$ curl http://idm:password@opendj.example.com:8089/users/jdoe?_prettyPrint=true 

{
  "_rev" : "00000000ab284af3",
  "schemas" : [ "urn:scim:schemas:core:1.0" ],
  "contactInformation" : {
    "telephoneNumber" : "12345",
    "emailAddress" : "jdoe@example.com"
  },
  "_id" : "jdoe",
  "name" : {
    "familyName" : "Doe",
    "givenName" : "John"
  },
  "userName" : "jdoe@example.com",
  "displayName" : "John Doe",
  "groups" : [ {
    "_id" : "openidm"
  } ]
}



Et voila!
