Please find the sample JMS client in the below location:

https://github.com/nuagenetworks/jmsclient/releases/tag/6.0.3

Download "jmsclient-1.0.11-dist.zip" from the above location

This is a sample code to run a JMS client and receive messages on a subscription channel.

To use it:
==========

1. unzip the file jmsclient-${project.version}.zip

2. using the VSD GUI, add a user eg: jmsclient to the 'csp' organization.

3. add this user and password to the jmsclient.properties file or whatever file used with the -Dprop_file command line
option:

jms_username = jmsclient@csp   # since in step 2 above, 'jmsclient' was added to the csp organizaion.
jms_password = <the password for the user>

4. For TLS support, copy the truststore from VSD "/opt/vsd/jboss/standalone/configuration/vsd.truststore" and place it in JMS client location (For eg : /root/TLS_JMS/vsd.truststore) . You need to add truststore_location in jmsclient.properties as shown below

truststore_location = /root/TLS_JMS/vsd.truststore


5. Make sure port 61616 is open in iptables for this client machine
ipset add vsd <ip address of the JMS client machine>

6. Next change directory to:
jmsclient-${project.version}

and run:

7. ./runjmsclient.sh [-durable] [-Dproperty.file=propertyfile] [-Dlog.file.path=logfile_directory] [-Dlog.file=logfilename] h1_or_ip1[,h2_or_ip2,h3_or_ip3] [jms port]

The only mandatory argument is the JMS Server host name or ip.

When connecting to a VSD cluster, the client must specify all the JMS Server host names or IP addresses
in the format : h1_or_ip1,h2_or_ip2,h3_or_ip3

The client would automatically connect to the master JMS server.

-durable : to establish durable connection

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

For TLS Support
===============
./runjmsclient.sh <other options> -Dproperty.file=jmsfilter.properties <host_or_ip_list> 61619

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
