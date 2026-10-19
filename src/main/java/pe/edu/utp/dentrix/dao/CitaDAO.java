package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Cita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CitaDAO {

    List<Cita> listarTodas() throws DentrixException;

    List<Cita> listarPorFecha(LocalDate fecha) throws DentrixException;

    List<Cita> listarPorFechaYConsultorio(LocalDate fecha, Integer consultorioId) throws DentrixException;

    List<Cita> listarPorPaciente(Integer pacienteId) throws DentrixException;

    List<Cita> listarPorPacienteEnFecha(Integer pacienteId, LocalDate fecha) throws DentrixException;

    Optional<Cita> buscarPorId(Long id) throws DentrixException;

    int insertar(Cita cita) throws DentrixException;

    boolean actualizar(Cita cita) throws DentrixException;

    boolean eliminar(Long id) throws DentrixException;
}
