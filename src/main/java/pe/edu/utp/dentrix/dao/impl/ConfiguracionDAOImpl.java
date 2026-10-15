package pe.edu.utp.dentrix.dao.impl;

import pe.edu.utp.dentrix.config.ConexionBD;
import pe.edu.utp.dentrix.dao.ConfiguracionDAO;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Configuracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ConfiguracionDAOImpl implements ConfiguracionDAO {

    private static final String COLUMNAS = "id, razon_social, ruc, direccion, telefono, email, "
            + "representante_legal, medico_director, licencia_sanitaria, licencia_municipal, "
            + "hora_apertura, hora_cierre, almuerzo_inicio, almuerzo_fin, igv_porcentaje, serie_boleta";

    private Configuracion mapear(ResultSet rs) throws SQLException {
        Configuracion c = new Configuracion();
        c.setId(rs.getInt("id"));
        c.setRazonSocial(rs.getString("razon_social"));
        c.setRuc(rs.getString("ruc"));
        c.setDireccion(rs.getString("direccion"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setRepresentanteLegal(rs.getString("representante_legal"));
        c.setMedicoDirector(rs.getString("medico_director"));
        c.setLicenciaSanitaria(rs.getString("licencia_sanitaria"));
        c.setLicenciaMunicipal(rs.getString("licencia_municipal"));
        c.setHoraApertura(rs.getObject("hora_apertura", java.time.LocalTime.class));
        c.setHoraCierre(rs.getObject("hora_cierre", java.time.LocalTime.class));
        c.setAlmuerzoInicio(rs.getObject("almuerzo_inicio", java.time.LocalTime.class));
        c.setAlmuerzoFin(rs.getObject("almuerzo_fin", java.time.LocalTime.class));
        c.setIgvPorcentaje(rs.getBigDecimal("igv_porcentaje"));
        c.setSerieBoleta(rs.getString("serie_boleta"));
        return c;
    }

    @Override
    public Optional<Configuracion> obtener() throws DentrixException {
        String sql = "SELECT " + COLUMNAS + " FROM configuracion WHERE id = 1";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapear(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DentrixException("No se pudo leer la configuración de la clínica.", e);
        }
    }

    @Override
    public boolean actualizar(Configuracion configuracion) throws DentrixException {
        String sql = "UPDATE configuracion SET razon_social = ?, ruc = ?, direccion = ?, telefono = ?, "
                + "email = ?, representante_legal = ?, medico_director = ?, licencia_sanitaria = ?, "
                + "licencia_municipal = ?, hora_apertura = ?, hora_cierre = ?, almuerzo_inicio = ?, "
                + "almuerzo_fin = ?, igv_porcentaje = ?, serie_boleta = ? WHERE id = ?";
        try (Connection cn = ConexionBD.abrir();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, configuracion.getRazonSocial());
            ps.setString(2, configuracion.getRuc());
            ps.setString(3, configuracion.getDireccion());
            ps.setString(4, configuracion.getTelefono());
            ps.setString(5, configuracion.getEmail());
            ps.setString(6, configuracion.getRepresentanteLegal());
            ps.setString(7, configuracion.getMedicoDirector());
            ps.setString(8, configuracion.getLicenciaSanitaria());
            ps.setString(9, configuracion.getLicenciaMunicipal());
            ps.setObject(10, configuracion.getHoraApertura());
            ps.setObject(11, configuracion.getHoraCierre());
            ps.setObject(12, configuracion.getAlmuerzoInicio());
            ps.setObject(13, configuracion.getAlmuerzoFin());
            ps.setBigDecimal(14, configuracion.getIgvPorcentaje());
            ps.setString(15, configuracion.getSerieBoleta());
            ps.setInt(16, configuracion.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DentrixException("No se pudo actualizar la configuración de la clínica.", e);
        }
    }
}
