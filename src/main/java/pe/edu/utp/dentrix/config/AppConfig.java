package pe.edu.utp.dentrix.config;

import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final String RECURSO = "/pe/edu/utp/dentrix/config.properties";
    private static AppConfig instancia;
    private final Properties props = new Properties();

    private AppConfig() throws DentrixException {
        try (InputStream in = AppConfig.class.getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new DentrixException(
                        "No se encontro " + RECURSO
                        + ". Copia config.properties.example a config.properties dentro de resources.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new DentrixException("No se pudo leer la configuracion.", e);
        }
    }

    public static AppConfig get() throws DentrixException {
        if (instancia == null) {
            instancia = new AppConfig();
        }
        return instancia;
    }

    public String get(String clave) {
        return props.getProperty(clave);
    }

    public String get(String clave, String porDefecto) {
        return props.getProperty(clave, porDefecto);
    }

    public int getInt(String clave, int porDefecto) {
        String v = props.getProperty(clave);
        return v == null ? porDefecto : Integer.parseInt(v.trim());
    }
}
