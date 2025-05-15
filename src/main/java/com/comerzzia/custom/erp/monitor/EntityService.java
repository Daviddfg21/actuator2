package com.comerzzia.custom.erp.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Versión super simplificada de la entidad. Representa una entidad de negocio con su propio pool de hilos.
 */
@Data
@Slf4j
public class EntityService {

	private Entity config;
	private Path inputFolder;
	private Path processedFolder;
	private Path failedFolder;

	// Executor específico para esta entidad
	private ExecutorService executor;

	// Estado para control de procesamiento
	private volatile boolean processing = false;

	/**
	 * Constructor que inicializa las carpetas y el executor
	 */
	public EntityService(Entity config) {
		this.config = config;

		// Inicializar carpetas
		String baseFolder = config.getFolder();

		this.inputFolder = Paths.get(baseFolder, "input");
		this.processedFolder = Paths.get(baseFolder, "processed");
		this.failedFolder = Paths.get(baseFolder, "failed");

		// Crear directorios si no existen
		createFolders();

		// Inicializar executor con el número de hilos específico para esta entidad
		this.executor = Executors.newFixedThreadPool(config.getThreads());
		log.info("Entidad {} inicializada con {} hilos", config.getType(), config.getThreads());
	}

	/**
	 * Crea las carpetas necesarias si no existen
	 */
	public void createFolders() {
		try {
			if (!Files.exists(inputFolder)) {
				Files.createDirectories(inputFolder);
				log.info("Carpeta de entrada creada: {}", inputFolder);
			}

			if (!Files.exists(processedFolder)) {
				Files.createDirectories(processedFolder);
				log.info("Carpeta de procesados creada: {}", processedFolder);
			}

			if (!Files.exists(failedFolder)) {
				Files.createDirectories(failedFolder);
				log.info("Carpeta de fallidos creada: {}", failedFolder);
			}
		}
		catch (Exception e) {
			log.error("Error al crear directorios para {}: {}", config.getType(), e.getMessage());
		}
	}

	/**
	 * Obtiene archivos disponibles para procesar
	 */
	public File[] getAvailableFiles() {
		File inputDir = inputFolder.toFile();
		if (!inputDir.exists() || !inputDir.isDirectory()) {
			return new File[0];
		}

		return inputDir.listFiles(file -> !file.isDirectory());
	}

	/**
	 * Obtiene el tipo de entidad
	 */
	public String getType() {
		return config.getType();
	}

	/**
	 * Obtiene la prioridad
	 */
	public int getPriority() {
		return config.getPriority();
	}

	/**
	 * Obtiene el número de hilos
	 */
	public int getThreads() {
		return config.getThreads();
	}

	/**
	 * Apaga el executor de la entidad
	 */
	public void shutdown() {
		if (executor != null) {
			executor.shutdown();
			log.info("Executor de la entidad {} apagado", getType());
		}
	}

	@Override
	public String toString() {
		return "Entity{" + "type='" + config.getType() + '\'' + ", priority=" + config.getPriority() + ", threads=" + config.getThreads() + '}';
	}
}