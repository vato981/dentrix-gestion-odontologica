package pe.edu.utp.dentrix.excepcion;

public class DentrixException extends Exception {

    public DentrixException(String mensaje) {
        super(mensaje);
    }

    public DentrixException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
