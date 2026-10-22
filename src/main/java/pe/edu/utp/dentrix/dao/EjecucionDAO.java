package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.CierrePlan;
import pe.edu.utp.dentrix.model.ControlPost;
import pe.edu.utp.dentrix.model.EjecucionItem;
import pe.edu.utp.dentrix.model.Evolucion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EjecucionDAO {

    Long insertarEjecucion(EjecucionItem ejecucion) throws DentrixException;

    Optional<EjecucionItem> buscarEjecucion(Long id) throws DentrixException;

    List<EjecucionItem> listarEjecuciones(Long presupuestoItemId) throws DentrixException;

    Optional<EjecucionItem> buscarSesionBorrador(Long presupuestoItemId) throws DentrixException;

    boolean eliminarEjecucion(Long id) throws DentrixException;

    boolean anularEjecucion(Long id, String motivo) throws DentrixException;

    Long insertarEvolucion(Evolucion evolucion) throws DentrixException;

    Optional<Evolucion> buscarEvolucion(Long id) throws DentrixException;

    List<Evolucion> listarEvoluciones(Long fichaId) throws DentrixException;

    Optional<Evolucion> evolucionDeSesion(Long ejecucionItemId) throws DentrixException;

    Long insertarControl(ControlPost control) throws DentrixException;

    Optional<ControlPost> controlPendiente(Long presupuestoItemId) throws DentrixException;

    List<ControlPost> listarControles(Long presupuestoItemId) throws DentrixException;

    boolean realizarControl(Long id, LocalDate fecha, String hallazgo, Integer profesionalId)
            throws DentrixException;

    Long insertarCierre(CierrePlan cierre) throws DentrixException;

    Optional<CierrePlan> buscarCierre(Long presupuestoId) throws DentrixException;
}
