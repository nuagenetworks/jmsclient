/*
 ***************************************************************************
 *
 * Filename    : JMSQueueClient
 * Author      : Natalia Balus
 * Date        : 2017-01-05
 * Description : 
 *
 ****************************************************************************
 */
package com.alu.cna.cloudmgmt.util.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

public class JMSQueueClient extends AbstractJMSClient implements ExceptionListener
{
    private static final Logger log = LoggerFactory.getLogger(JMSQueueClient.class);

    public static void main(String[] args) throws Exception
    {
        _mainThread = Thread.currentThread();
        parseArgs(args);
        new JMSQueueClient().execute();
    }

    @Override
    protected QueueConnectionFactory getConnectionFactory(InitialContext iniCtx) throws Exception
    {
        return (QueueConnectionFactory) iniCtx.lookup(jmsRemoteFactory);
    }

    @Override
    protected Queue getDestination(InitialContext ctx, String topicName) throws Exception
    {
        return (Queue) ctx.lookup(topicName);
    }

    @Override
    protected QueueSession createSession(Connection conn) throws Exception
    {
        return ((QueueConnection) conn).createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
    }

    @Override
    Logger getLogger()
    {
        return log;
    }

    @Override
    public QueueReceiver createSubscriber() throws JMSException
    {
        QueueReceiver recv = ((QueueSession) session).createReceiver((Queue) topic, messageSelector);
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
