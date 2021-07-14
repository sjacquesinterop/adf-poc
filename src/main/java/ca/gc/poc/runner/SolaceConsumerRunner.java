package ca.gc.poc.runner;

import ca.gc.poc.handler.MessageListenerEventHandler;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

/**
 * The type Solace consumer runner.
 */
@Component
public class SolaceConsumerRunner implements CommandLineRunner {

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SolaceConsumerRunner.class);
    /**
     * The Message listener.
     */
    private final MessageListenerEventHandler messageListener = new MessageListenerEventHandler();

    /**
     * The Solace host.
     */
    @Value("${solace.java.host}")
    private String solaceHost;

    /**
     * The Solace vpn.
     */
    @Value("${solace.java.vpn}")
    private String solaceVpn;

    /**
     * The Solace username.
     */
    @Value("${solace.java.client.username}")
    private String solaceUsername;

    /**
     * The Solace password.
     */
    @Value("${solace.java.client.password}")
    private String solacePassword;

    /**
     * The Solace consumption.
     */
    @Value("${solace.java.consumption}")
    private String solaceConsumption;

    /**
     * The Solace topic.
     */
    @Value("${solace.java.topic}")
    private String solaceTopic;

    /**
     * The Solace queue.
     */
    @Value("${solace.java.queue}")
    private String solaceQueue;

    @Override
    public void run(String... args) {
        // Retrieve properties
        JCSMPProperties jcsmpProperties = new JCSMPProperties();
        jcsmpProperties.setProperty(JCSMPProperties.HOST, solaceHost);
        jcsmpProperties.setProperty(JCSMPProperties.VPN_NAME, solaceVpn);
        jcsmpProperties.setProperty(JCSMPProperties.USERNAME, solaceUsername);
        jcsmpProperties.setProperty(JCSMPProperties.PASSWORD, solacePassword);

        // Instantiate variables that need to be closed
        JCSMPSession session = null;
        XMLMessageConsumer xmlMessageConsumer = null;
        FlowReceiver flowReceiver = null;

        // Instantiate JCSMP variables
        MessageListenerEventHandler messageListenerEventHandler;
        Topic topic;
        Queue queue;

        try {
            // Create session and event handler
            session = JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            messageListenerEventHandler = new MessageListenerEventHandler();

            // Listen to topic
            if (solaceConsumption.equalsIgnoreCase("topic")) {
                topic = JCSMPFactory.onlyInstance().createTopic(solaceTopic);
                xmlMessageConsumer = session.getMessageConsumer(messageListenerEventHandler);
                session.addSubscription(topic);
                xmlMessageConsumer.start();
            }

            // Listen to queue
            if (solaceConsumption.equalsIgnoreCase("queue")) {
                queue = JCSMPFactory.onlyInstance().createQueue(solaceQueue);
                final ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
                consumerFlowProperties.setEndpoint(queue);
                flowReceiver = session.createFlow(messageListenerEventHandler, consumerFlowProperties);
                flowReceiver.start();
            }

            // Await messages
            countDownLatch.await();

        } catch (Exception e) {
            logger.error(LocalDateTime.now() + " - Runner encountered an exception: " + e.getMessage());
        }


    }
}