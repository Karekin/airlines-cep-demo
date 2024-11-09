package com.ververica.cep;

import com.ververica.config.AppConfig;
import com.ververica.models.FlightEvent;
import com.ververica.serdes.FlightEventSerdes;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.ververica.config.AppConfig.buildSecurityProps;

public class FlightCEPRunner {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);

        var properties = buildSecurityProps(new Properties());

        // 1 Create Kafka/Flink Consumer
        KafkaSource<FlightEvent> flightEventKafkaSource = KafkaSource.<FlightEvent>builder()
                .setBootstrapServers(AppConfig.BOOTSTRAP_URL)
                .setTopics(AppConfig.FLIGHTEVENTS_TOPIC)
                .setGroupId("flightevents.consumer")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new FlightEventSerdes())
                .setProperties(properties)
                .build();

        WatermarkStrategy<FlightEvent> watermarkStrategy = WatermarkStrategy
                .<FlightEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp());

        DataStream<FlightEvent> flightEventStream = environment
                .fromSource(flightEventKafkaSource, watermarkStrategy, "Flight Events Source")
                .name("FlightEventsSource")
                .uid("FlightEventsSource");

        // 2. Generate flight events
        // Pattern 1: Detect security incidents
        Pattern<FlightEvent, ?> securityPattern = Pattern.<FlightEvent>begin("securityAlert")
                .where(new IterativeCondition<FlightEvent>() {
                    @Override
                    public boolean filter(FlightEvent event, Context<FlightEvent> ctx) throws Exception {
                        return event.getEventType().equals("SECURITY");
                    }
                });

        // Pattern 2: Detect severe weather alerts
        Pattern<FlightEvent, ?> weatherPattern = Pattern.<FlightEvent>begin("weatherAlert")
                .where(new IterativeCondition<FlightEvent>() {
                    @Override
                    public boolean filter(FlightEvent event, Context<FlightEvent> ctx) throws Exception {
                        return event.getEventType().equals("WEATHER") && event.getDetails().contains("storm");
                    }
                });

        // Pattern 3: Detect turnaround efficiency
        Pattern<FlightEvent, ?> turnaroundPattern = Pattern.<FlightEvent>begin("arrival")
                .where(new IterativeCondition<FlightEvent>() {
                    @Override
                    public boolean filter(FlightEvent event, Context<FlightEvent> ctx) throws Exception {
                        return event.getEventType().equals("LANDING");
                    }
                })
                .next("departure")
                .where(new IterativeCondition<FlightEvent>() {
                    @Override
                    public boolean filter(FlightEvent event, Context<FlightEvent> ctx) throws Exception {
                        FlightEvent arrivalEvent = ctx.getEventsForPattern("arrival").iterator().next();

                        return event.getEventType().equals("TAKEOFF") &&
                                event.getFlightId().equals(arrivalEvent.getFlightId()) &&
                                event.getAirportCode().equals(arrivalEvent.getAirportCode()) &&
                                (event.getTimestamp() - arrivalEvent.getTimestamp()) <= 45 * 60 * 1000; // 45 minutes
                    }
                });

        // 3. Apply the pattern to the event stream
        PatternStream<FlightEvent> patternStream = CEP.pattern(flightEventStream, turnaroundPattern);
        PatternStream<FlightEvent> securityPatternStream = CEP.pattern(flightEventStream, securityPattern);
        PatternStream<FlightEvent> weatherPatternStream = CEP.pattern(flightEventStream, weatherPattern);

        // 4. Select functions for each pattern
        DataStream<String> securityAlerts = securityPatternStream.select((PatternSelectFunction<FlightEvent, String>) pattern -> {
            FlightEvent securityEvent = pattern.get("securityAlert").get(0);
            return "Security Alert: Incident detected at " + securityEvent.getAirportCode() + " for Flight " + securityEvent.getFlightId() +
                    " - " + securityEvent.getDetails();
        }).name("SecurityAlertsPattern").uid("SecurityAlertsPattern");

        DataStream<String> weatherAlerts = weatherPatternStream.select(new PatternSelectFunction<FlightEvent, String>() {
            @Override
            public String select(Map<String, List<FlightEvent>> pattern) {
                FlightEvent weatherEvent = pattern.get("weatherAlert").get(0);
                return "Weather Alert: Severe weather condition at " + weatherEvent.getAirportCode() + " - " + weatherEvent.getDetails();
            }
        }).name("WeatherAlertsPattern").uid("WeatherAlertsPattern");

        DataStream<String> turnaroundAlerts = patternStream.select(new PatternSelectFunction<FlightEvent, String>() {
            @Override
            public String select(Map<String, List<FlightEvent>> pattern) {
                FlightEvent arrival = pattern.get("arrival").get(0);
                FlightEvent departure = pattern.get("departure").get(0);
                return "Turnaround Efficiency Achieved: Flight " + arrival.getFlightId() +
                        " landed at " + arrival.getAirportCode() + " and took off for " + departure.getAirportCode() + " within 45 minutes.";
            }
        }).name("TurnaroundAlertsPattern").uid("TurnaroundAlertsPattern");

        // 5. Union and print all alerts
        securityAlerts
                .union(weatherAlerts, turnaroundAlerts)
                .print()
                .name("print")
                .uid("print");

        environment.execute("Flight CEP Demo with Security, Weather and Turnaround Alerts");
    }
}
