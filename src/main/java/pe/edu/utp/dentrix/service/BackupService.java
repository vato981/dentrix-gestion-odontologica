package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.config.AppConfig;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class BackupService {

    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public File generarBackup() throws DentrixException {
        AppConfig cfg = AppConfig.get();
        String base = cfg.get("db.backup.dir", "backups");
        File carpeta = new File(base);
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new DentrixException("No se pudo crear la carpeta de respaldos: " + carpeta.getAbsolutePath());
        }
        File destino = new File(carpeta, "dentrix_backup_" + LocalDateTime.now().format(MARCA) + ".sql");

        String pgDump = cfg.get("db.pgdump",
                "C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe");
        if (!new File(pgDump).isFile()) {
            throw new DentrixException("No se encontró pg_dump en: " + pgDump
                    + ". Ajusta db.pgdump en config.properties.");
        }

        ProcessBuilder pb = new ProcessBuilder(pgDump,
                "-U", cfg.get("db.usuario", "postgres"),
                "-h", cfg.get("db.host", "localhost"),
                "-p", cfg.get("db.puerto", "5432"),
                "-d", cfg.get("db.nombre", "dentrix"),
                "-n", cfg.get("db.esquema", "dentrix"),
                "-f", destino.getAbsolutePath(),
                "--no-owner", "--no-privileges");
        pb.environment().put("PGPASSWORD", cfg.get("db.clave", ""));
        pb.redirectErrorStream(true);

        StringBuilder registro = new StringBuilder();
        try {
            Process proceso = pb.start();
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    registro.append(linea).append('\n');
                }
            }
            if (!proceso.waitFor(120, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                throw new DentrixException("El respaldo tardó más de 2 minutos y fue cancelado.");
            }
            if (proceso.exitValue() != 0) {
                throw new DentrixException("pg_dump falló (código " + proceso.exitValue() + "): "
                        + registro.toString().trim());
            }
        } catch (IOException e) {
            throw new DentrixException("No se pudo ejecutar pg_dump: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DentrixException("El respaldo fue interrumpido.", e);
        }

        if (!destino.isFile() || destino.length() == 0) {
            throw new DentrixException("El archivo de respaldo quedó vacío o no se creó: "
                    + destino.getAbsolutePath());
        }
        return destino;
    }

    public long pesoEnBytes(File archivo) {
        return archivo == null || !archivo.isFile() ? 0 : archivo.length();
    }

    public String pesoLegible(File archivo) {
        long bytes = pesoEnBytes(archivo);
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public void verificarLegible(File archivo) throws DentrixException {
        if (archivo == null || !archivo.isFile()) {
            throw new DentrixException("El archivo de respaldo no existe.");
        }
        try {
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            boolean estructura = contenido.contains("CREATE TABLE") || contenido.contains("COPY ");
            boolean esquema = contenido.contains("dentrix") || contenido.contains("SET search_path");
            if (!estructura || !esquema) {
                throw new DentrixException("El respaldo no contiene estructura SQL válida del esquema dentrix.");
            }
        } catch (IOException e) {
            throw new DentrixException("No se pudo leer el respaldo para verificarlo: " + e.getMessage(), e);
        }
    }
}
