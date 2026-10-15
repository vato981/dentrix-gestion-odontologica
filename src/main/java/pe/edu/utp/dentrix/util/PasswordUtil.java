package pe.edu.utp.dentrix.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private static final int COSTE = 10;

    private PasswordUtil() {
    }

    public static String hash(String clave) {
        return BCrypt.hashpw(clave, BCrypt.gensalt(COSTE));
    }

    public static boolean verificar(String clave, String hash) {
        if (clave == null || hash == null) {
            return false;
        }
        return BCrypt.checkpw(clave, hash);
    }
}
