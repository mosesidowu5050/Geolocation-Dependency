package org.mosesidowu.geolocation_api_service.controller;


import org.mosesidowu.geolocation_core.dto.request.AddressRequest;
import org.mosesidowu.geolocation_core.dto.response.AddressValidationResponse;
import org.mosesidowu.geolocation_core.dto.response.CoordinatesResponse;
import org.mosesidowu.geolocation_core.service.GoogleMapsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geolocation")
public class GeolocationController {

    private final GoogleMapsService googleMapsService;

    public GeolocationController(GoogleMapsService googleMapsService) {
        this.googleMapsService = googleMapsService;
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
        AddressValidationResponse response = googleMapsService.validateAndGeocodeAddress(request);
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
        CoordinatesResponse response = googleMapsService.getCoordinates(address);
        return ResponseEntity.ok(response);
    }
}
