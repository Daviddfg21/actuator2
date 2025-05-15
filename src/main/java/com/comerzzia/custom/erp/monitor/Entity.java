package com.comerzzia.custom.erp.monitor;

import lombok.Data;

/**
 * Configuración super simplificada de una entidad que se procesará. Solo contiene las propiedades esenciales, sin
 * dependencias ni otras complicaciones.
 */
@Data // Lombok proporcionará getters, setters y otros métodos
public class Entity {

	// Tipo de entidad (items, promotions, etc.)
	private String type;

	// Prioridad de procesamiento (mayor número = mayor prioridad)
	private int priority = 0;

	// Número de hilos dedicados a esta entidad
	private int threads = 1;

	// Número máximo de archivos a procesar por lote para esta entidad
	private int maxFilesPerBatch = 10;

	// Carpeta de monitoreo
	private String folder;

	// Bean de servicio para procesamiento
	private String serviceBean;

	// Método del servicio para procesar
	private String processMethod;

	// Clase DTO para deserialización
	private String dtoClass;

	// Constructor sin argumentos requerido para deserialización
	public Entity() {
	}
}