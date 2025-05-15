package com.comerzzia.custom.erp.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomInfoContributor implements InfoContributor {

	@Override
	public void contribute(Info.Builder builder) {
		Map<String, Object> details = new HashMap<>();

		// Información sobre el monitor de archivos
		Map<String, Object> monitorDetails = new HashMap<>();
		monitorDetails.put("scan-interval", "10 segundos");
		monitorDetails.put("entities", "items,promotions,rates,warehouse,sales,salesfiles,saleschannels");
		details.put("monitor", monitorDetails);

		// Información sobre el sistema
		Map<String, Object> systemDetails = new HashMap<>();
		systemDetails.put("jvm-memory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
		systemDetails.put("processors", Runtime.getRuntime().availableProcessors());
		details.put("system", systemDetails);

		builder.withDetail("custom", details);
	}
}