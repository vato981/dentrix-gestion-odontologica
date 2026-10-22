package pe.edu.utp.dentrix.dao;

import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.TarifarioItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TarifarioDAO {

    Optional<Tarifario> buscarPorId(Long id) throws DentrixException;

    Optional<Tarifario> buscarPublicado() throws DentrixException;

    List<Tarifario> listarTodos() throws DentrixException;

    List<TarifarioItem> listarItems(Long tarifarioId) throws DentrixException;

    List<TarifarioItem> listarVigentes() throws DentrixException;

    int ultimaVersion() throws DentrixException;

    Long insertar(Tarifario tarifario) throws DentrixException;

    void insertarItem(TarifarioItem item) throws DentrixException;

    boolean actualizarPrecio(Long tarifarioId, Integer tratamientoId, BigDecimal precio) throws DentrixException;

    void actualizar(Tarifario tarifario) throws DentrixException;

    void publicar(Long tarifarioId, Integer usuarioId) throws DentrixException;

    BigDecimal precioAFecha(Integer tratamientoId, LocalDate fecha) throws DentrixException;
}
