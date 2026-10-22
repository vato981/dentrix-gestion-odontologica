package pe.edu.utp.dentrix.dto;

public record LineaBoleta(Long presupuestoItemId, Integer tratamientoId, Integer piezaFdi, Integer cantidad) {

    public static LineaBoleta deItem(Long presupuestoItemId) {
        return new LineaBoleta(presupuestoItemId, null, null, 1);
    }

    public static LineaBoleta directa(Integer tratamientoId, Integer piezaFdi, Integer cantidad) {
        return new LineaBoleta(null, tratamientoId, piezaFdi, cantidad);
    }

    public boolean esItem() {
        return presupuestoItemId != null;
    }
}
