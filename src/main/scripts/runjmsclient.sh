#! /bin/bash -p

##
 ###########################################################################
 #
 # Filename    : runjmsclient.sh
 # Author      : Bala Ramakrishnan
 # Date        : 2016-12-06
 # Description : JMS Client code example
 #
 ##**************************************************************************
 ##

if [[ -z $JAVA_HOME ]]; then
    export JAVA_HOME=/opt/jdk
    (>&2 echo "JAVA_HOME is not set, setting it to $JAVA_HOME")
fi
export PATH=${JAVA_HOME}/bin:${PATH}

scriptdir=$(cd $(dirname $0); pwd)
targetdir=$scriptdir/target
propdir=${scriptdir}

if [[ ! -d $targetdir ]]; then
    targetdir=$(cd $scriptdir/../../../target; pwd)
fi

if [[ ! -f $propdir/log4j.properties ]]; then
    propdir=$(cd $scriptdir/../resources; pwd)
fi

CP=${propdir}:${targetdir}/*:${targetdir}/lib/*

usage() {
    cat <<EOF
usage: $0 [-durable|-queue] [-Dproperty.file=<properties file>] [-Dlog.file.path=<logfile directory>] [-Dlog.file=<logfile name>] [-Dclient.id=<clientid>] [-Dclient.name=<clientName>] <hostname_or_ip> [jms_port]

The only mandatory argument is the JMS Server host name or ip
    hostname_or_ip can be specified in the format:
       h1_or_ip1[,h2_or_ip2,h3_or_ip3]
       up to three VSD hosts in the cluster can be specified separated by comma with no spaces around the comma.
       If connection to a host fails, the next available in the list will be tried. This happens even if the
       connection fails while the JMS client is running.

    -durable : to establish durable connection

    -queue : to establish a connection to a JMS queue

    default jms_port : 61616

    default property.file : <script directory>/jmsclient.properties
        The property.file contains JMS client parameters such as user name, password, any topic filters etc.

    default log.file.path : <script directory>/log

    default log.file : jmsclient.log

    default client.id : random uuid

    default client.name : DurableJMSClient
EOF
}

mainClass=JMSTopicClient
systemProp=''
otherArgs=''
DEBUGOPT=''

while (( $# > 0 )); do
    if [[ ${1} == '-debug' ]]; then
        DEBUGOPT="-Xdebug -Xrunjdwp:transport=dt_socket,address=5050,server=y,suspend=y"
    elif [[ ${1} == '-durable' ]]; then
        mainClass=DurableJMSTopicClient
    elif [[ ${1} == '-queue' ]]; then
        mainClass=JMSQueueClient
    elif [[ ${1%%-D*} != $1 ]]; then
        systemProp="$systemProp $1"
    else
        otherArgs="$otherArgs $1"
    fi
    shift
done

exec java $DEBUGOPT -cp $CP $systemProp com.alu.cna.cloudmgmt.util.jms.${mainClass} $otherArgs
