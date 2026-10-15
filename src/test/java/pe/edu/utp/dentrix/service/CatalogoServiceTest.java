package pe.edu.utp.dentrix.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.impl.ConfiguracionDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConsultorioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.DiagnosticoCie10DAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;
import pe.edu.utp.dentrix.model.DiagnosticoCie10;
import pe.edu.utp.dentrix.model.Tratamiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CatalogoServiceTest {

    private boolean baseDisponible() {
        try (Connection cn = ConexionBD.abrir()) {
            return cn != null;
        } catch (DentrixException | SQLException e) {
            return false;
        }
    }

    private CatalogoService crearServicio() {
        return new CatalogoService(new TratamientoDAOImpl(), new DiagnosticoCie10DAOImpl(),
                new ConsultorioDAOImpl(), new ConfiguracionDAOImpl());
    }

    @Test
    void laSemillaTieneLosCatalogosEsperados() throws Exception {
        assumeTrue(baseDisponible(), "config.properties o BD no disponibles: se omite esta prueba");

        CatalogoService servicio = crearServicio();

        assertEquals(5, servicio.listarTratamientos().size(),
                "La semilla del tarifario debe tener 5 tratamientos");
        assertEquals(5, servicio.listarTratamientosActivos().size(),
                "Los 5 tratamientos de la semilla deben estar activos");
        assertEquals(6, servicio.listarDiagnosticos().size(),
                "La semilla CIE-10 debe tener 6 diagnósticos");
        assertEquals(4, servicio.listarDiagnosticosFrecuentes().size(),
                "De los 6 diagnósticos, 4 deben estar marcados como frecuentes");
        assertEquals(2, servicio.listarConsultorios().size(),
                "La clínica debe tener 2 consultorios (C01 y C02)");

        Configuracion configuracion = servicio.obtenerConfiguracion();
        assertEquals("20608945123", configuracion.getRuc(), "El RUC de la semilla debe ser 20608945123");
        assertEquals("B001", configuracion.getSerieBoleta());
        assertTrue(configuracion.getIgvPorcentaje().compareTo(new BigDecimal("18.00")) == 0,
                "El IGV de la semilla debe ser 18%");
    }

    @Test
    void registrarTratamientoValidaPrecioYCodigoUnicoSinEscribir() throws Exception {
        assumeTrue(baseDisponible(), "config.properties o BD no disponibles: se omite esta prueba");

        CatalogoService servicio = crearServicio();

        Tratamiento negativo = new Tratamiento("TMP-NEG", "Temporal", "Operatoria", "PIEZA",
                new BigDecimal("-1.00"));
        DentrixException errorPrecio = assertThrows(DentrixException.class,
                () -> servicio.registrarTratamiento(negativo));
        assertTrue(errorPrecio.getMessage().contains("no puede ser negativo"),
                "Mensaje de precio negativo amigable: " + errorPrecio.getMessage());

        Tratamiento duplicado = new Tratamiento("TRAT-01", "Duplicado", "Operatoria", "PIEZA",
                new BigDecimal("10.00"));
        DentrixException errorCodigo = assertThrows(DentrixException.class,
                () -> servicio.registrarTratamiento(duplicado));
        assertEquals("Ya existe un tratamiento con el código TRAT-01", errorCodigo.getMessage());

        assertEquals(5, servicio.listarTratamientos().size(),
                "Ninguna de las validaciones rechazadas debe haber escrito filas");
    }

    @Test
    void registrarDiagnosticoValidaElFormatoDelCodigoCie10() throws Exception {
        assumeTrue(baseDisponible(), "config.properties o BD no disponibles: se omite esta prueba");

        CatalogoService servicio = crearServicio();

        DiagnosticoCie10 sinFormato = new DiagnosticoCie10("XYZ", "Código inválido", "Sin capítulo", false);
        DentrixException errorFormato = assertThrows(DentrixException.class,
                () -> servicio.registrarDiagnostico(sinFormato));
        assertTrue(errorFormato.getMessage().contains("formato tipo K02.1"),
                "Mensaje de formato CIE-10 amigable: " + errorFormato.getMessage());

        DiagnosticoCie10 demasiadoLargo = new DiagnosticoCie10("K02.15", "Demasiado largo", "K02", false);
        assertThrows(DentrixException.class, () -> servicio.registrarDiagnostico(demasiadoLargo));

        DiagnosticoCie10 duplicado = new DiagnosticoCie10("K02.1", "Duplicado", "K02 Caries dental", false);
        DentrixException errorDuplicado = assertThrows(DentrixException.class,
                () -> servicio.registrarDiagnostico(duplicado));
        assertEquals("Ya existe un diagnóstico CIE-10 con el código K02.1", errorDuplicado.getMessage());

        assertEquals(6, servicio.listarDiagnosticos().size(),
                "Ninguna validación rechazada debe haber escrito filas");
    }

    @Test
    void eliminarDiagnosticoEnUsoDevuelveMensajeAmigable() throws Exception {
        assumeTrue(baseDisponible(), "config.properties o BD no disponibles: se omite esta prueba");

        CatalogoService servicio = crearServicio();

        DentrixException error = assertThrows(DentrixException.class,
                () -> servicio.eliminarDiagnostico("K02.1"));
        assertTrue(error.getMessage().contains("está en uso por fichas clínicas"),
                "Mensaje de FK amigable: " + error.getMessage());

        assertEquals(6, servicio.listarDiagnosticos().size(),
                "El diagnóstico en uso no debe haberse eliminado");
    }
}
