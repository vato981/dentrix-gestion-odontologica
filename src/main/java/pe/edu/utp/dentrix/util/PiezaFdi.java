package pe.edu.utp.dentrix.util;

public final class PiezaFdi {

    private PiezaFdi() {
    }

    public static boolean esValida(Integer pieza) {
        if (pieza == null) {
            return true;
        }
        if (pieza >= 11 && pieza <= 48) {
            int diente = pieza % 10;
            return diente >= 1 && diente <= 8;
        }
        if (pieza >= 51 && pieza <= 85) {
            int diente = pieza % 10;
            return diente >= 1 && diente <= 5;
        }
        return false;
    }
}
