package com.comerzzia.custom.erp.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

/**
 * Configuración principal del monitor de archivos.
 */
@Component
@ConfigurationProperties(prefix = "monitor")
@Slf4j
public class Monitor {

	private int scanInterval = 10000;

	// Cambiar a tipo específico (en lugar de List<Object>)
	private List<Entity> entities = new ArrayList<>();

	// Ya no necesitamos esta lista separada
	// private List<Entity> configuredEntities = new ArrayList<>();

	public int getScanInterval() {
		return scanInterval;
	}

	public void setScanInterval(int scanInterval) {
		this.scanInterval = scanInterval;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	@PostConstruct
	public void init() {
		log.info("Inicializando monitor con configuración...");

		if (entities == null || entities.isEmpty()) {
			log.warn("No se cargaron entidades del monitor. Verifica la configuración.");
			return;
		}

		log.info("Monitor configurado con {} entidades y scanInterval={}", entities.size(), scanInterval);

		for (int i = 0; i < entities.size(); i++) {
			Entity entity = entities.get(i);
			log.info("Entidad {}: tipo={}, prioridad={}, carpeta={}", i + 1, entity.getType(), entity.getPriority(), entity.getFolder());
		}
	}
}