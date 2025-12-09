package com.fieldforcepro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReverseGeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;

    public ReverseGeocodingService(
            @Value("${fieldforcepro.geocode.google-api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
    }

    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            // No API key configured; skip reverse geocoding.
            return null;
        }
        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                    latitude, longitude, apiKey
            );
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }
            JsonNode first = results.get(0);
            JsonNode formatted = first.get("formatted_address");
            if (formatted == null || formatted.isNull()) {
                return null;
            }
            return formatted.asText();
        } catch (Exception e) {
            // Fail silently for now; we don't want geocoding errors to break attendance.
            return null;
        }
    }
}
