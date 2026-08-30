package com.lapwise.lapwise_backend.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
            cwd.resolve(".env"),
            cwd.resolve("..").resolve(".env").normalize()
        };
        for (Path file : candidates) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Path dir = file.getParent();
            if (dir == null) {
                continue;
            }
            Dotenv dotenv = Dotenv.configure()
                .directory(dir.toAbsolutePath().toString())
                .filename(".env")
                .ignoreIfMalformed()
                .load();
            Map<String, Object> map = new LinkedHashMap<>();
            dotenv.entries().forEach(entry -> {
                String value = entry.getValue();
                if (value != null) {
                    map.put(entry.getKey(), value.trim());
                }
            });
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvFile", map));
            return;
        }
    }
}
