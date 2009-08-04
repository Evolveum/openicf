Resource Name: WebTimeSheet
Package Name: org.identityconnectors.webtimesheet
Bundle Directory: /Users/rjackson/NetBeansProjects/WebTimeSheet
Framework Directory: /Users/rjackson/Downloads/connector_toolkit-1.0.4709
Framework Version: 1.0



Test Parameters:
The following parameters must be set in your connector properties file (ie. ~/.connectors/connectors.properties) in order to perform the unit tests.

WTSURL=http://hosted.webtimesheet.com/mycompany/replicon
WTSLOGIN=admin
WTSPASSWORD=adminpassword
WTSAPPNAME=MyCompany-IdM
WTSAPPPASSWORD=IdMPassword



Dependencies:

Currently, the Web TimeSheet connector bundle source does not include the required HTTP client libraries.

/lib/build/commons-codec-1.3.jar
/lib/build/commons-logging-1.1.1.jar
/lib/test/commons-codec-1.3.jar
/lib/test/ commons-logging-1.1.1.jar
/lib/httpclient-4.0-beta2.jar
/lib/httpcore-4.0.1.jar

The Apache Commons codec and logging jars are dependencies of the HTTP client, but should not be included in the dist bundle as they already exist in Sun IdM (and can cause versioning problems)


Downloads:

http://hc.apache.org/httpcomponents-client/download.html
http://hc.apache.org/httpcomponents-core/download.html
http://commons.apache.org/downloads/download_logging.cgi
http://commons.apache.org/downloads/download_codec.cgi



Test Environment:

This connector has been tested against Web TimeSheet v8.8 and v8.9.  The RTAPI interface that is uses has not changed since v8.3.  It will connect to the self-hosted version or the SaaS version.  Replicon offers instant trial accounts (30 days) of the SaaS version that can be used for testing (http://replicon.com).


