/*
 ***************************************************************************
 *
 * Filename    : AbstractJMSClient
 * Author      : Bala Ramakrishnan
 * Date        : 2016-12-06
 * Description : JMS Client code example
 *
 ****************************************************************************
 */

package com.alu.cna.cloudmgmt.util.jms;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger; 
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.JMSSecurityException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJMSClient implements ExceptionListener
{

    private static final String NL = System.getProperty("line.separator");
    static
    {
        initializeLogging();
    }
    private static AtomicInteger threadId =  new AtomicInteger();

    public static final Object syncObject = new Object();
    private static final Logger log = LoggerFactory.getLogger(AbstractJMSClient.class.getName());
    // /////// Configurable properties /////////
    private static final String TOPIC = "topic";
    private static final String JMS_USERNAME = "jms_username";
    private static final String JMS_PASSWORD = "jms_password";
    private static final String MESSAGE_SELECTOR = "message_selector";
    private static final String TRUSTSTORE_LOCATION = "truststore_location";

    protected static String jmsHostList = null;
    // ActiveMQ Open wire connector port
    protected static int jmsPort = 61616;
    protected static String propFile = "jmsclient.properties";

    // ////////////////////////////////////////

    private static final String JMS_VALIDATION_ERROR = "Unable to validate user";
    private static final String DEFAULT_JMS_USERNAME = "jmsclient@csp";
    private static final String DEFAULT_JMS_PASSWORD = "clientpass";
    private static final String INITIAL_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    private static final String PROVIDER_URL_FMT = "tcp://%s:%d?wireFormat.cacheEnabled=false&wireFormat.tightEncodingEnabled=false";
    private static final String PROVIDER_URL_TLS = "ssl://%s:%d?socket.verifyHostName=false";
    private static final String FAILOVER_URL_FMT = "failover:(%s)?maxReconnectDelay=1000";

    private static final int PROPERTIES_REFRESH = 10000; // poll every 10 seconds for property change.

    public static int reconnectWaitIntervalSecs = 15;
    public static int maxReconnectWaitIntervalSecs = 90;
    public static double reconnectDelayFactor = 2;

    protected String topicName = "jms/topic/CNAMessages";
    protected String jmsRemoteFactory = "ConnectionFactory";

    protected String messageSelector = null;

    protected String trustStoreLocation = null;
    protected Boolean isSslConnection = false;

    protected Connection conn = null;
    protected Session session = null;
    protected Destination topic = null;
    protected MessageConsumer subscriber = null;
    protected Properties configProperties = null;
    private int connectAttempts = 1;

    private InitialContext jndiCtx = null;
    ExecutorService taskPool = null;

    protected String jmsUser = DEFAULT_JMS_USERNAME;
    protected String jmsPassword = DEFAULT_JMS_PASSWORD;

    protected String providerUrl = null;
    protected String clientId;

    protected static volatile boolean _reconnect = false;
    protected static Thread _mainThread = null;

    public AbstractJMSClient() {
        clientId = System.getProperty("client.id");
        if (clientId == null)
            clientId = UUID.randomUUID().toString();

        initializeVariables();
        taskPool = createThreadPool(configProperties);
    }

    void execute()
    {
        getLogger().info(">>>> Starting {} <<<<", getClass().getSimpleName());
        getLogger().debug("===============================================");
        getLogger().debug("* using following values: ");
        getLogger().debug("* JMS Server list: {}", jmsHostList);
        getLogger().debug("* JMS port: {}", jmsPort);
        getLogger().debug("* property.file: {}", propFile);
        getLogger().debug("* log.file.path: {}", System.getProperty("log.file.path"));
        getLogger().debug("* log.file: {}", System.getProperty("log.file"));
        getLogger().debug("* client.id: {}", clientId);
        getLogger().debug("* client.name: {}", System.getProperty("client.name"));
        getLogger().debug("===============================================");

        getLogger().info("Connection: {}", providerUrl);
        getLogger().info("Topic: {}", topicName);
        getLogger().info("User: {}", jmsUser);
        try
        {
            connect();
            waitForShutDown();
        }
        catch (Exception e)

        {
            getLogger().error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public static void initializeLogging()
    {
        if (StringUtils.isBlank(System.getProperty("log.file.path")))
        {
            // get the jar file directory, go one step above, and then set to log directory
            String logfilePath = null;
            try
            {
                logfilePath = AbstractJMSClient.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                logfilePath += File.separator + ".." + File.separator + ".." + File.separator + "log";
                logfilePath = new File(logfilePath).getCanonicalPath();
            }
            catch (Exception e)
            {
                logfilePath = ".";
            }
            System.setProperty("log.file.path", logfilePath);
        }
        if(StringUtils.isBlank(System.getProperty("log.file")))
        {
            System.setProperty("log.file", "jmsclient.log");
        }
        PropertyConfigurator.configure(System.getProperties());

        PropertyConfigurator.configureAndWatch(
                AbstractJMSClient.class.getResource(File.separator + "log4j.properties").getPath(), PROPERTIES_REFRESH);
    }

    protected static void parseArgs(String[] args)
    {
        if (args.length == 0)
        {
            System.err.println("*** host name or IP must be specified ***");
            usage();
            System.exit(-2);
        }

        if (args.length >= 1)
        {
            jmsHostList = args[0];
            try
            {
                Integer.parseInt(jmsHostList);
                System.err.println("*** Invalid host name or IP ***");
                usage();
                System.exit(-2);
            }
            catch (NumberFormatException ne)
            {
                // ignore.
            }
        }

        if (args.length >= 2)
        {
            try
            {
                jmsPort = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException ne)
            {
                System.err.println("*** Invalid port number ***");
                usage();
                System.exit(-2);
            }
        }

        propFile = System.getProperty("property.file", propFile);

        // if the property file does not start with absolute path, then look for it in the classpath.
        if (!propFile.startsWith(File.separator))
        {
            URL propUrl = AbstractJMSClient.class.getResource(File.separator + propFile);
            if (propUrl != null)
                propFile = propUrl.getPath();
        }
    }

    public void waitForShutDown() throws Exception
    {
        Thread shutDownHook = new Thread("ShutdownHook")
        {
            @Override
            public void run()
            {
                getLogger().info("executing shutdown");
                try
                {
                    // Clean all the connections...
                    releaseResources();
                }
                catch (Throwable e)
                {
                    getLogger().error("could not shutdown gracefully", e);
                }

                // shutdown thread pool
                taskPool.shutdownNow();

                getLogger().info("executed shutdown");
            }
        };

        Runtime.getRuntime().addShutdownHook(shutDownHook);
        // now simply keep waiting
        while(true)
        {
            synchronized(syncObject)
            {
                try
                {
                    syncObject.wait();
                }
                catch (InterruptedException ie)
                {
                    getLogger().info("Interrupted while waiting for shutdown to complete");
                    // set the interrupt status.
                    Thread.currentThread().interrupt(); // set the interrupt status
                }

                if (Thread.interrupted())
                {
                    // check if the thread was interrupted for reconnection attempt
                    // otherwise quit.
                    if (_reconnect)
                    {
                        _reconnect = false;
                        releaseResources();
                        getLogger().debug("reconnecting after interruption");
                        connect();
                    }
                    else
                    {
                        getLogger().warn("{} got interrupted, exiting", Thread.currentThread().getName());
                        break;
                    }
                }
            }
        }
    }

    protected void initializeVariables() {

        try
        {
            configProperties = loadProperties(propFile);
        }
        catch (Exception e)
        {
            System.err.println("*** Error loading property file: " + propFile + " ***");
            e.printStackTrace();
            usage();
            System.exit(-2);
        }

        topicName = configProperties.getProperty(TOPIC, topicName);
        jmsUser = configProperties.getProperty(JMS_USERNAME, jmsUser);
        jmsPassword = configProperties.getProperty(JMS_PASSWORD, jmsPassword);
        messageSelector = configProperties.getProperty(MESSAGE_SELECTOR);

        trustStoreLocation = configProperties.getProperty(TRUSTSTORE_LOCATION);

        String lProviderUrlFormat = PROVIDER_URL_FMT;
        if (trustStoreLocation != null && !trustStoreLocation.isEmpty())
        {
            getLogger().debug("Connecting through SSL, Using the truststore location: "+ trustStoreLocation);
            isSslConnection = true;
            lProviderUrlFormat = PROVIDER_URL_TLS;
            System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        }

        String[] jmsHosts = jmsHostList.split(",");
        StringBuilder urls = new StringBuilder();
        for (String jmsHost: jmsHosts)
        {
            String url = String.format(lProviderUrlFormat, jmsHost, jmsPort);
            urls.append(url).append(",");
        }

        // remove the last comma.
        urls.deleteCharAt(urls.length()-1);
        // If there is more than one JMS host, then use fail over transport to connect to
        // the Master broker.
        if (jmsHosts.length > 1)
        {
            providerUrl = String.format(FAILOVER_URL_FMT, urls);
        }
        else
        {
            providerUrl = urls.toString();
        }

        reconnectWaitIntervalSecs = Integer.parseInt(configProperties.getProperty("reconnect_wait_interval_secs",
                Integer.toString(reconnectWaitIntervalSecs)));
        maxReconnectWaitIntervalSecs = Integer.parseInt(configProperties.getProperty("max_reconnect_wait_interval_secs",
                Integer.toString(maxReconnectWaitIntervalSecs)));
        reconnectDelayFactor = Double.parseDouble(configProperties.getProperty("reconnect_delay_factor",
                Double.toString(reconnectDelayFactor)));
        connectAttempts = Integer.parseInt(configProperties.getProperty("connect_attempts",
                Integer.toString(connectAttempts)));
        if (connectAttempts <= 0)
            connectAttempts = 1;  // minimum at least one.

    }

    protected Properties getJndiConfig()
    {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        env.put(InitialContext.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        // need to try redundant connection attempts in the format below.
        // env.put(Context.PROVIDER_URL, "remote://<s1>:<p1>,remote://<s2>:<p2>");
        env.put(Context.PROVIDER_URL, providerUrl);

        return env;
    }

    protected void connect() throws Exception
    {
        int attempt = 1;
        int sleepTime = reconnectWaitIntervalSecs;

        while (attempt <= connectAttempts)
        {

            try
            {
                Properties jndiProperties = getJndiConfig();
                getLogger().debug("Establishing JNDI Context: {}", jndiProperties);
                jndiCtx = new InitialContext(jndiProperties);
                topic = getDestination(jndiCtx, topicName);
                ConnectionFactory tcf = getConnectionFactory(jndiCtx);
                getLogger().debug("topic name from provider: {}, topicObject: {}", topicName, topic);

                getLogger().debug("creating JMS connection");
                conn = tcf.createConnection(jmsUser, jmsPassword);

                conn.setClientID(clientId);
                session = createSession(conn);
                getLogger().debug("started JMS session");

                getLogger().debug("starting JMS connection");
                conn.start();
                conn.setExceptionListener(this);
                getLogger().debug("conn client id from JMS: {}", conn.getClientID());

                // subscribe to topic
                getLogger().info("Filter: " + messageSelector);
                subscriber = createSubscriber();
                JMSMessageHandler handler = new JMSMessageHandler(taskPool); // pass a pool to it?
                subscriber.setMessageListener(handler);

                getLogger().info("Started connection...");
                break;
            }
            catch(Exception e)
            {
                getLogger().error("Exception while connecting to JMS", e);
                try
                {
                    releaseResources();
                }
                catch (Exception ee)
                {
                    getLogger().error("Exception while discconnecting from JMS", ee);
                }

                if (e instanceof JMSSecurityException)
                {
                    JMSSecurityException se = (JMSSecurityException) e;
                    if (se.getMessage().contains(JMS_VALIDATION_ERROR))
                    {
                        throw new Exception("Authentication failed", se);
                    }
                }
                if (attempt < connectAttempts)
                {
                    getLogger().warn("Waiting {} seconds, before reconnection attempt {} of {}",
                            sleepTime, (attempt+1), connectAttempts);
                    sleepTime *= reconnectDelayFactor;
                    if (sleepTime > maxReconnectWaitIntervalSecs)
                        sleepTime = maxReconnectWaitIntervalSecs;
                    Thread.sleep(sleepTime * 1000);
                }
                attempt++;
            }
        }

        if (attempt > connectAttempts)
        {
            throw new Exception("Failed to connect after " + connectAttempts + " attempts");
        }
    }

    protected abstract MessageConsumer createSubscriber() throws JMSException;
    protected abstract ConnectionFactory getConnectionFactory(InitialContext iniCtx) throws Exception;
    protected abstract Destination getDestination(InitialContext ctx, String topicName) throws Exception;
    protected abstract Session createSession(Connection conn) throws Exception;
    abstract Logger getLogger();

    @Override
    public void onException(JMSException ex)
    {
        getLogger().error("+++++++++++++++++ onException invoked", ex);
        _reconnect = true;
        _mainThread.interrupt(); // notify the thread to reconnect
    }

    protected void releaseResources()
    {
        try
        {
            if (conn != null)
            {
                getLogger().debug("stopping connection");
                conn.stop();
                getLogger().debug("closing connection");
                if (session != null)
                    session.close();
            }
        }
        catch (Exception e)
        {
            getLogger().error("Exception while releasing resources", e);
        }
        finally
        {
            try
            {
                if (conn != null)
                {
                    getLogger().debug("closing session");
                    conn.close();
                }
                if (jndiCtx != null)
                {
                    getLogger().debug("closing jndi context");
                    jndiCtx.close();
                }
            }
            catch (Exception e)
            {
                getLogger().error("Exception while releasing connections in finally block", e);
            }
            session = null;
            conn = null;
            jndiCtx = null;
        }
    }

    public static ExecutorService createThreadPool(Properties prop)
    {
        int minPoolSize = Integer.parseInt(prop.getProperty("min_pool_size", "5"));
        int maxPoolSize = Integer.parseInt(prop.getProperty("max_pool_size", "10"));
        int keepAliveTime = Integer.parseInt(prop.getProperty("keep_alive_secs", "10"));

        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable,
                        "Message_Handler_Pool_Thread_" + threadId.incrementAndGet());
                if (thread.isDaemon())
                    thread.setDaemon(false);
                if (thread.getPriority() != Thread.NORM_PRIORITY)
                    thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };

        ExecutorService taskExecutor = new ThreadPoolExecutor(minPoolSize,
                 maxPoolSize,
                 keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);

        // ExecutorService executorService1 = Executors.newSingleThreadExecutor();

        return taskExecutor;
    }

    public static Properties loadProperties(String propFile) throws Exception
    {
        Properties prop = new Properties();
        InputStream input = null;

        try
        {
            input = new FileInputStream(propFile);
            // load a properties file
            prop.load(input);

        }
        finally
        {
            if (input != null)
                input.close();
        }
        return prop;
    }

    public static void usage()
    {
        String usageString =
        "Usage: runjmsclient.sh [-durable|-queue] [-Dproperty.file=<properties file>] [-Dlog.file.path=<logfile directory>] " +
              "[-Dlog.file=<logfile name>] [-Dclient.id=<clientid>] [-Dclient.name=<clientName>] <hostname_or_ip> [jms_port]" + NL +
        "The only mandatory argument is the JMS Server host name or ip" + NL +
        "    hostname_or_ip can be specified in the format:" + NL +
        "         h1_or_ip1[,h2_or_ip2,h3_or_ip3]" + NL +
        "         up to three VSD hosts in the cluster can be specified separated by comma with no spaces around the comma." + NL +
        "         If connection to a host fails, the next available in the list will be tried. This happens even if the" + NL +
        "         connection fails while the JMS client is running" + NL +
        NL +
        "    -durable : to establish durable connection. The client.name and client.id parameters are mandatory for durable connections" + NL +
        NL +
        "    -queue : to establish a connection to a JMS queue" + NL +
        NL +
        "    default jms_port : 61616" + NL +
        NL +
        "    default property.file : <script directory>/jmsclient.properties" + NL +
        "        If the property.file is specified and does not start with absolute path name," + NL +
        "        it is searched in the classpath which may not be the current directory" + NL +
        "        The property.file contains JMS client parameters such as user name, password, any topic filters etc." + NL +
        NL +
        "    default log.file.path : <script directory>/log" + NL +
        NL +
        "    default log.file : jmsclient.log" + NL +
        NL +
        "    default client.id : random uuid, for durable connections, this value must be supplied on the command line"
        ;
        System.err.println(usageString);
    }
}
