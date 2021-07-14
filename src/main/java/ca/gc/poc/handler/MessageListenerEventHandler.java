package ca.gc.poc.handler;

import ca.gc.poc.utils.CuramUtil;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

/**
 * The type Message listener event handler.
 */
public class MessageListenerEventHandler implements XMLMessageListener {

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(MessageListenerEventHandler.class);

    /**
     * The Event handler count down latch.
     */
    private final CountDownLatch eventHandlerCountDownLatch = new CountDownLatch(1);

    @Override
    public void onReceive(BytesXMLMessage msg) {
        if (msg instanceof TextMessage) {
            logger.info(LocalDateTime.now() + " - Consumer received message : " + ((TextMessage) msg).getText());

            try {

                if (!msg.getProperties().containsKey("destination")) {
                    throw new Exception("Destination isn't set in the message properties. Destination must be set in order to reach a Curam endpoint.");
                }
                String response = CuramUtil
                        .getInstance()
                        .sendToService(((TextMessage) msg).getText(), ((String) msg.getProperties().get("destination")));

                logger.info(LocalDateTime.now() + " - Response received from Curam: " + response);

                eventHandlerCountDownLatch.countDown();
            } catch (Exception e) {
                logger.error(LocalDateTime.now() + " - Error received from Curam: " + e.getMessage());
            }
        }
    }

    @Override
    public void onException(JCSMPException e) {
        logger.error(LocalDateTime.now() + " - Consumer received exception : " + e.getMessage());

        eventHandlerCountDownLatch.countDown();
    }
}
