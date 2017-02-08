/*
 ***************************************************************************
 *
 * Filename    : DurableJMSClient
 * Author      : Bala Ramakrishnan
 * Date        : 2016-12-06
 * Description : Durable JMS Client code example
 *
 ****************************************************************************
 */

package com.alu.cna.cloudmgmt.util.jms;
import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DurableJMSTopicClient extends AbstractJMSClient  implements ExceptionListener
{
    private static final Logger log = LoggerFactory.getLogger(DurableJMSTopicClient.class.getName());

    protected String clientName;

    public DurableJMSTopicClient(String clientName)
    {
        this.clientName = clientName;
    }

    public static void main(String[] args)
    {
        _mainThread = Thread.currentThread();
        String clientName = System.getProperty("client.name");
        if (StringUtils.isBlank(clientName))
        {
            clientName = DurableJMSTopicClient.class.getSimpleName();
            System.setProperty("client.name", clientName);
        }

        parseArgs(args);
        new DurableJMSTopicClient(clientName).execute();
    }

    @Override
    protected TopicConnectionFactory getConnectionFactory(InitialContext iniCtx) throws Exception
    {
        return (TopicConnectionFactory) iniCtx.lookup(jmsRemoteFactory);
    }

    @Override
    protected Topic getDestination(InitialContext ctx, String topicName) throws Exception
    {
        return (Topic) ctx.lookup(topicName);
    }

    @Override
    protected TopicSession createSession(Connection conn) throws Exception
    {
        return ((TopicConnection) conn).createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
    }

    @Override
    Logger getLogger()
    {
        return log;
    }

    @Override
    public TopicSubscriber createSubscriber() throws JMSException
    {
        // clientName

        TopicSubscriber recv = session.createDurableSubscriber((Topic) topic, clientName, messageSelector, true);
        log.debug("Created Durable subscriber for name: {}, clientId: {}", clientName, clientId);
        return recv;
    }

    @Override
    protected void releaseResources()
    {
        try
        {
            log.debug("unsubscribing to topic: {}", topicName);
            if (subscriber != null)
                subscriber.close();

            log.debug("unsubscribing durable client name: {}", clientName);
            if (session != null)
                session.unsubscribe(clientName);
        }
        catch (Exception e)
        {
            log.error("Exception while unsubscribing durable client: {}", clientName, e);
        }
        finally
        {
            subscriber = null;
            super.releaseResources();
        }
    }
}
