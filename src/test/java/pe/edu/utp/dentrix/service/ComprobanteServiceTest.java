package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.ComprobanteDAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PresupuestoDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dao.impl.CierreCajaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ComprobanteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PagoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PresupuestoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TarifarioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.ComprobanteItem;
import pe.edu.utp.dentrix.model.Pago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ComprobanteServiceTest {

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final ComprobanteDAO comprobanteDAO = new ComprobanteDAOImpl();
    private final CierreCajaDAO cierreCajaDAO = new CierreCajaDAOImpl();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAOImpl();
    private final FichaClinicaDAO fichaClinicaDAO = new FichaClinicaDAOImpl();
    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAOImpl();
    private final TarifarioService tarifario = new TarifarioService(new TarifarioDAOImpl());
    private final CajaService servicio = new CajaService(pagoDAO, cierreCajaDAO, pacienteDAO,
            tratamientoDAO, fichaClinicaDAO, presupuestoDAO, tarifario, comprobanteDAO);

    private boolean hayBaseDeDatos() {
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM comprobante_item WHERE 1 = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void borrarDocumento(Long pagoId) {
        if (pagoId == null) {
            return;
        }
        try (Connection cn = ConexionBD.abrir()) {
            try (Statement st = cn.createStatement()) {
                st.execute("ALTER TABLE pago DISABLE TRIGGER tr_pago_no_delete");
            }
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM imputacion WHERE pago_id = ?")) {
                    ps.setObject(1, pagoId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM pago WHERE id = ?")) {
                    ps.setObject(1, pagoId);
                    ps.executeUpdate();
                }
            } finally {
                try (Statement st = cn.createStatement()) {
                    st.execute("ALTER TABLE pago ENABLE TRIGGER tr_pago_no_delete");
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void creaBorradorConfirmaYAnulaElComprobante() throws Exception {
        assumeTrue(hayBaseDeDatos(), "BD o config no disponibles: se omite esta prueba");
        Long borradorId = null;
        Long documentoId = null;
        try {
            Pago borrador = servicio.crearBorrador(2, null, 1, null);
            borradorId = borrador.getId();
            assertNotNull(borradorId, "El borrador debe quedar registrado con id");
            assertEquals("BORRADOR", borrador.getEstado(), "El documento nace en BORRADOR");
            assertNull(borrador.getNumero(), "El borrador no tiene numero");
            assertEquals(0, BigDecimal.ZERO.compareTo(borrador.getMonto()),
                    "El borrador nace con totales en cero");

            ComprobanteItem primero = servicio.agregarItem(borradorId, null, "Profilaxis",
                    new BigDecimal("1"), new BigDecimal("80.00"));
            ComprobanteItem segundo = servicio.agregarItem(borradorId, null, "Endodoncia",
                    new BigDecimal("1"), new BigDecimal("120.00"));
            assertEquals(0, new BigDecimal("80.00").compareTo(primero.getSubtotal()),
                    "El trigger calcula el subtotal del primer item");
            assertEquals(0, new BigDecimal("120.00").compareTo(segundo.getSubtotal()),
                    "El trigger calcula el subtotal del segundo item");
            assertEquals(2, servicio.itemsComprobante(borradorId).size(),
                    "El comprobante debe tener dos items");

            Pago conItems = pagoDAO.buscarPorId(borradorId).orElseThrow();
            assertEquals(0, new BigDecimal("200.00").compareTo(conItems.getMonto()),
                    "El total del borrador es 80 + 120");
            assertEquals(0, new BigDecimal("169.49").compareTo(conItems.getSubtotal()),
                    "El subtotal sin IGV es 169.49");
            assertEquals(0, new BigDecimal("30.51").compareTo(conItems.getIgv()),
                    "El IGV del 18% es 30.51");

            Pago emitido = servicio.confirmar(borradorId, "EFECTIVO", null);
            documentoId = emitido.getId();
            borradorId = null;
            assertNotNull(emitido.getNumero(), "Al confirmar se asigna numero");
            assertTrue(emitido.getNumero().startsWith("B001-"),
                    "El numero sigue la serie B001: " + emitido.getNumero());
            assertEquals("EMITIDO", emitido.getEstado(), "Al confirmar el documento queda EMITIDO");

            Long idEmitido = emitido.getId();
            DentrixException error = assertThrows(DentrixException.class,
                    () -> servicio.agregarItem(idEmitido, null, "Limpieza",
                            new BigDecimal("1"), new BigDecimal("10.00")));
            assertTrue(error.getMessage().contains("BORRADOR"),
                    "No se edita el detalle de un documento emitido: " + error.getMessage());

            servicio.anular(idEmitido, "Error de digitacion", 1);
            Pago anulado = pagoDAO.buscarPorId(idEmitido).orElseThrow();
            assertEquals("ANULADO", anulado.getEstado(), "Con motivo el documento queda ANULADO");
        } finally {
            borrarDocumento(borradorId);
            borrarDocumento(documentoId);
        }
    }
}
