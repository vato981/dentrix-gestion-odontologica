package pe.edu.utp.dentrix.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FirmaSimulada {

    private FirmaSimulada() {
    }

    public static String hash(String... partes) {
        String texto = String.join("|", partes == null ? new String[0] : partes);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte valor : bytes) {
                hex.append(Character.forDigit((valor >> 4) & 0xF, 16));
                hex.append(Character.forDigit(valor & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
