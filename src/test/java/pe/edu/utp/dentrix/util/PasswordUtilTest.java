package pe.edu.utp.dentrix.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashNoGuardaClaveEnTextoPlano() {
        String hash = PasswordUtil.hash("Admin2026*");
        assertNotEquals("Admin2026*", hash);
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    void verificaClaveCorrectaEIncorrecta() {
        String hash = PasswordUtil.hash("Admin2026*");
        assertTrue(PasswordUtil.verificar("Admin2026*", hash));
        assertFalse(PasswordUtil.verificar("clave-mala", hash));
    }
}
