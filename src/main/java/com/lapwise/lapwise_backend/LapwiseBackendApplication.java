package com.lapwise.lapwise_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root. Wires adapters onto domain ports. Stays outside domain.
 */
@SpringBootApplication
public class LapwiseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LapwiseBackendApplication.class, args);
	}

}
