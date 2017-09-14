Please find the sample JMS client in the below location:

https://github.com/nuagenetworks/jmsclient/releases/tag/R5.0

Download "jmsclient-1.0.8.zip" from the above location

This is a sample code to run a JMS client and receive messages on a subscription channel.

To use it:
==========

1. unzip the file jmsclient-${project.version}.zip

2. using the VSD GUI, add a user eg: jmsclient to the 'csp' organization.

3. add this user and password to the jmsclient.properties file or whatever file used with the -Dprop_file command line
option:

jms_username = jmsclient@csp   # since in step 2 above, 'jmsclient' was added to the csp organizaion.
jms_password = <the password for the user>

4. Make sure port 61616 is open in iptables for this client machine
ipset add vsd <ip address of the JMS client machine>

5. Next change directory to:
jmsclient-${project.version}

and run:

6. ./runjmsclient.sh [-durable|-queue] [-Dproperty.file=propertyfile] [-Dlog.file.path=logfile_directory] [-Dlog.file=logfilename] h1_or_ip1[,h2_or_ip2,h3_or_ip3] [jms port]

The only mandatory argument is the JMS Server host name or ip.

When connecting to a VSD cluster, the client must specify all the JMS Server host names or IP addresses
in the format : h1_or_ip1,h2_or_ip2,h3_or_ip3

The client would automatically connect to the master JMS server.

-durable : to establish durable connection
-queue   : to establish a connection to a JMS queue

default jms_port : 61616
default property.file : <script directory>/jmsclient.properties
The property.file contains JMS client parameters such as user name, password, any topic filters etc.
default log.file.path : <script directory>/log
default log.file : jmsclient.log
default client.id : random uuid
default client.name : DurableJMSTopicClient. client name is used only if the connection is durable

if log.file.path property option is not specified, the default is the 'log' subdirectory of this script directory.
if log.file property option is not specified, the default log file name is jmsclient.log. The log file is created in
the directory specified by the log.file.path property.


If you want to use the filter version
=====================================
./runjmsclient.sh <other options> -Dproperty.file=jmsfilter.properties <host_or_ip_list>


Setting log level
=================
The log level of messages written to jmsclient.log can be modified even when the program is running by running this script as below.

./setLogLevel.sh -l <LOG LEVEL> [component]
    LOG LEVEL is one of WARN, DEBUG, INFO
    component is one of all or client
        when component is 'client', only the log level of client code is set
        when component is 'all', the log level of client as well as the root logger are set. When the log level of root logger is set to for example DEBUG, detailed messages from low level libraries will also be output.


Note:
The durable version requires a unique client name and client id to identify the connection. Thus in order for the client to receive messages that were published when the client had abnormally terminated, the same connection id and connection name must be used. This also means that two simultaneous durable client connections to the same topic MUST use a different client.id and client.name.
