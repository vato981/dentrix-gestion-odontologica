package pe.edu.utp.dentrix.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.sql.SQLException;

public final class ConexionBD {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final int MAXIMO_POOL = 5;

    private static HikariDataSource pool;

    private ConexionBD() {
    }

    public static Connection abrir() throws DentrixException {
        try {
            return pool().getConnection();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo conectar a PostgreSQL: " + e.getMessage(), e);
        }
    }

    public static synchronized void cerrar() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
        pool = null;
    }

    private static synchronized HikariDataSource pool() throws DentrixException {
        if (pool == null || pool.isClosed()) {
            AppConfig cfg = AppConfig.get();
            String url = "jdbc:postgresql://" + cfg.get("db.host", "localhost")
                    + ":" + cfg.get("db.puerto", "5432")
                    + "/" + cfg.get("db.nombre", "dentrix")
                    + "?currentSchema=" + cfg.get("db.esquema", "dentrix");
            HikariConfig configuracion = new HikariConfig();
            configuracion.setPoolName("dentrix-pool");
            configuracion.setDriverClassName(DRIVER);
            configuracion.setJdbcUrl(url);
            configuracion.setUsername(cfg.get("db.usuario"));
            configuracion.setPassword(cfg.get("db.clave"));
            configuracion.setMaximumPoolSize(MAXIMO_POOL);
            configuracion.setMinimumIdle(1);
            configuracion.setConnectionTimeout(10_000);
            try {
                pool = new HikariDataSource(configuracion);
            } catch (RuntimeException e) {
                throw new DentrixException("No se pudo conectar a PostgreSQL: " + causa(e), e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrar, "dentrix-pool-cierre"));
        }
        return pool;
    }

    private static String causa(RuntimeException e) {
        Throwable raiz = e;
        while (raiz.getCause() != null) {
            raiz = raiz.getCause();
        }
        return raiz.getMessage() == null ? e.getMessage() : raiz.getMessage();
    }
}
