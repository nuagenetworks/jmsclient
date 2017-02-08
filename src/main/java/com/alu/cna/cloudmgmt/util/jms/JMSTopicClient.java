/*
 ***************************************************************************
 *
 * Filename    : JMSClient
 * Author      : Bala Ramakrishnan
 * Date        : 2016-12-06
 * Description : Non-durable JMS Client code example
 *
 ****************************************************************************
 */

package com.alu.cna.cloudmgmt.util.jms;
import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JMSTopicClient extends AbstractJMSClient  implements ExceptionListener
{
    private static final Logger log = LoggerFactory.getLogger(JMSTopicClient.class.getName());

    public JMSTopicClient()
    {
    }

    public static void main(String[] args) throws Exception
    {
        _mainThread = Thread.currentThread();
        parseArgs(args);
        new JMSTopicClient().execute();
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
    public MessageConsumer createSubscriber() throws JMSException
    {
        TopicSubscriber recv = ((TopicSession) session).createSubscriber((Topic) topic, messageSelector, true);
        log.debug("Created non-durable subscriber");
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
        }
        catch (Exception e)
        {
            log.error("Exception while unsubscribing to topic: {}", topicName, e);
        }
        finally
        {
            subscriber = null;
            super.releaseResources();
        }
    }
}
