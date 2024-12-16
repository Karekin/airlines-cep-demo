package com.ververica.producer;

import com.ververica.models.FlightEvent;
import com.ververica.utils.FlightEventGenerator;
import com.ververica.config.AppConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

import static com.ververica.config.AppConfig.buildProducerProps;

public class FlightEventsProducer {
    private static final Logger logger
            = LoggerFactory.getLogger(FlightEventsProducer.class);

    private static final int TOTAL_EVENTS = 100;

    public static void main(String[] args) {
        Properties properties = buildProducerProps();
        List<FlightEvent> generatedEvents = FlightEventGenerator.generateFlightEvents(TOTAL_EVENTS);

        logger.info("Starting Kafka Producers  ...");
        logger.info(properties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        KafkaProducer<String, FlightEvent> flightEventProducer = new KafkaProducer<>(properties);

        logger.info("Sending {} flight events ...", TOTAL_EVENTS);

        int count = 0;
        for (FlightEvent flightEvent : generatedEvents) {
            ProducerRecord<String, FlightEvent> record = new ProducerRecord<>(
                    AppConfig.FLIGHTEVENTS_TOPIC,
                    flightEvent.getFlightId(),
                    flightEvent
            );

            flightEventProducer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        logger.error("Error while producing: ", exception);
                    } else {
                        // Uncomment for detailed logging
                        // logger.info("Successfully stored offset '{}': partition: {} - {}", metadata.offset(), metadata.partition(), metadata.topic());
                    }
                }
            });

            count += 1;
            if (count % 1000 == 0) {
                logger.info("Total so far {}.", count);
            }
        }

        logger.info("Closing Producers ...");
        flightEventProducer.flush();
        flightEventProducer.close();
    }
}
