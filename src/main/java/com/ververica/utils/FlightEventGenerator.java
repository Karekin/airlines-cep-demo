package com.ververica.utils;

import com.ververica.models.FlightEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlightEventGenerator {
    private static final String[] FLIGHT_IDS = {"EK201", "EK202", "EK203", "EK204", "EK205", "EK206", "EK207", "EK208", "EK209", "EK210"};
    private static final String[] EVENT_TYPES = {"TAKEOFF", "LANDING", "DELAY", "SECURITY", "WEATHER"};
    private static final String[] AIRPORT_CODES = {"DXB", "JFK", "LHR", "LAX"};
    private static final String[] DETAILS_TAKEOFF = {"On-time departure", "Runway congestion", "Delayed due to heavy rain"};
    private static final String[] DETAILS_LANDING = {"Smooth landing", "Diverted due to weather", "Delayed landing"};
    private static final String[] DETAILS_DELAY = {"Maintenance issue", "Technical check", "Crew availability issue"};
    private static final String[] DETAILS_SECURITY = {"Unattended luggage", "Suspicious activity", "Bomb threat investigation"};
    private static final String[] DETAILS_WEATHER = {"Heavy storm warning", "Sandstorm approaching", "Snowstorm expected"};

    public static List<FlightEvent> generateFlightEvents(int numberOfEvents) {
        List<FlightEvent> events = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numberOfEvents; i++) {
            String flightId = FLIGHT_IDS[random.nextInt(FLIGHT_IDS.length)];
            String eventType = EVENT_TYPES[random.nextInt(EVENT_TYPES.length)];
            String airportCode = AIRPORT_CODES[random.nextInt(AIRPORT_CODES.length)];
            String details = getRandomDetails(eventType, random);

            FlightEvent event = new FlightEvent(flightId, eventType, airportCode, details, System.currentTimeMillis());
            events.add(event);
        }

        // Add specific security and weather events to ensure alerts are triggered
        events.add(new FlightEvent("EK300", "SECURITY", "DXB", "Unattended luggage", System.currentTimeMillis()));
        events.add(new FlightEvent("EK301", "WEATHER", "JFK", "Heavy storm warning", System.currentTimeMillis()));

        events.add(new FlightEvent("EK201", "LANDING", "DXB", "Smooth landing", System.currentTimeMillis() - 60 * 60 * 1000));
        events.add(new FlightEvent("EK201", "TAKEOFF", "DXB", "On-time departure", System.currentTimeMillis() - 30 * 60 * 1000));
        events.add(new FlightEvent("EK201", "TAKEOFF", "DXB", "On-time departure", System.currentTimeMillis() - 60 * 60 * 1000));

        return events;
    }

    private static String getRandomDetails(String eventType, Random random) {
        switch (eventType) {
            case "TAKEOFF":
                return DETAILS_TAKEOFF[random.nextInt(DETAILS_TAKEOFF.length)];
            case "LANDING":
                return DETAILS_LANDING[random.nextInt(DETAILS_LANDING.length)];
            case "DELAY":
                return DETAILS_DELAY[random.nextInt(DETAILS_DELAY.length)];
            case "SECURITY":
                return DETAILS_SECURITY[random.nextInt(DETAILS_SECURITY.length)];
            case "WEATHER":
                return DETAILS_WEATHER[random.nextInt(DETAILS_WEATHER.length)];
            default:
                return "No details available";
        }
    }
}
