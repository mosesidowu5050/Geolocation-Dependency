package org.mosesidowu.geolocation_core.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoordinatesResponse {
    private String originalAddress;
    private String latitude;
    private String longitude;
    private String message;
}