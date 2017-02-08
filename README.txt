This is a sample code to run a JMS client and receive messages on a subscription channel.

To use it:
==========

1. unzip the file jmsclient-${project.version}.zip

2. using the VSD GUI, add a user eg: jmsclient to the 'csp' organization.

3. add this user and password to the jmsclient.properties file or whatever file used with the -Dprop_file command line
option:

jms_username = jmsclient@csp   # since in step 2 above, 'jmsclient' was added to the csp organizaion.
jms_password = <the password for the user>

4. Make sure ports 5005 and 4447 are open in iptables for this client machine
ipset add vsd <ip address of the JMS client machine>

5. Next change directory to:
jmsclient-${project.version}

and run:

6. ./runjmsclient.sh [-durable|-queue] [-Dproperty.file=propertyfile] [-Dlog.file.path=logfile_directory] [-Dlog.file=logfilename] h1_or_ip1[,h2_or_ip2,h3_or_ip3] [jms port]

The only mandatory argument is the JMS Server host name or ip.
hostname or ip address can be specified in the format: h1_or_ip1[,h2_or_ip2,h3_or_ip3]
Optionally up to two additional VSD hosts in the cluster can be specified separated by
comma with no spaces around the comma.

If connection to a host fails, the next available in the list will be tried. This happens even if the
connection fails while the JMS client is running.

-durable : to establish durable connection
-queue   : to establish a connection to a JMS queue

default jms_port : 4447
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


Durable messages survive client restart. However, for messages to survive JBOSS server restart, the following two lines need to be commented in /etc/init.d/jboss.sh

ie. the lines:
rm -rf $JBOSS_HOME/standalone/tmp/*
rm -rf $JBOSS_HOME/standalone/data/*

should be commented out, and look like below.
# rm -rf $JBOSS_HOME/standalone/tmp/*
# rm -rf $JBOSS_HOME/standalone/data/*

First stop JBOSS with: monit stop -g vsd-core. After the JBOSS process has terminated, make changes to this script file,
then restart JBOSS with: monit start -g vsd-core

To simulate messages surviving client restart, do the following:

Kill the durable JMS client process with kill -9.
Then perform some activity on the VSD gui such as instantiating a new L3 domain from a template.

Run the durable JMS client again, specifying the same clientId and clientName as before. The durable jms client will now receive all the L3Domain creation messages that were published to CNAMessages topic.

To simulate messges surving server restart, do the following:
run a jms client with durable connections, specifying all the three hosts on the command line. The client now establisjhes connection to server 1. This can be verified by running the 'netstat -t -n -p' command on the client machine.

When the client is connected to server1, perform some activity on server 2 node of VSD that generates a large number of messages - eg. creating a large number of vports at the same time. Next, terminate the jboss process on server1. After a delay, the client will start receiving messages from server2. There may a loss of one or two messages around the time of server1 shutdown.
