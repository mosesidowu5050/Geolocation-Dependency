package org.mosesidowu.geolocation_core.dto.request;

import lombok.Data;

@Data
public class AddressRequest {

    private String address;
    private String countryCode;
}
