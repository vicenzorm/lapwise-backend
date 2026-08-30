package com.lapwise.lapwise_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;
import com.lapwise.lapwise_backend.domain.usecase.UserService;

/**
 * Composition root. Wires adapters onto domain ports. Stays outside domain.
 */
@SpringBootApplication
public class LapwiseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LapwiseBackendApplication.class, args);
	}

	@Bean
	UserService userService(UserRepositoryPort userRepositoryPort, StravaAuthPort stravaAuthPort) {
		return new UserService(userRepositoryPort, stravaAuthPort);
	}

}
