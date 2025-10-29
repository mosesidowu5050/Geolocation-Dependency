package org.mosesidowu.geolocation_core.service.googleService;

import lombok.RequiredArgsConstructor;
import org.mosesidowu.geolocation_core.dto.request.AddressRequest;
import org.mosesidowu.geolocation_core.dto.response.AddressValidationResponse;
import org.mosesidowu.geolocation_core.dto.response.CoordinatesResponse;
import org.mosesidowu.geolocation_core.dto.response.NearbyPlaceResponse;
import org.mosesidowu.geolocation_core.service.rateLimiterService.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleService implements GoogleServiceInterface {

    private final RestTemplate restTemplate;
    private final RateLimiterService rateLimiterService;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private static final String GOOGLE_GEOCODING_API_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String GOOGLE_PLACE_API_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    public GoogleService(RestTemplate restTemplate, RateLimiterService rateLimiterService) {
        this.restTemplate = restTemplate;
        this.rateLimiterService = rateLimiterService;
    }


    @Override
    @Cacheable(value = "addressValidationCache", key = "#addressRequest.address + '_' + #addressRequest.countryCode")
    public AddressValidationResponse validateAndGeocodeAddress(AddressRequest addressRequest) {
        rateLimiterService.consumeToken(addressRequest.getUserId());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODING_API_BASE_URL)
                .queryParam("address", addressRequest.getAddress())
                .queryParam("key", googleMapsApiKey);

        if (addressRequest.getCountryCode() != null && !addressRequest.getCountryCode().isEmpty())
            uriBuilder.queryParam("components", "country:" + addressRequest.getCountryCode());

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonNode.class);
        JsonNode root = responseEntity.getBody();

        return parseGeocodingResponseForValidation(addressRequest.getAddress(), root);
    }


    @Override
    @Cacheable(value = "coordinatesCache", key = "#address")
    public CoordinatesResponse getCoordinates(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODING_API_BASE_URL)
                .queryParam("address", address)
                .queryParam("key", googleMapsApiKey);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonNode.class);
        JsonNode root = responseEntity.getBody();

        return parseGeocodingResponseForCoordinates(address, root);
    }


    @Override
    @Cacheable(value = "nearbyPlacesCache", key = "#latitude + '_' + #longitude + '_' + #radius + '_' + #type")
    public NearbyPlaceResponse findNearbyPlaces(double latitude, double longitude, int radius, String type) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(GOOGLE_PLACE_API_BASE_URL)
                .queryParam("location", latitude + "," + longitude)
                .queryParam("radius", radius)
                .queryParam("key", googleMapsApiKey);

        if (type != null && !type.isEmpty())
            uriBuilder.queryParam("type", type);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonNode.class);
        JsonNode root = responseEntity.getBody();

        return parseNearbyPlacesResponse(root);
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


    private NearbyPlaceResponse parseNearbyPlacesResponse(JsonNode root) {
        NearbyPlaceResponse response = new NearbyPlaceResponse();
        List<NearbyPlaceResponse.NearbyPlace> places = new ArrayList<>();

        if (root == null || root.get("results") == null) {
            response.setStatus("FAILED");
            response.setMessage("No results found or invalid response.");
            response.setPlaces(places);
            return response;
        }

        JsonNode results = root.get("results");
        for (JsonNode place : results) {
            String name = place.path("name").asText("Unknown");
            String address = place.path("vicinity").asText("No address available");

            double lat = place.path("geometry").path("location").path("lat").asDouble();
            double lng = place.path("geometry").path("location").path("lng").asDouble();

            places.add(new NearbyPlaceResponse.NearbyPlace(name, address, lat, lng));
        }

        String status = root.path("status").asText("UNKNOWN_STATUS");
        response.setStatus(status);

        if ("OK".equals(status)) {
            response.setMessage("Nearby places fetched successfully.");
        } else {
            response.setMessage("Failed to fetch nearby places. Status: " + status);
        }

        response.setPlaces(places);
        return response;
    }
}
