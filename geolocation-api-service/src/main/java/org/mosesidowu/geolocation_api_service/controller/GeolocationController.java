package org.mosesidowu.geolocation_api_service.controller;


import org.mosesidowu.geolocation_core.dto.request.AddressRequest;
import org.mosesidowu.geolocation_core.dto.response.AddressValidationResponse;
import org.mosesidowu.geolocation_core.dto.response.CoordinatesResponse;
import org.mosesidowu.geolocation_core.dto.response.NearbyPlaceResponse;
import org.mosesidowu.geolocation_core.service.GoogleService;
import org.mosesidowu.geolocation_core.service.GoogleServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geolocation")
public class GeolocationController {

    private final GoogleServiceInterface googleServiceInterface;

    public GeolocationController(GoogleService googleMapsService) {
        this.googleServiceInterface = googleMapsService;
    }

    @PostMapping("/validate-address")
    public ResponseEntity<AddressValidationResponse> validateAddress(@RequestBody AddressRequest request) {
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            return new ResponseEntity<>(
                    AddressValidationResponse.builder()
                            .originalAddress(request.getAddress())
                            .isValid(false)
                            .validationMessage("Address cannot be empty.")
                            .build(),
                    HttpStatus.BAD_REQUEST);
        }
        AddressValidationResponse response = googleServiceInterface.validateAndGeocodeAddress(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/coordinates")
    public ResponseEntity<CoordinatesResponse> getCoordinates(@RequestParam String address) {
        if (address == null || address.trim().isEmpty()) {
            return new ResponseEntity<>(
                    CoordinatesResponse.builder()
                            .originalAddress(address)
                            .message("Address cannot be empty.")
                            .build(),
                    HttpStatus.BAD_REQUEST);
        }
        CoordinatesResponse response = googleServiceInterface.getCoordinates(address);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/nearby/address")
    public ResponseEntity<?> getNearbyPlacesByAddress(
            @RequestParam String address,
            @RequestParam(defaultValue = "1500") int radius,
            @RequestParam(required = false) String type) {

        CoordinatesResponse coordinates = googleServiceInterface.getCoordinates(address);

        if (coordinates.getLatitude() != null && coordinates.getLongitude() != null) {
            NearbyPlaceResponse response = googleServiceInterface.findNearbyPlaces(
                    Double.parseDouble(coordinates.getLatitude()),
                    Double.parseDouble(coordinates.getLongitude()),
                    radius,
                    type
            );
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(
                NearbyPlaceResponse.builder()
                        .status("ERROR")
                        .message("Invalid or unrecognized address.")
                        .build()
        );
    }
}
