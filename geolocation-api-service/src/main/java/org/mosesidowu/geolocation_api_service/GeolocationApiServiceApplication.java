package org.mosesidowu.geolocation_api_service;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"org.mosesidowu.geolocation_core",
		"org.mosesidowu.geolocation_api_service"
})public class GeolocationApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeolocationApiServiceApplication.class, args);
	}

}
