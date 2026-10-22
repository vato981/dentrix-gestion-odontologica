package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Paciente;

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

public class PacienteDAOImpl implements PacienteDAO {

    private static final String COLUMNAS =
            "id, codigo_hc, dni, apellidos, nombres, fecha_nac, celular, email, direccion, "
                    + "alergias, antecedentes, tutor_nombre, tutor_documento, tutor_vinculo, "
                    + "pagador_nombre, pagador_documento, fecha_apertura, estado, usuario_registro_id, "
                    + "fecha_registro, motivo_anulacion, fecha_anulacion";

    private Paciente leer(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente();
        paciente.setId(rs.getInt(1));
        paciente.setCodigoHc(rs.getString(2));
        paciente.setDni(rs.getString(3));
        paciente.setApellidos(rs.getString(4));
        paciente.setNombres(rs.getString(5));
        paciente.setFechaNac(rs.getObject(6, LocalDate.class));
        paciente.setCelular(rs.getObject(7, String.class));
        paciente.setEmail(rs.getObject(8, String.class));
        paciente.setDireccion(rs.getObject(9, String.class));
        paciente.setAlergias(rs.getObject(10, String.class));
        paciente.setAntecedentes(rs.getObject(11, String.class));
        paciente.setTutorNombre(rs.getObject(12, String.class));
        paciente.setTutorDocumento(rs.getObject(13, String.class));
        paciente.setTutorVinculo(rs.getObject(14, String.class));
        paciente.setPagadorNombre(rs.getObject(15, String.class));
        paciente.setPagadorDocumento(rs.getObject(16, String.class));
        paciente.setFechaApertura(rs.getObject(17, LocalDate.class));
        paciente.setEstado(rs.getString(18));
        paciente.setUsuarioRegistroId(rs.getObject(19, Integer.class));
        paciente.setFechaRegistro(rs.getObject(20, LocalDateTime.class));
        paciente.setMotivoAnulacion(rs.getObject(21, String.class));
        paciente.setFechaAnulacion(rs.getObject(22, LocalDateTime.class));
        return paciente;
    }

    @Override
    public List<Paciente> listarTodos() throws DentrixException {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT " + COLUMNAS + " FROM paciente ORDER BY id";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pacientes.add(leer(rs));
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo listar los pacientes.", e);
        }
        return pacientes;
    }

    @Override
    public List<Paciente> listarPorTexto(String texto) throws DentrixException {
        List<Paciente> pacientes = new ArrayList<>();
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%";
        String sql = "SELECT " + COLUMNAS + " FROM paciente "
                + "WHERE LOWER(dni) LIKE ? OR LOWER(codigo_hc) LIKE ? OR LOWER(apellidos) LIKE ? "
                + "OR LOWER(nombres) LIKE ? OR LOWER(apellidos || ' ' || nombres) LIKE ? "
                + "ORDER BY apellidos, nombres";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo buscar pacientes.", e);
        }
        return pacientes;
    }

    @Override
    public Optional<Paciente> buscarPorId(Integer id) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM paciente WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el paciente.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM paciente WHERE dni = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(leer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo consultar el paciente por DNI.", e);
        }
        return Optional.empty();
    }

    @Override
    public int insertar(Paciente paciente) throws DentrixException {
        String sql = "INSERT INTO paciente (codigo_hc, dni, apellidos, nombres, fecha_nac, celular, email, "
                + "direccion, alergias, antecedentes, tutor_nombre, tutor_documento, tutor_vinculo, "
                + "pagador_nombre, pagador_documento, fecha_apertura, estado, usuario_registro_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, paciente.getCodigoHc());
            ps.setObject(2, paciente.getDni());
            ps.setObject(3, paciente.getApellidos());
            ps.setObject(4, paciente.getNombres());
            ps.setObject(5, paciente.getFechaNac());
            ps.setObject(6, paciente.getCelular());
            ps.setObject(7, paciente.getEmail());
            ps.setObject(8, paciente.getDireccion());
            ps.setObject(9, paciente.getAlergias());
            ps.setObject(10, paciente.getAntecedentes());
            ps.setObject(11, paciente.getTutorNombre());
            ps.setObject(12, paciente.getTutorDocumento());
            ps.setObject(13, paciente.getTutorVinculo());
            ps.setObject(14, paciente.getPagadorNombre());
            ps.setObject(15, paciente.getPagadorDocumento());
            ps.setObject(16, paciente.getFechaApertura());
            ps.setObject(17, paciente.getEstado());
            ps.setObject(18, paciente.getUsuarioRegistroId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DentrixException("No se pudo registrar el paciente.", e);
        }
        throw new DentrixException("No se pudo obtener el identificador del paciente.");
    }

    @Override
    public boolean actualizar(Paciente paciente) throws DentrixException {
        String sql = "UPDATE paciente SET dni = ?, apellidos = ?, nombres = ?, fecha_nac = ?, celular = ?, "
                + "email = ?, direccion = ?, alergias = ?, antecedentes = ?, tutor_nombre = ?, "
                + "tutor_documento = ?, tutor_vinculo = ?, pagador_nombre = ?, pagador_documento = ?, "
                + "fecha_apertura = ?, estado = ?, usuario_registro_id = ?, motivo_anulacion = ?, "
                + "fecha_anulacion = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, paciente.getDni());
            ps.setObject(2, paciente.getApellidos());
            ps.setObject(3, paciente.getNombres());
            ps.setObject(4, paciente.getFechaNac());
            ps.setObject(5, paciente.getCelular());
            ps.setObject(6, paciente.getEmail());
            ps.setObject(7, paciente.getDireccion());
            ps.setObject(8, paciente.getAlergias());
            ps.setObject(9, paciente.getAntecedentes());
            ps.setObject(10, paciente.getTutorNombre());
            ps.setObject(11, paciente.getTutorDocumento());
            ps.setObject(12, paciente.getTutorVinculo());
            ps.setObject(13, paciente.getPagadorNombre());
            ps.setObject(14, paciente.getPagadorDocumento());
            ps.setObject(15, paciente.getFechaApertura());
            ps.setObject(16, paciente.getEstado());
            ps.setObject(17, paciente.getUsuarioRegistroId());
            ps.setObject(18, paciente.getMotivoAnulacion());
            ps.setObject(19, paciente.getFechaAnulacion());
            ps.setObject(20, paciente.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar el paciente.", e);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws DentrixException {
        String sql = "DELETE FROM paciente WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo eliminar el paciente.", e);
        }
    }
}
