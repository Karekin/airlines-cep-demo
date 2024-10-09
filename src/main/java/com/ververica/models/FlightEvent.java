package com.ververica.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightEvent {
    private String flightId;
    private String eventType;
    private String airportCode;
    private String details;
    private long timestamp;
}
