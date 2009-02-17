

This is Connector for Google Apps.

Google Apps is a software-as-a-service platform
(SAAS) that provides email, calendar, documents and other services. This 
connector uses the Google Apps provisioning APIs to create, add, delete and modify user
accounts and email aliases. Note that the provisioning APIs are not available
for the free version of the Google Apps platform (the exception is for
educational and non profit groups where Google makes this available for at no
cost) . Please make sure you are using a
google apps platform that supports these APIs.


For more information on Google Apps see:

http://www.google.com/apps/intl/en/business/index.html


This is a Netbeans project.

Compiling:

1) edit build.xml to point to the connectors toolkit location. You will have to 
download this from the identity connectors project

2) Download the Google Apps for Your domain Java toolkit from:
http://code.google.com/p/gdata-java-client/downloads/list


3) Add the following jar files to a NetBeans Library called GoogleApps. Add
the library to your project.

gdata-appsforyourdomain-1.0.jar
gdata-client-1.0.jar
gdata-core-1.0.jar


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

