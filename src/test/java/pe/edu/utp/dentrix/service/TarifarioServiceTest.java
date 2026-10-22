package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.TarifarioDAO;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.TarifarioItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarifarioServiceTest {

    private final TarifarioDAO tarifarioDAO = new TarifarioDAOImpl();
    private final TarifarioService servicio = new TarifarioService(tarifarioDAO);

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM v_precio_vigente WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void eliminarBorrador(Long tarifarioId) {
        if (tarifarioId == null) {
            return;
        }
        try (Connection cn = ConexionBD.abrir()) {
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM tarifario_item WHERE tarifario_id = ?")) {
                ps.setLong(1, tarifarioId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM tarifario WHERE id = ?")) {
                ps.setLong(1, tarifarioId);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void losPreciosVigentesCubrenLosCincoTratamientosDelTarifario() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Map<Integer, BigDecimal> precios = servicio.preciosVigentes();
        assertEquals(5, precios.size(), "El tarifario vigente debe cubrir los 5 tratamientos del catalogo");
    }

    @Test
    void elTratamientoCincoTieneUnPrecioVigentePositivo() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        BigDecimal precio = servicio.precioVigente(5);
        assertNotNull(precio, "El tratamiento 5 debe tener precio en la vista de vigentes");
        assertTrue(precio.signum() > 0, "El precio vigente del tratamiento 5 debe ser mayor a cero");
    }

    @Test
    void elPrecioALaFechaDelPrimeroDeJunioNoEsNulo() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        BigDecimal precio = servicio.precioAFecha(5, LocalDate.of(2026, 6, 1));
        assertNotNull(precio, "precio_a_fecha debe resolver el precio del tratamiento 5 al 01/06/2026");
    }

    @Test
    void abreUnBorradorQueCopiaLosPreciosYPermiteFijarUnoNuevo() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Long tarifarioId = null;
        try {
            Tarifario borrador = servicio.abrirVersion(LocalDate.of(2026, 12, 1),
                    "Prueba automatizada del tarifario", 1);
            tarifarioId = borrador.getId();
            assertNotNull(tarifarioId, "El borrador debe quedar registrado con id");
            assertTrue(borrador.esBorrador(), "La version nueva debe nacer en estado BORRADOR");
            assertEquals("PEN", borrador.getMoneda(), "La moneda por defecto es PEN");

            List<TarifarioItem> copiados = servicio.items(tarifarioId);
            assertEquals(servicio.preciosVigentes().size(), copiados.size(),
                    "El borrador debe copiar todos los precios del tarifario publicado");

            BigDecimal nuevoPrecio = new BigDecimal("123.45");
            servicio.fijarPrecio(tarifarioId, 5, nuevoPrecio);

            TarifarioItem item = servicio.items(tarifarioId).stream()
                    .filter(i -> Integer.valueOf(5).equals(i.getTratamientoId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(0, nuevoPrecio.compareTo(item.getPrecio()),
                    "El precio fijado en el borrador debe quedar registrado");
            assertNotNull(item.getTratamientoNombre(),
                    "Los items deben traer el nombre del tratamiento desde el catalogo");
        } finally {
            eliminarBorrador(tarifarioId);
        }
    }

    @Test
    void rechazaUnPrecioNegativoYUnTarifarioQueNoEsBorrador() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");

        DentrixException negativo = assertThrows(DentrixException.class,
                () -> servicio.fijarPrecio(1L, 5, new BigDecimal("-1.00")));
        assertTrue(negativo.getMessage().contains("negativo"),
                "El precio negativo debe rechazarse antes de tocar la BD: " + negativo.getMessage());

        Tarifario publicado = servicio.versionPublicada().orElse(null);
        if (publicado == null) {
            return;
        }
        DentrixException noBorrador = assertThrows(DentrixException.class,
                () -> servicio.fijarPrecio(publicado.getId(), 5, new BigDecimal("99.00")));
        assertTrue(noBorrador.getMessage().contains("BORRADOR"),
                "Solo un tarifario BORRADOR admite cambios de precio: " + noBorrador.getMessage());
    }
}
