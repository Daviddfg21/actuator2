package com.comerzzia.custom.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ImportResource;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = { ErrorMvcAutoConfiguration.class })
@ImportResource({ "classpath*:comerzzia-*context.xml" })
@Slf4j
public class CustomApplication {

	public static void main(String[] args) {
		try {
			SpringApplication app = new SpringApplication(CustomApplication.class);
			// Asegurar que se habilita el override de beans
			java.util.Properties props = new java.util.Properties();
			props.setProperty("spring.main.allow-bean-definition-overriding", "true");
			app.setDefaultProperties(props);
			app.run(args);
			log.info("Aplicación iniciada correctamente");
		}
		catch (Exception e) {
			log.error("Error al iniciar la aplicación: {}", e.getMessage(), e);
		}
	}
}