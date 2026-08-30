package com.lapwise.lapwise_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lapwiseOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Lapwise API")
                .version("0.0.1")
                .contact(new Contact().name("Lapwise"))
                .description("""
                    Hand-written REST for the Lapwise iOS client. Swagger describes \
                    this API; it does not generate the service.

                    ## Tokens

                    Strava access and refresh tokens stay on the User row in Postgres. \
                    The iOS app never stores them. After OAuth, `GET /auth/strava/callback` \
                    returns a Lapwise session JWT (`sessionToken`). Send it as \
                    `Authorization: Bearer <sessionToken>` on every later route (`POST /sync`, \
                    `GET /swim-activities`, …). `sub` is the Lapwise user UUID. The token lives 30 days. \
                    `401` means that JWT is missing, invalid, or expired — not that Strava failed.

                    ## OAuth in Swagger

                    Do not use Try it out on `GET /auth/strava/authorize`. The browser must follow \
                    the 302 to Strava (cookies + consent). Try it out hits CORS / does not complete \
                    the redirect. Open `/auth/strava/authorize` in the browser instead.

                    Do not call callback from Swagger without a real `code` and the `lapwise_oauth_state` \
                    cookie from authorize. You will get `missing_code` or `invalid_state`.

                    ## Errors

                    JSON errors use `{ "error": "<stable code>", "message": "<safe text>" }`. \
                    Upstream Strava bodies are never forwarded. Planned codes the client should \
                    handle: `401` session, `429` Strava rate limit (once `/sync` exists), \
                    `422` incomplete Strava payload, `502`/`503` Strava down.

                    ## What is not in this document yet

                    Insight is phase 5.
                    """))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local (`mvn spring-boot:run`)")
            ))
            .tags(List.of(
                new Tag()
                    .name("Auth")
                    .description("Strava authorization-code OAuth. Issues the Lapwise session JWT. Public; no Bearer header."),
                new Tag()
                    .name("Sync")
                    .description("On-demand pull of swim activities from Strava. Requires Bearer JWT. No insight."),
                new Tag()
                    .name("Swim Activities")
                    .description("List and detail of swims already stored for the JWT user. Does not call Strava.")
            ))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("""
                        HS256 JWT from `sessionToken` on the OAuth callback body. \
                        Same secret as `lapwise.session.secret` / `SESSION_SECRET`. \
                        Not a Strava access token.
                        """)));
    }
}
