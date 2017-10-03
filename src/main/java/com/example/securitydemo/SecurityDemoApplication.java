package com.example.securitydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class SecurityDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityDemoApplication.class, args);
    }
}

@Configuration
class WebConfiguration {

    Mono<ServerResponse> message(ServerRequest serverRequest) {
        Mono<String> principalPublisher = serverRequest.principal().map(p -> "Hello, " + p.getName() + "!");
        return ServerResponse.ok().body(principalPublisher, String.class);
    }

    Mono<ServerResponse> username(ServerRequest serverRequest) {
        Mono<UserDetails> detailsMono = serverRequest
                .principal()
                .map(p -> UserDetails.class.cast(Authentication.class.cast(p).getPrincipal()));
        return ServerResponse.ok().body(detailsMono, UserDetails.class);
    }

    @Bean
    RouterFunction<?> routes() {
        return route(GET("/message"), this::message)
                .andRoute(GET("/users/{username}"), this::username);
    }
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    UserDetailsRepository userDetailsRepository() {
        UserDetails jlong = User.withUsername("jlong").roles("USER").password("password").build();
        UserDetails rwinch = User.withUsername("rwinch").roles("USER", "ADMIN").password("password").build();
        return new MapUserDetailsRepository(jlong, rwinch);
    }

    @Bean
    SecurityWebFilterChain security(HttpSecurity httpSecurity) {
        return httpSecurity
                .authorizeExchange()
                .pathMatchers("/users/{username}")
                .access((mono, context) -> mono
                        .map(auth -> auth.getName().equals(context.getVariables().get("username")))
                        .map(AuthorizationDecision::new))
                .anyExchange().authenticated()
                .and()
                .build();
    }
}