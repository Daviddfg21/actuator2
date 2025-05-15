package com.comerzzia.custom.erp.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		// Configuración para los endpoints de actuator
		http.requestMatcher(EndpointRequest.toAnyEndpoint()).authorizeRequests().requestMatchers(EndpointRequest.to("health", "info")).permitAll().anyRequest().hasRole("ACTUATOR_ADMIN").and()
		        .httpBasic();

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
		// Configuración general de la aplicación
		http.authorizeRequests().anyRequest().permitAll().and().csrf().disable();

		return http.build();
	}
}