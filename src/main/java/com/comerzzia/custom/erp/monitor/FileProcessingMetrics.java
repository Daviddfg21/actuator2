package com.comerzzia.custom.erp.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class FileProcessingMetrics {

	private final MeterRegistry meterRegistry;

	// Contadores para diferentes tipos de archivos
	private ConcurrentHashMap<String, Counter> processedFilesCounter = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Counter> failedFilesCounter = new ConcurrentHashMap<>();

	// Timers para medir tiempo de procesamiento
	private ConcurrentHashMap<String, Timer> processingTimers = new ConcurrentHashMap<>();

	public FileProcessingMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	public void init() {
		// Inicializar contadores y timers para cada tipo de entidad
		String[] entityTypes = { "items", "promotions", "rates", "warehouse", "sales", "salesfiles", "saleschannels" };

		for (String entityType : entityTypes) {
			// Contador de archivos procesados
			processedFilesCounter.put(entityType, Counter.builder("file.processed").tag("type", entityType).description("Número de archivos procesados correctamente").register(meterRegistry));

			// Contador de archivos fallidos
			failedFilesCounter.put(entityType, Counter.builder("file.failed").tag("type", entityType).description("Número de archivos con errores de procesamiento").register(meterRegistry));

			// Timer para tiempo de procesamiento
			processingTimers.put(entityType, Timer.builder("file.processing.time").tag("type", entityType).description("Tiempo de procesamiento de archivos").register(meterRegistry));
		}
	}

	/**
	 * Incrementa el contador de archivos procesados correctamente
	 */
	public void incrementProcessedFiles(String entityType) {
		Counter counter = processedFilesCounter.get(entityType);
		if (counter != null) {
			counter.increment();
		}
	}

	/**
	 * Incrementa el contador de archivos fallidos
	 */
	public void incrementFailedFiles(String entityType) {
		Counter counter = failedFilesCounter.get(entityType);
		if (counter != null) {
			counter.increment();
		}
	}

	/**
	 * Registra el tiempo de procesamiento de un archivo
	 */
	public void recordProcessingTime(String entityType, long millis) {
		Timer timer = processingTimers.get(entityType);
		if (timer != null) {
			timer.record(millis, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Crea y devuelve un timer que se puede usar con un bloque try-with-resources
	 */
	public Timer.Sample startTimer() {
		return Timer.start(meterRegistry);
	}

	/**
	 * Detiene el timer y registra el tiempo
	 */
	public void stopTimer(Timer.Sample sample, String entityType) {
		Timer timer = processingTimers.get(entityType);
		if (timer != null && sample != null) {
			sample.stop(timer);
		}
	}
}