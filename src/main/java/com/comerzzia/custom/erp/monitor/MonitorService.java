package com.comerzzia.custom.erp.monitor;

import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 * Servicio principal que monitorea carpetas y procesa archivos.
 */
@Service
@Slf4j
public class MonitorService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Monitor monitor;

	@Autowired
	private FileProcessingMetrics metrics;

	// Lista de entidades monitorizadas
	private List<EntityService> monitoredEntities = new ArrayList<>();

	// Control de entidades en procesamiento
	private ConcurrentHashMap<String, Boolean> processingEntities = new ConcurrentHashMap<>();

	// Caché de contextos JAXB para una mejor performance
	private Map<String, JAXBContext> jaxbContextCache = new HashMap<>();

	@PostConstruct
	public void init() {
		try {
			log.info("Inicializando servicio de monitoreo...");

			// Obtener la configuración de manera segura
			List<Entity> configEntities = Collections.emptyList();
			try {
				configEntities = monitor.getEntities();
			}
			catch (Exception e) {
				log.error("Error al obtener entidades del monitor: {}", e.getMessage());
				// Continuamos con una lista vacía
			}

			if (configEntities == null || configEntities.isEmpty()) {
				log.warn("No hay entidades configuradas para monitorear. El servicio de monitoreo no procesará archivos.");
				return;
			}

			// Convertir cada Entity a EntityService
			for (Entity config : configEntities) {
				try {
					EntityService entity = new EntityService(config);
					monitoredEntities.add(entity);
				}
				catch (Exception e) {
					log.error("Error al inicializar entidad {}: {}", config.getType(), e.getMessage(), e);
				}
			}

			// Ordenar por prioridad (mayor primero)
			monitoredEntities.sort(Comparator.comparing(EntityService::getPriority).reversed());

			log.info("Servicio de monitoreo inicializado con {} entidades", monitoredEntities.size());
			for (EntityService entity : monitoredEntities) {
				log.info("  - {}", entity);
			}
		}
		catch (Exception e) {
			log.error("Error al inicializar el servicio de monitoreo: {}", e.getMessage(), e);
		}
	}

	@PreDestroy
	public void shutdown() {
		log.info("Cerrando servicio de monitoreo...");
		for (EntityService entity : monitoredEntities) {
			try {
				entity.shutdown();
			}
			catch (Exception e) {
				log.error("Error al cerrar entidad {}: {}", entity.getType(), e.getMessage());
			}
		}
	}

	/**
	 * Método principal que se ejecuta periódicamente para escanear y procesar archivos
	 */
	@Scheduled(fixedDelayString = "${monitor.scanInterval:10000}")
	public void scanAndProcess() {
		if (monitoredEntities.isEmpty()) {
			return; // No hay entidades configuradas, no hacemos nada
		}

		log.debug("Iniciando escaneo de carpetas...");

		// Procesar entidades en orden de prioridad
		for (EntityService entity : monitoredEntities) {
			try {
				processEntity(entity);
			}
			catch (Exception e) {
				log.error("Error procesando entidad {}: {}", entity.getType(), e.getMessage());
			}
		}
	}

	/**
	 * Procesa los archivos de una entidad
	 */
	private void processEntity(EntityService entity) {
		if (entity == null) {
			return;
		}

		String entityType = entity.getType();

		// Verificar si la entidad ya está en procesamiento
		if (processingEntities.containsKey(entityType)) {
			log.debug("La entidad {} ya está en procesamiento", entityType);
			return;
		}

		// Obtener archivos disponibles de forma segura
		File[] files = null;
		try {
			files = entity.getAvailableFiles();
		}
		catch (Exception e) {
			log.error("Error al obtener archivos para entidad {}: {}", entityType, e.getMessage());
			return;
		}

		if (files == null || files.length == 0) {
			log.debug("No hay archivos para procesar en la entidad {}", entityType);
			return;
		}

		// Limitar cantidad de archivos por lote según configuración
		int maxFiles = Math.min(files.length, entity.getConfig().getMaxFilesPerBatch());

		log.info("Procesando {} archivos para la entidad {}", maxFiles, entityType);

		// Marcar entidad como en procesamiento
		processingEntities.put(entityType, true);

		try {
			// Procesar archivos con el executor de la entidad
			CountDownLatch latch = new CountDownLatch(maxFiles);

			for (int i = 0; i < maxFiles; i++) {
				final File file = files[i];
				final EntityService finalEntity = entity;

				entity.getExecutor().submit(() -> {
					// Iniciar timer para medir tiempo de procesamiento
					Timer.Sample sample = metrics.startTimer();

					try {
						boolean success = processFile(finalEntity, file);

						// Registrar métricas de éxito o fallo
						if (success) {
							metrics.incrementProcessedFiles(finalEntity.getType());
						}
						else {
							metrics.incrementFailedFiles(finalEntity.getType());
						}
					}
					catch (Exception e) {
						log.error("Error al procesar archivo {}: {}", file.getName(), e.getMessage(), e);
						metrics.incrementFailedFiles(finalEntity.getType());
					}
					finally {
						// Detener timer y registrar tiempo
						metrics.stopTimer(sample, finalEntity.getType());
						latch.countDown();
					}
				});
			}

			// Esperar a que todos los archivos terminen, con timeout
			boolean completed = latch.await(5, TimeUnit.MINUTES);
			if (!completed) {
				log.warn("Timeout esperando que se procesen los archivos para entidad {}", entityType);
			}
		}
		catch (Exception e) {
			log.error("Error al procesar entidad {}: {}", entityType, e.getMessage(), e);
		}
		finally {
			// Eliminar entidad del mapa de procesamiento
			processingEntities.remove(entityType);
		}
	}

	/**
	 * Procesa un archivo individual
	 */
	private boolean processFile(EntityService entity, File file) {
		try {
			// Verificaciones de seguridad
			if (entity == null || file == null || !file.exists()) {
				log.error("Entidad o archivo inválido para procesar");
				return false;
			}

			// Deserializar el archivo XML
			Entity config = entity.getConfig();
			if (config == null || config.getDtoClass() == null) {
				log.error("Configuración de entidad inválida: {}", entity);
				return handleFailure(entity, file);
			}

			Class<?> dtoClass;
			try {
				dtoClass = Class.forName(config.getDtoClass());
			}
			catch (ClassNotFoundException e) {
				log.error("No se encontró la clase DTO: {}", config.getDtoClass());
				return handleFailure(entity, file);
			}

			// Obtener el objeto DTO
			Object dto = parseXml(file.toPath(), dtoClass);
			if (dto == null) {
				log.error("Error al deserializar el archivo: {}", file);
				return handleFailure(entity, file);
			}

			// Obtener el servicio correspondiente
			Object service = context.getBean(config.getServiceBean());
			if (service == null) {
				log.error("Servicio no encontrado: {}", config.getServiceBean());
				return handleFailure(entity, file);
			}

			// Invocar el método de procesamiento
			java.lang.reflect.Method method = service.getClass().getMethod(config.getProcessMethod(), dtoClass);
			method.invoke(service, dto);

			// Mover archivo a carpeta de procesados
			moveToProcessed(entity, file);

			log.info("Archivo procesado correctamente: {}", file.getName());
			return true;

		}
		catch (Exception e) {
			log.error("Error al procesar archivo {}: {}", file.getName(), e.getMessage(), e);
			return handleFailure(entity, file);
		}
	}

	/**
	 * Parsea un archivo XML a un objeto DTO
	 */
	private <T> T parseXml(Path filePath, Class<T> dtoClass) {
		try (InputStream is = Files.newInputStream(filePath)) {
			// Obtener o crear contexto JAXB
			JAXBContext context = jaxbContextCache.computeIfAbsent(dtoClass.getName(), className -> {
				try {
					return JAXBContext.newInstance(dtoClass);
				}
				catch (Exception e) {
					log.error("Error al crear contexto JAXB para {}: {}", className, e.getMessage());
					return null;
				}
			});

			if (context == null) {
				return null;
			}

			Unmarshaller unmarshaller = context.createUnmarshaller();
			@SuppressWarnings("unchecked")
			T result = (T) unmarshaller.unmarshal(is);

			return result;
		}
		catch (Exception e) {
			log.error("Error al parsear XML {}: {}", filePath, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Maneja el fallo de procesamiento
	 */
	private boolean handleFailure(EntityService entity, File file) {
		try {
			moveToFailed(entity, file);
			return false;
		}
		catch (Exception e) {
			log.error("Error al mover archivo a carpeta 'failed': {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Mueve un archivo a la carpeta de procesados
	 */
	private void moveToProcessed(EntityService entity, File file) throws Exception {
		String timestamp = LocalDateTime.now().format(DATE_FORMAT);
		Path destination = entity.getProcessedFolder().resolve(timestamp + "_" + file.getName());
		Files.move(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Mueve un archivo a la carpeta de fallidos
	 */
	private void moveToFailed(EntityService entity, File file) throws Exception {
		String timestamp = LocalDateTime.now().format(DATE_FORMAT);
		Path destination = entity.getFailedFolder().resolve(timestamp + "_" + file.getName());
		Files.move(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
	}
}