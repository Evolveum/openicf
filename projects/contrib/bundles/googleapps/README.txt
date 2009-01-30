

This is Connector for Google Apps for your Domain. The connector allows you to
create and manage Google Apps accounts.

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

