package pe.edu.utp.dentrix.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.excepcion.DentrixException;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AgendaFxmlTest {

    private static void arrancarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaArrancado) {
        }
    }

    private static <T> T enFx(Callable<T> accion) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                valor.set(accion.call());
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });
        if (!listo.await(20, TimeUnit.SECONDS)) {
            throw new AssertionError("La operacion en el hilo de JavaFX no termino");
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get().getMessage(), fallo.get());
        }
        return valor.get();
    }

    private static int esperarCarga(ComboBox<?> combo) throws Exception {
        long limite = System.currentTimeMillis() + 20000;
        int items = enFx(() -> combo.getItems().size());
        while (System.currentTimeMillis() < limite && items == 0) {
            Thread.sleep(200);
            items = enFx(() -> combo.getItems().size());
        }
        return items;
    }

    @Test
    void laPantallaDeAgendaCargaFiltrosCitasYPaneles() throws Exception {
        Connection conexion = null;
        try {
            conexion = ConexionBD.abrir();
        } catch (DentrixException e) {
            conexion = null;
        }
        assumeTrue(conexion != null, "config.properties o BD no disponibles: se omite esta prueba");
        conexion.close();

        var contexto = pe.edu.utp.dentrix.config.AppContext.get();
        var usuario = contexto.usuarios().buscarPorUsername("jvallejos")
                .orElseThrow(() -> new IllegalStateException("Falta el usuario jvallejos de la semilla"));
        var rol = contexto.roles().buscarPorId(usuario.getRolId())
                .orElseThrow(() -> new IllegalStateException("Falta el rol del usuario jvallejos"));
        contexto.sesion().abrir(usuario, rol);

        boolean hayCitasHoy;
        try {
            hayCitasHoy = !contexto.agenda().agendaDelDia(LocalDate.now()).isEmpty();
        } catch (DentrixException e) {
            hayCitasHoy = false;
        }

        arrancarJavaFx();
        Parent pantalla = enFx(() -> {
            Parent cargado = FXMLLoader.load(
                    getClass().getResource("/pe/edu/utp/dentrix/fxml/agenda.fxml"));
            new javafx.scene.Scene(cargado);
            cargado.getStylesheets().add(
                    getClass().getResource("/pe/edu/utp/dentrix/estilos.css").toExternalForm());
            cargado.applyCss();
            cargado.layout();
            return cargado;
        });
        assertNotNull(pantalla, "El FXML debe producir un nodo raiz");

        assertNotNull((DatePicker) pantalla.lookup("#campoFecha"), "Falta la fecha de la agenda");
        assertNotNull((ComboBox<?>) pantalla.lookup("#filtroOdontologo"), "Falta el filtro por odontologo");
        assertNotNull((ComboBox<?>) pantalla.lookup("#filtroConsultorio"), "Falta el filtro por consultorio");
        assertNotNull((ComboBox<?>) pantalla.lookup("#filtroEstado"), "Falta el filtro por estado");
        assertNotNull((ComboBox<?>) pantalla.lookup("#comboTipo"), "Falta el tipo de registro");
        assertNotNull((ComboBox<?>) pantalla.lookup("#comboPaciente"), "Falta el combo de pacientes");
        assertNotNull((ComboBox<?>) pantalla.lookup("#comboTratamiento"), "Falta el combo de tratamientos");
        assertNotNull((ComboBox<?>) pantalla.lookup("#comboOdontologo"), "Falta el combo de odontologos");
        assertNotNull((ComboBox<?>) pantalla.lookup("#comboConsultorio"), "Falta el combo de consultorios");
        assertNotNull((CheckBox) pantalla.lookup("#checkPrimeraVez"), "Falta el check Primera vez");
        assertNotNull((TextField) pantalla.lookup("#campoInicio"), "Falta la hora de inicio");
        assertNotNull((TextField) pantalla.lookup("#campoFin"), "Falta la hora de fin calculada");
        assertNotNull(pantalla.lookup("#tablaAgenda"), "Falta la tabla de agenda");
        assertNotNull(pantalla.lookup("#tablaEspera"), "Falta la tabla de lista de espera");
        assertNotNull(pantalla.lookup("#tablaRecordatorios"), "Falta la tabla de recordatorios");
        assertNotNull((Button) pantalla.lookup("#btnReprogramar"), "Falta el boton Reprogramar");
        assertNotNull((Button) pantalla.lookup("#btnHistorial"), "Falta el boton Historial");
        assertNotNull(pantalla.lookup("#etiquetaEstadoBadge"), "Falta el badge de estado del detalle");
        assertNotNull(pantalla.lookup("#etiquetaDetallePaciente"), "Falta el detalle del paciente");

        TableView<?> tabla = (TableView<?>) pantalla.lookup("#tablaAgenda");
        List<String> cabeceras = enFx(() -> tabla.getColumns().stream()
                .map(c -> c.getText() == null ? "" : c.getText()).toList());
        assertEquals(7, cabeceras.size(), "La agenda debe tener HORA, PACIENTE, TRATAMIENTO, "
                + "ODONTOLOGO, CONSULTORIO, ESTADO y ACCION: " + cabeceras);
        assertTrue(cabeceras.contains("HORA"), "Falta la columna HORA: " + cabeceras);
        assertTrue(cabeceras.contains("TRATAMIENTO"), "Falta la columna TRATAMIENTO: " + cabeceras);
        assertTrue(cabeceras.contains("ODONTOLOGO"), "Falta la columna ODONTOLOGO: " + cabeceras);
        assertTrue(cabeceras.contains("CONSULT."), "Falta la columna CONSULTORIO: " + cabeceras);
        assertTrue(cabeceras.contains("ACCION"), "Falta la columna ACCION: " + cabeceras);

        ComboBox<?> comboTipo = (ComboBox<?>) pantalla.lookup("#comboTipo");
        assertEquals(4, enFx(() -> comboTipo.getItems().size()),
                "El tipo de registro debe ofrecer CITA y los tres bloqueos");
        ComboBox<?> filtroEstado = (ComboBox<?>) pantalla.lookup("#filtroEstado");
        assertTrue(enFx(() -> filtroEstado.getItems().size()) > 5,
                "El filtro de estado debe ofrecer todos los estados de la cita");

        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#comboPaciente")) > 0,
                "El combo de pacientes debe cargar desde la base");
        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#comboTratamiento")) > 0,
                "El combo de tratamientos debe cargar el catalogo");
        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#comboOdontologo")) > 0,
                "El combo de odontologos debe cargar desde la base");
        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#comboConsultorio")) > 0,
                "El combo de consultorios debe cargar el catalogo");
        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#filtroOdontologo")) > 1,
                "El filtro de odontologo debe cargar los nombres del equipo");
        assertTrue(esperarCarga((ComboBox<?>) pantalla.lookup("#filtroConsultorio")) > 1,
                "El filtro de consultorio debe cargar los consultorios");

        CheckBox primeraVez = (CheckBox) pantalla.lookup("#checkPrimeraVez");
        assertTrue(enFx(primeraVez::isSelected), "La primera cita de un paciente nuevo solo agenda consulta");
        ComboBox<?> comboTratamiento = (ComboBox<?>) pantalla.lookup("#comboTratamiento");
        assertTrue(enFx(comboTratamiento::isDisabled),
                "Con Primera vez marcado el catalogo de tratamientos queda bloqueado (P4)");

        TextField inicio = (TextField) pantalla.lookup("#campoInicio");
        TextField fin = (TextField) pantalla.lookup("#campoFin");
        assertEquals("09:30", enFx(() -> {
            inicio.setText("09:00");
            return fin.getText();
        }), "La hora de fin se calcula con los 30 minutos de la consulta");

        if (hayCitasHoy) {
            long limite = System.currentTimeMillis() + 20000;
            int filas = 0;
            while (System.currentTimeMillis() < limite && filas == 0) {
                filas = enFx(() -> tabla.getItems().size());
                if (filas == 0) {
                    Thread.sleep(200);
                }
            }
            assertTrue(filas > 0, "La agenda debe cargar las citas del dia");
        }

        DatePicker fecha = (DatePicker) pantalla.lookup("#campoFecha");
        assertEquals(LocalDate.now(), enFx(fecha::getValue), "La agenda abre en la fecha de hoy");
        assertTrue(enFx(() -> pantalla.lookup("#etiquetaHoy").isVisible()),
                "El indicador HOY debe estar visible cuando la fecha es hoy");
    }
}
