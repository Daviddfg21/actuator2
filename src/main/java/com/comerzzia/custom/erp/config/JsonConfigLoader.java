package com.comerzzia.custom.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class JsonConfigLoader {

	@Bean
	public static BeanFactoryPostProcessor jsonConfigProcessor() {
		return beanFactory -> {
			try {
				// Cargar el archivo JSON
				ObjectMapper mapper = new ObjectMapper();
				ClassPathResource resource = new ClassPathResource("application.json");

				if (!resource.exists()) {
					log.error("No se encontró el archivo application.json en el classpath");
					return;
				}

				// Leer el JSON a un Map
				Map<String, Object> jsonMap;
				try (InputStream is = resource.getInputStream()) {
					jsonMap = mapper.readValue(is, Map.class);
				}

				// Aplanar la estructura del JSON para compatibilidad con Spring Boot
				Map<String, Object> flattenedMap = new HashMap<>();
				flattenMap(flattenedMap, jsonMap, null);

				log.info("Cargando configuración desde application.json con {} propiedades", flattenedMap.size());

				// Registrar como PropertySource con alta prioridad
				ConfigurableEnvironment env = beanFactory.getBean(ConfigurableEnvironment.class);
				env.getPropertySources().addFirst(new MapPropertySource("jsonConfig", flattenedMap));

			}
			catch (Exception e) {
				log.error("Error al cargar configuración desde application.json", e);
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static void flattenMap(Map<String, Object> result, Map<String, Object> sourceMap, String prefix) {
		for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
			String key = prefix != null ? prefix + "." + entry.getKey() : entry.getKey();

			if (entry.getValue() instanceof Map) {
				// Para objetos anidados, aplanar recursivamente
				flattenMap(result, (Map<String, Object>) entry.getValue(), key);
			}
			else if (entry.getValue() instanceof List) {
				// Para listas, usar formato de índice de Spring Boot: property[0]
				List<?> list = (List<?>) entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					String indexedKey = key + "[" + i + "]";
					if (list.get(i) instanceof Map) {
						flattenMap(result, (Map<String, Object>) list.get(i), indexedKey);
					}
					else {
						result.put(indexedKey, list.get(i));
					}
				}
				// También guardar la lista completa para métodos que esperan colecciones
				result.put(key, entry.getValue());
			}
			else {
				// Para valores simples
				result.put(key, entry.getValue());
			}
		}
	}
}