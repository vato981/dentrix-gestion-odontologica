package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.TarifarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.TarifarioItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TarifarioService {

    private final TarifarioDAO tarifarioDAO;

    public TarifarioService(TarifarioDAO tarifarioDAO) {
        this.tarifarioDAO = tarifarioDAO;
    }

    public Map<Integer, BigDecimal> preciosVigentes() throws DentrixException {
        Map<Integer, BigDecimal> precios = new HashMap<>();
        for (TarifarioItem item : tarifarioDAO.listarVigentes()) {
            precios.put(item.getTratamientoId(), item.getPrecio());
        }
        return precios;
    }

    public BigDecimal precioVigente(Integer tratamientoId) throws DentrixException {
        if (tratamientoId == null) {
            throw new DentrixException("El tratamiento es obligatorio para consultar su precio.");
        }
        BigDecimal precio = preciosVigentes().get(tratamientoId);
        if (precio == null) {
            throw new DentrixException(
                    "El tratamiento " + tratamientoId + " no tiene precio vigente en el tarifario");
        }
        return precio;
    }

    public List<Tarifario> historial() throws DentrixException {
        return tarifarioDAO.listarTodos();
    }

    public List<TarifarioItem> items(Long tarifarioId) throws DentrixException {
        if (tarifarioId == null) {
            throw new DentrixException("El tarifario es obligatorio.");
        }
        return tarifarioDAO.listarItems(tarifarioId);
    }

    public Optional<Tarifario> versionPublicada() throws DentrixException {
        return tarifarioDAO.buscarPublicado();
    }

    public Tarifario abrirVersion(LocalDate vigenciaDesde, String motivo, Integer usuarioId)
            throws DentrixException {
        if (vigenciaDesde == null) {
            throw new DentrixException("La fecha de vigencia inicial es obligatoria.");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que abre la version es obligatorio.");
        }
        Tarifario borrador = new Tarifario(tarifarioDAO.ultimaVersion() + 1, "PEN", vigenciaDesde,
                motivo, usuarioId);
        borrador.setEstado("BORRADOR");
        Long id = tarifarioDAO.insertar(borrador);
        borrador.setId(id);
        Optional<Tarifario> publicado = tarifarioDAO.buscarPublicado();
        if (publicado.isPresent()) {
            for (TarifarioItem original : tarifarioDAO.listarItems(publicado.get().getId())) {
                TarifarioItem copia = new TarifarioItem();
                copia.setTarifarioId(id);
                copia.setTratamientoId(original.getTratamientoId());
                copia.setPrecio(original.getPrecio());
                tarifarioDAO.insertarItem(copia);
            }
        }
        return borrador;
    }

    public void fijarPrecio(Long tarifarioId, Integer tratamientoId, BigDecimal precio)
            throws DentrixException {
        if (tarifarioId == null) {
            throw new DentrixException("El tarifario es obligatorio.");
        }
        if (tratamientoId == null) {
            throw new DentrixException("El tratamiento es obligatorio.");
        }
        if (precio == null) {
            throw new DentrixException("El precio es obligatorio.");
        }
        if (precio.signum() < 0) {
            throw new DentrixException("El precio no puede ser negativo.");
        }
        Tarifario tarifario = tarifarioDAO.buscarPorId(tarifarioId)
                .orElseThrow(() -> new DentrixException("No existe el tarifario " + tarifarioId + "."));
        if (!tarifario.esBorrador()) {
            throw new DentrixException("Solo se pueden fijar precios en un tarifario BORRADOR; el tarifario "
                    + tarifarioId + " se encuentra en estado " + tarifario.getEstado() + ".");
        }
        if (!tarifarioDAO.actualizarPrecio(tarifarioId, tratamientoId, precio)) {
            TarifarioItem item = new TarifarioItem();
            item.setTarifarioId(tarifarioId);
            item.setTratamientoId(tratamientoId);
            item.setPrecio(precio);
            tarifarioDAO.insertarItem(item);
        }
    }

    public void publicar(Long tarifarioId, Integer usuarioId) throws DentrixException {
        if (tarifarioId == null) {
            throw new DentrixException("El tarifario a publicar es obligatorio.");
        }
        if (usuarioId == null) {
            throw new DentrixException("El usuario que publica es obligatorio.");
        }
        try {
            tarifarioDAO.publicar(tarifarioId, usuarioId);
        } catch (DentrixException e) {
            throw new DentrixException("No se pudo publicar el tarifario: " + detalle(e), e);
        }
    }

    public BigDecimal precioAFecha(Integer tratamientoId, LocalDate fecha) throws DentrixException {
        if (tratamientoId == null) {
            throw new DentrixException("El tratamiento es obligatorio.");
        }
        if (fecha == null) {
            throw new DentrixException("La fecha es obligatoria.");
        }
        BigDecimal precio = tarifarioDAO.precioAFecha(tratamientoId, fecha);
        if (precio == null) {
            throw new DentrixException(
                    "El tratamiento " + tratamientoId + " no tiene precio vigente al " + fecha);
        }
        return precio;
    }

    private String detalle(DentrixException e) {
        Throwable causa = e.getCause();
        if (causa instanceof SQLException sqle) {
            String texto = sqle.getMessage();
            if (texto != null && !texto.isBlank()) {
                int salto = texto.indexOf('\n');
                return salto > 0 ? texto.substring(0, salto) : texto;
            }
        }
        return e.getMessage();
    }
}
