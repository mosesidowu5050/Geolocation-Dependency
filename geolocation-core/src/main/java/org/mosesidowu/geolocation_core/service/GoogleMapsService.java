package org.mosesidowu.geolocation_core.service;

import org.mosesidowu.geolocation_core.dto.request.AddressRequest;
import org.mosesidowu.geolocation_core.dto.response.AddressValidationResponse;
import org.mosesidowu.geolocation_core.dto.response.CoordinatesResponse;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class GoogleMapsService {

    private final RestTemplate restTemplate;
    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private static final String GOOGLE_GEOCODING_API_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    public GoogleMapsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "addressValidationCache", key = "#addressRequest.address + '_' + #addressRequest.countryCode")
    public AddressValidationResponse validateAndGeocodeAddress(AddressRequest addressRequest) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODING_API_BASE_URL)
                .queryParam("address", addressRequest.getAddress())
                .queryParam("key", googleMapsApiKey);

        if (addressRequest.getCountryCode() != null && !addressRequest.getCountryCode().isEmpty()) {
            uriBuilder.queryParam("components", "country:" + addressRequest.getCountryCode());
        }

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonNode.class);
        JsonNode root = responseEntity.getBody();

        return parseGeocodingResponseForValidation(addressRequest.getAddress(), root);
    }

    @Cacheable(value = "coordinatesCache", key = "#address")
    public CoordinatesResponse getCoordinates(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODING_API_BASE_URL)
                .queryParam("address", address)
                .queryParam("key", googleMapsApiKey);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonNode.class);
        JsonNode root = responseEntity.getBody();

        return parseGeocodingResponseForCoordinates(address, root);
    }

    private AddressValidationResponse parseGeocodingResponseForValidation(String originalAddress, JsonNode root) {
        AddressValidationResponse.AddressValidationResponseBuilder builder = AddressValidationResponse.builder()
                .originalAddress(originalAddress)
                .isValid(false)
                .validationMessage("Address not found or invalid.");

        if (root != null && "OK".equals(root.get("status").asText())) {
            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                JsonNode geometry = firstResult.get("geometry");
                JsonNode location = geometry != null ? geometry.get("location") : null;
                JsonNode addressComponents = firstResult.get("address_components");

                if (location != null) {
                    builder.latitude(location.get("lat").asText())
                            .longitude(location.get("lng").asText());
                }

                if (addressComponents != null && addressComponents.isArray()) {
                    for (JsonNode component : addressComponents) {
                        String type = component.get("types").get(0).asText();
                        String longName = component.get("long_name").asText();
                        switch (type) {
                            case "postal_code":
                                builder.postalCode(longName);
                                break;
                            case "locality":
                                builder.city(longName);
                                break;
                            case "administrative_area_level_1":
                                builder.state(longName);
                                break;
                            case "country":
                                builder.country(longName);
                                break;
                        }
                    }
                }

                builder.normalizedAddress(firstResult.get("formatted_address").asText());
                builder.isValid(true);
                builder.validationMessage("Address validated successfully.");
            }
        }
        return builder.build();
    }

    private CoordinatesResponse parseGeocodingResponseForCoordinates(String originalAddress, JsonNode root) {
        CoordinatesResponse.CoordinatesResponseBuilder builder = CoordinatesResponse.builder()
                .originalAddress(originalAddress)
                .message("Address not found or could not be geocoded.");

        if (root != null && "OK".equals(root.get("status").asText())) {
            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                JsonNode geometry = firstResult.get("geometry");
                JsonNode location = geometry != null ? geometry.get("location") : null;

                if (location != null) {
                    builder.latitude(location.get("lat").asText())
                            .longitude(location.get("lng").asText());
                    builder.message("Successfully geocoded.");
                }
            }
        }
        return builder.build();
    }
}