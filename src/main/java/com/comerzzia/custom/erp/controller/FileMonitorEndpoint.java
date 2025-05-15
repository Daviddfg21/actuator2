package com.comerzzia.custom.erp.controller;

import com.comerzzia.custom.erp.monitor.Entity;
import com.comerzzia.custom.erp.monitor.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint personalizado para mostrar información detallada del monitor de archivos
 */
@Component
@RestControllerEndpoint(id = "filemonitor")
public class FileMonitorEndpoint {

	@Autowired
	private Monitor monitor;

	@GetMapping
	public ResponseEntity<Map<String, Object>> getMonitorStatus() {
		Map<String, Object> status = new HashMap<>();
		status.put("scanInterval", monitor.getScanInterval());

		List<Map<String, Object>> entitiesInfo = new ArrayList<>();

		for (Entity entity : monitor.getEntities()) {
			Map<String, Object> entityInfo = new HashMap<>();
			entityInfo.put("type", entity.getType());
			entityInfo.put("priority", entity.getPriority());
			entityInfo.put("threads", entity.getThreads());
			entityInfo.put("maxFilesPerBatch", entity.getMaxFilesPerBatch());

			// Información de las carpetas
			Map<String, Object> folders = new HashMap<>();
			String baseFolder = entity.getFolder();

			Path inputPath = Paths.get(baseFolder, "input");
			Path processedPath = Paths.get(baseFolder, "processed");
			Path failedPath = Paths.get(baseFolder, "failed");

			File inputDir = inputPath.toFile();
			File processedDir = processedPath.toFile();
			File failedDir = failedPath.toFile();

			// Información de la carpeta de entrada
			Map<String, Object> inputInfo = new HashMap<>();
			inputInfo.put("exists", inputDir.exists());
			inputInfo.put("path", inputPath.toString());
			if (inputDir.exists()) {
				File[] files = inputDir.listFiles(file -> !file.isDirectory());
				inputInfo.put("pendingFiles", files != null ? files.length : 0);
			}
			folders.put("input", inputInfo);

			// Información de la carpeta de procesados
			Map<String, Object> processedInfo = new HashMap<>();
			processedInfo.put("exists", processedDir.exists());
			processedInfo.put("path", processedPath.toString());
			if (processedDir.exists()) {
				File[] files = processedDir.listFiles(file -> !file.isDirectory());
				processedInfo.put("processedFiles", files != null ? files.length : 0);
			}
			folders.put("processed", processedInfo);

			// Información de la carpeta de fallidos
			Map<String, Object> failedInfo = new HashMap<>();
			failedInfo.put("exists", failedDir.exists());
			failedInfo.put("path", failedPath.toString());
			if (failedDir.exists()) {
				File[] files = failedDir.listFiles(file -> !file.isDirectory());
				failedInfo.put("failedFiles", files != null ? files.length : 0);
			}
			folders.put("failed", failedInfo);

			entityInfo.put("folders", folders);
			entitiesInfo.add(entityInfo);
		}

		status.put("entities", entitiesInfo);
		return ResponseEntity.ok(status);
	}
}