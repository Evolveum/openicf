

This is Connector for Google Apps.

Google Apps is a software-as-a-service platform
(SAAS) that provides email, calendar, documents and other services. This 
connector uses the Google Apps provisioning APIs to create, add, delete and modify user
accounts and email aliases. 

Note: Only the Premium (paid) or Educational versions of Google Apps provide access to the provisioning APIs. This connector
will not work on free Google Apps domains. 


For more information on Google Apps see:

http://www.google.com/apps/intl/en/business/index.html


This is structured as a Netbeans project.

TO DO: The dependencies are difficult to set up. Apologies in advance.
This probably needs to be converted to maven...

Compiling:

0) If you are using Netbeans, you will have to add junit4.jar to
the ant classpath. Preferences->Ant->Classpath

1) Edit build.xml to point to the connectors toolkit directory. You can
download this from the identity connectors project

2) You need to add the following libraries to the Netbeans project:
GoogleApps
Junit4.5
groovy-all
connector-framework.jar
connector-framework-internal.jar


The Google Apps jar files are in the lib directory. The following
jars are needed:

gdata-appsforyourdomain-1.0.jar
gdata-client-1.0.jar
gdata-core-1.0.jar
google-collect-1.0.rc1.jar


**** Note that the lib/nblibaries.properties file references these jars files
. You should be able to edit this file for your environment (you
may have to close and re-open the project after editing this file). 






Tests:


To run Connector tests, you will need a groovy build.groovy file in your
$home path that contains the URL, admin id, and password of a google apps
test account.

By default the connector framework will look in:
~/.connectors/connector-googleapps/build.groovy


The build.groovy will look something like this:
connector.connectionUrl="https://www.google.com/a/feeds/foobar.org/"
connector.login="admin@foobar.org"
connector.password="Password"



Please send any questions to warren.strange@gmail.com

Thanks



Update August 2009:

-Changed the configuration to break out the domain name as a seperate field. The
previous configuration confused a lot of people.


- Added Group support. Thanks to Rob Jackson @Nulli for getting this started.


