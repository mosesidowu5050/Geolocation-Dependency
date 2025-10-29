package org.mosesidowu.geolocation_core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NearbyPlaceResponse {
    private String status;
    private String message;
    private List<NearbyPlace> places;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NearbyPlace {
        private String name;
        private String address;
        private double latitude;
        private double longitude;
    }
}
