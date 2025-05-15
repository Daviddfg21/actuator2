package com.comerzzia.custom.erp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

	@Value("${management.server.port:8081}")
	private String managementPort;

	@Value("${management.endpoints.web.base-path:/manage}")
	private String basePath;

	@Value("${server.port:8080}")
	private String serverPort;

	@Value("${spring.application.name:Comerzzia ERP Custom}")
	private String appName;

	@GetMapping("/")
	@ResponseBody
	public String home() {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"es\">\n");
		html.append("<head>\n");
		html.append("    <meta charset=\"UTF-8\">\n");
		html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("    <title>").append(appName).append(" - Monitor</title>\n");
		html.append("    <style>\n");
		html.append("        body { font-family: Arial, sans-serif; line-height: 1.6; padding: 20px; max-width: 1000px; margin: 0 auto; }\n");
		html.append("        h1 { color: #2c3e50; border-bottom: 1px solid #eee; padding-bottom: 10px; }\n");
		html.append("        h2 { color: #3498db; margin-top: 30px; }\n");
		html.append("        .card { background: #f9f9f9; border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; border-radius: 4px; }\n");
		html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
		html.append("        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #ddd; }\n");
		html.append("        th { background-color: #f2f2f2; }\n");
		html.append("        tr:hover { background-color: #f5f5f5; }\n");
		html.append("        .badge { display: inline-block; padding: 3px 7px; font-size: 12px; font-weight: bold; border-radius: 4px; }\n");
		html.append("        .badge-info { background-color: #3498db; color: white; }\n");
		html.append("        .badge-warning { background-color: #f39c12; color: white; }\n");
		html.append("        .footer { margin-top: 40px; font-size: 12px; color: #7f8c8d; text-align: center; }\n");
		html.append("    </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("    <h1>").append(appName).append(" - Panel de monitoreo</h1>\n");
		html.append("    <div class=\"card\">\n");
		html.append("        <p>Fecha y hora actual: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("</p>\n");
		html.append("    </div>\n");

		html.append("    <h2>Endpoints de Spring Actuator</h2>\n");
		html.append("    <p>Los siguientes endpoints están disponibles en el puerto ").append(managementPort).append(":</p>\n");
		html.append("    <table>\n");
		html.append("        <tr><th>Endpoint</th><th>Descripción</th><th>URL</th></tr>\n");

		// Añadir endpoints comunes
		addEndpoint(html, "health", "Estado de salud de la aplicación", "http://localhost:" + managementPort + basePath + "/health");
		addEndpoint(html, "info", "Información general de la aplicación", "http://localhost:" + managementPort + basePath + "/info");
		addEndpoint(html, "metrics", "Métricas de la aplicación", "http://localhost:" + managementPort + basePath + "/metrics");
		addEndpoint(html, "prometheus", "Métricas en formato Prometheus", "http://localhost:" + managementPort + basePath + "/prometheus");
		addEndpoint(html, "loggers", "Configuración de loggers", "http://localhost:" + managementPort + basePath + "/loggers");
		addEndpoint(html, "env", "Variables de entorno", "http://localhost:" + managementPort + basePath + "/env");
		addEndpoint(html, "beans", "Beans de Spring", "http://localhost:" + managementPort + basePath + "/beans");
		addEndpoint(html, "threaddump", "Dump de threads", "http://localhost:" + managementPort + basePath + "/threaddump");
		addEndpoint(html, "heapdump", "Dump del heap (descarga un archivo)", "http://localhost:" + managementPort + basePath + "/heapdump");
		addEndpoint(html, "filemonitor", "Estado del monitor de archivos", "http://localhost:" + managementPort + basePath + "/filemonitor");

		html.append("    </table>\n");

		html.append("    <h2>Ejemplos de uso</h2>\n");
		html.append("    <div class=\"card\">\n");
		html.append("        <h3>Verificar estado</h3>\n");
		html.append("        <p>Para verificar el estado de salud detallado:</p>\n");
		html.append("        <pre>curl -u admin:actmonitor http://localhost:").append(managementPort).append(basePath).append("/health</pre>\n");
		html.append("    </div>\n");

		html.append("    <div class=\"card\">\n");
		html.append("        <h3>Ver métricas disponibles</h3>\n");
		html.append("        <p>Para listar todas las métricas disponibles:</p>\n");
		html.append("        <pre>curl -u admin:actmonitor http://localhost:").append(managementPort).append(basePath).append("/metrics</pre>\n");
		html.append("        <p>Para ver una métrica específica (por ejemplo, archivos procesados):</p>\n");
		html.append("        <pre>curl -u admin:actmonitor http://localhost:").append(managementPort).append(basePath).append("/metrics/file.processed</pre>\n");
		html.append("    </div>\n");

		html.append("    <div class=\"card\">\n");
		html.append("        <h3>Ver información del monitor de archivos</h3>\n");
		html.append("        <p>Para ver información detallada del monitor:</p>\n");
		html.append("        <pre>curl -u admin:actmonitor http://localhost:").append(managementPort).append(basePath).append("/filemonitor</pre>\n");
		html.append("    </div>\n");

		html.append("    <div class=\"footer\">\n");
		html.append("        <p>").append(appName).append(" &copy; ").append(LocalDateTime.now().getYear()).append("</p>\n");
		html.append("    </div>\n");
		html.append("</body>\n");
		html.append("</html>");

		return html.toString();
	}

	private void addEndpoint(StringBuilder html, String name, String description, String url) {
		html.append("        <tr>\n");
		html.append("            <td><span class=\"badge badge-info\">").append(name).append("</span></td>\n");
		html.append("            <td>").append(description).append("</td>\n");
		html.append("            <td><a href=\"").append(url).append("\" target=\"_blank\">").append(url).append("</a></td>\n");
		html.append("        </tr>\n");
	}

	@GetMapping("/status")
	@ResponseBody
	public Map<String, Object> status() {
		Map<String, Object> status = new HashMap<>();
		status.put("status", "UP");
		status.put("timestamp", LocalDateTime.now().toString());
		status.put("application", appName);

		return status;
	}
}