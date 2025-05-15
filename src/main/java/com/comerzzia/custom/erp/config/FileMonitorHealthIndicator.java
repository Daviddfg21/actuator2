package com.comerzzia.custom.erp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.comerzzia.custom.erp.monitor.Entity;
import com.comerzzia.custom.erp.monitor.Monitor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileMonitorHealthIndicator implements HealthIndicator {

	@Autowired
	private Monitor monitor;

	@Override
	public Health health() {
		Health.Builder builder = Health.up();

		int totalPendingFiles = 0;
		int entitiesWithIssues = 0;

		for (Entity entity : monitor.getEntities()) {
			String baseFolder = entity.getFolder();

			// Verificar que las carpetas existan
			Path inputPath = Paths.get(baseFolder, "input");
			File inputDir = inputPath.toFile();

			if (!inputDir.exists() || !inputDir.isDirectory()) {
				entitiesWithIssues++;
				builder.withDetail("entity." + entity.getType() + ".error", "Input directory does not exist: " + inputPath);
				continue;
			}

			// Contar archivos pendientes
			File[] pendingFiles = inputDir.listFiles(file -> !file.isDirectory());
			int pendingCount = pendingFiles != null ? pendingFiles.length : 0;
			totalPendingFiles += pendingCount;

			builder.withDetail("entity." + entity.getType() + ".pendingFiles", pendingCount);

			// Verificar carpeta de errores
			Path failedPath = Paths.get(baseFolder, "failed");
			File failedDir = failedPath.toFile();

			if (failedDir.exists() && failedDir.isDirectory()) {
				File[] failedFiles = failedDir.listFiles(file -> !file.isDirectory());
				int failedCount = failedFiles != null ? failedFiles.length : 0;

				builder.withDetail("entity." + entity.getType() + ".failedFiles", failedCount);

				// Si hay muchos archivos fallidos, mostramos una advertencia
				if (failedCount > 100) {
					entitiesWithIssues++;
					builder.withDetail("entity." + entity.getType() + ".warning", "Many failed files: " + failedCount);
				}
			}
		}

		builder.withDetail("totalPendingFiles", totalPendingFiles);
		builder.withDetail("entitiesWithIssues", entitiesWithIssues);

		// Si hay demasiados problemas, cambiar el estado a DOWN
		if (entitiesWithIssues > 0) {
			return builder.status("WARNING").build();
		}

		return builder.build();
	}
}