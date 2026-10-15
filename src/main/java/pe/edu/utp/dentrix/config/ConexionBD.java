package pe.edu.utp.dentrix.config;

import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionBD {

    private static final String DRIVER = "org.postgresql.Driver";

    private ConexionBD() {
    }

    public static Connection abrir() throws DentrixException {
        AppConfig cfg = AppConfig.get();
        String url = "jdbc:postgresql://" + cfg.get("db.host", "localhost")
                + ":" + cfg.get("db.puerto", "5432")
                + "/" + cfg.get("db.nombre", "dentrix")
                + "?currentSchema=" + cfg.get("db.esquema", "dentrix");
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new DentrixException("No se cargo el driver PostgreSQL.", e);
        }
        try {
            return DriverManager.getConnection(url, cfg.get("db.usuario"), cfg.get("db.clave"));
        } catch (SQLException e) {
            throw new DentrixException("No se pudo conectar a PostgreSQL: " + e.getMessage(), e);
        }
    }
}
