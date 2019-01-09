/*
 ***************************************************************************
 *
 * Filename    : JMSMessageHandler
 * Author      : Bala Ramakrishnan
 * Date        : 2016-12-06
 * Description : JMS Client code example
 *
 ****************************************************************************
 */

package com.alu.cna.cloudmgmt.util.jms;

import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JMSMessageHandler implements MessageListener
{
    private static final Logger log = LoggerFactory.getLogger(JMSMessageHandler.class.getName());

    private ExecutorService taskPool;
    public JMSMessageHandler(ExecutorService taskPool)
    {
        this.taskPool = taskPool;
    }

    @Override
    public void onMessage(final Message msg)
    {
        try
        {
            taskPool.submit(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    return processMessage(msg);
                }
            });
        }
        catch (RejectedExecutionException e)
        {
            log.error("error while submitting message task, message: {}", msg, e);
        }
    }

    protected boolean processMessage(Message msg)
    {
        // simply print the message for now
        log.debug("message type: {}", msg.getClass().getSimpleName());
        StringBuilder params = new StringBuilder("+++ Params:\n");
        try
        {
            @SuppressWarnings("unchecked")
            Enumeration<String> e = msg.getPropertyNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                params.append(key);
                params.append("=");
                params.append(msg.getObjectProperty(key));
                params.append("\n");
            }
            log.debug("message params: {}", params.toString());
            log.debug("message ID: {}", msg.getJMSMessageID());
            if (msg instanceof TextMessage)
            {
                log.debug("message body: {}", ((TextMessage) msg).getText());
            }
            else
            {
                log.debug("message body: {}", msg.toString());
            }
            return true;
        }
        catch (JMSException e)
        {
            log.error(e.getMessage(), e);
            return false;
        }
    }

}
