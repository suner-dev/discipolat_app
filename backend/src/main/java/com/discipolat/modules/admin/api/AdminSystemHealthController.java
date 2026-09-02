package com.discipolat.modules.admin.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/system-health")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class AdminSystemHealthController {

    private final JdbcTemplate jdbc;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    public AdminSystemHealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("jvm", getJvmInfo());
        response.put("database", getDatabaseInfo());
        response.put("uptime", getUptime());
        response.put("thread", getThreadInfo());
        response.put("processors", Runtime.getRuntime().availableProcessors());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> getJvmInfo() {
        Map<String, Object> jvm = new LinkedHashMap<>();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

        jvm.put("heapUsed", formatBytes(heap.getUsed()));
        jvm.put("heapMax", formatBytes(heap.getMax()));
        jvm.put("heapUsedPercent", heap.getMax() > 0 ? Math.round((double) heap.getUsed() / heap.getMax() * 100) : 0);
        jvm.put("nonHeapUsed", formatBytes(nonHeap.getUsed()));
        jvm.put("heapUsedBytes", heap.getUsed());
        jvm.put("heapMaxBytes", heap.getMax());

        Runtime runtime = Runtime.getRuntime();
        jvm.put("totalMemory", formatBytes(runtime.totalMemory()));
        jvm.put("freeMemory", formatBytes(runtime.freeMemory()));
        jvm.put("maxMemory", formatBytes(runtime.maxMemory()));

        return jvm;
    }

    private Map<String, Object> getDatabaseInfo() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            Long userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            Long soulCount = jdbc.queryForObject("SELECT COUNT(*) FROM souls", Long.class);
            Long familyCount = jdbc.queryForObject("SELECT COUNT(*) FROM families", Long.class);
            Long auditCount = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);

            db.put("status", "UP");
            db.put("userCount", userCount);
            db.put("soulCount", soulCount);
            db.put("familyCount", familyCount);
            db.put("auditCount", auditCount);

            Long tableCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'", Long.class);
            db.put("tableCount", tableCount);
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> getUptime() {
        Map<String, Object> uptime = new LinkedHashMap<>();
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        long ms = rt.getUptime();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        uptime.put("milliseconds", ms);
        uptime.put("formatted", String.format("%dj %02dh %02dm %02ds", days, hours % 24, minutes % 60, seconds % 60));
        uptime.put("startTime", java.time.Instant.ofEpochMilli(rt.getStartTime()).toString());

        return uptime;
    }

    private Map<String, Object> getThreadInfo() {
        Map<String, Object> thread = new LinkedHashMap<>();
        thread.put("peakCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        thread.put("daemonCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        thread.put("currentCount", ManagementFactory.getThreadMXBean().getThreadCount());
        return thread;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}