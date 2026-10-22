package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.TarifarioDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Tarifario;
import pe.edu.utp.dentrix.model.TarifarioItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TarifarioDAOImpl implements TarifarioDAO {

    private static final String COLUMNAS =
            "id, version, moneda, vigencia_desde, vigencia_hasta, estado, motivo, creado_por, "
                    + "publicado_por, fecha_publicacion";

    private Tarifario leer(ResultSet rs) throws SQLException {
        Tarifario tarifario = new Tarifario();
        tarifario.setId(rs.getLong("id"));
        tarifario.setVersion(rs.getObject("version", Integer.class));
        tarifario.setMoneda(rs.getString("moneda"));
        tarifario.setVigenciaDesde(rs.getObject("vigencia_desde", LocalDate.class));
        tarifario.setVigenciaHasta(rs.getObject("vigencia_hasta", LocalDate.class));
        tarifario.setEstado(rs.getString("estado"));
        tarifario.setMotivo(rs.getString("motivo"));
        tarifario.setCreadoPor(rs.getObject("creado_por", Integer.class));
        tarifario.setPublicadoPor(rs.getObject("publicado_por", Integer.class));
        tarifario.setFechaPublicacion(rs.getObject("fecha_publicacion", LocalDateTime.class));
        return tarifario;
    }

    private TarifarioItem leerItem(ResultSet rs) throws SQLException {
        TarifarioItem item = new TarifarioItem();
        item.setId(rs.getObject("id", Long.class));
        item.setTarifarioId(rs.getObject("tarifario_id", Long.class));
        item.setTratamientoId(rs.getObject("tratamiento_id", Integer.class));
        item.setTratamientoNombre(rs.getString("tratamiento_nombre"));
        item.setPrecio(rs.getBigDecimal("precio"));
        return item;
    }

    @Override
    public Optional<Tarifario> buscarPorId(Long id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tarifario WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el tarifario.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Tarifario> buscarPublicado() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tarifario WHERE estado = 'PUBLICADO' "
                + "ORDER BY version DESC LIMIT 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(leer(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el tarifario publicado.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Tarifario> listarTodos() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM tarifario ORDER BY version DESC";
        List<Tarifario> tarifarios = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tarifarios.add(leer(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar el historial de tarifarios.", e);
        }
        return tarifarios;
    }

    @Override
    public List<TarifarioItem> listarItems(Long tarifarioId) throws DentrixException {
        String sql = "SELECT ti.id, ti.tarifario_id, ti.tratamiento_id, t.nombre AS tratamiento_nombre, "
                + "ti.precio FROM tarifario_item ti "
                + "JOIN tratamiento t ON t.id = ti.tratamiento_id "
                + "WHERE ti.tarifario_id = ? ORDER BY t.codigo";
        List<TarifarioItem> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tarifarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(leerItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron listar los items del tarifario.", e);
        }
        return items;
    }

    @Override
    public List<TarifarioItem> listarVigentes() throws DentrixException {
        String sql = "SELECT NULL::bigint AS id, v.tarifario_id, v.tratamiento_id, "
                + "t.nombre AS tratamiento_nombre, v.precio "
                + "FROM v_precio_vigente v JOIN tratamiento t ON t.id = v.tratamiento_id "
                + "ORDER BY t.codigo";
        List<TarifarioItem> items = new ArrayList<>();
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(leerItem(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudieron consultar los precios vigentes.", e);
        }
        return items;
    }

    @Override
    public int ultimaVersion() throws DentrixException {
        String sql = "SELECT COALESCE(MAX(version), 0) FROM tarifario";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo obtener la ultima version del tarifario.", e);
        }
    }

    @Override
    public Long insertar(Tarifario tarifario) throws DentrixException {
        String sql = "INSERT INTO tarifario (version, moneda, vigencia_desde, vigencia_hasta, estado, "
                + "motivo, creado_por) VALUES (?, ?, ?, ?, ?::estado_tarifario, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, tarifario.getVersion());
            ps.setString(2, tarifario.getMoneda() == null ? "PEN" : tarifario.getMoneda());
            ps.setObject(3, tarifario.getVigenciaDesde());
            ps.setObject(4, tarifario.getVigenciaHasta());
            ps.setString(5, tarifario.getEstado() == null ? "BORRADOR" : tarifario.getEstado());
            ps.setObject(6, tarifario.getMotivo());
            ps.setObject(7, tarifario.getCreadoPor());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new DentrixException("No se genero el identificador del tarifario.");
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el tarifario.", e);
        }
    }

    @Override
    public void insertarItem(TarifarioItem item) throws DentrixException {
        String sql = "INSERT INTO tarifario_item (tarifario_id, tratamiento_id, precio) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, item.getTarifarioId());
            ps.setObject(2, item.getTratamientoId());
            ps.setBigDecimal(3, item.getPrecio());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el item del tarifario.", e);
        }
    }

    @Override
    public boolean actualizarPrecio(Long tarifarioId, Integer tratamientoId, BigDecimal precio)
            throws DentrixException {
        String sql = "UPDATE tarifario_item SET precio = ? WHERE tarifario_id = ? AND tratamiento_id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBigDecimal(1, precio);
            ps.setLong(2, tarifarioId);
            ps.setInt(3, tratamientoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el precio del tarifario.", e);
        }
    }

    @Override
    public void actualizar(Tarifario tarifario) throws DentrixException {
        String sql = "UPDATE tarifario SET estado = ?::estado_tarifario, publicado_por = ?, fecha_publicacion = ?, "
                + "vigencia_hasta = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, tarifario.getEstado());
            ps.setObject(2, tarifario.getPublicadoPor());
            ps.setObject(3, tarifario.getFechaPublicacion());
            ps.setObject(4, tarifario.getVigenciaHasta());
            ps.setLong(5, tarifario.getId());
            if (ps.executeUpdate() == 0) {
                throw new DentrixException("No se encontro el tarifario a actualizar.");
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el tarifario.", e);
        }
    }

    @Override
    public void publicar(Long tarifarioId, Integer usuarioId) throws DentrixException {
        String sql = "SELECT publicar_tarifario(?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, tarifarioId);
            ps.setInt(2, usuarioId);
            ps.execute();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo publicar el tarifario.", e);
        }
    }

    @Override
    public BigDecimal precioAFecha(Integer tratamientoId, LocalDate fecha) throws DentrixException {
        String sql = "SELECT precio_a_fecha(?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, tratamientoId);
            ps.setObject(2, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el precio a la fecha indicada.", e);
        }
    }
}
