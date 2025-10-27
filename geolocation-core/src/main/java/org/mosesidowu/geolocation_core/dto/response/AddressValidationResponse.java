package org.mosesidowu.geolocation_core.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressValidationResponse {

    private String originalAddress;
    private String normalizedAddress;
    private boolean isValid;
    private String latitude;
    private String longitude;
    private String postalCode;
    private String city;
    private String state;
    private String country;
    private String validationMessage;
}
