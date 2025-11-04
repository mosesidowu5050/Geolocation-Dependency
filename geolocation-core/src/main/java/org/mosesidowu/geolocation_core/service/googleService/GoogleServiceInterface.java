package org.mosesidowu.geolocation_core.service.googleService;

import org.mosesidowu.geolocation_core.dto.request.AddressRequest;
import org.mosesidowu.geolocation_core.dto.response.AddressValidationResponse;
import org.mosesidowu.geolocation_core.dto.response.CoordinatesResponse;
import org.mosesidowu.geolocation_core.dto.response.NearbyPlaceResponse;
import org.springframework.stereotype.Component;

@Component
public interface GoogleServiceInterface {

    AddressValidationResponse validateAndGeocodeAddress(AddressRequest addressRequest);

    CoordinatesResponse getCoordinates(String address);

    NearbyPlaceResponse findNearbyPlaces(double latitude, double longitude, int radius, String type);

    }
