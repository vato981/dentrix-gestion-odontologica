package pe.edu.utp.dentrix.service;

import pe.edu.utp.dentrix.dao.impl.ReporteDAOImpl;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dto.KpiDia;
import pe.edu.utp.dentrix.dto.KpiMes;
import pe.edu.utp.dentrix.dto.ProductividadOdontologo;
import pe.edu.utp.dentrix.dto.RankingTratamiento;
import pe.edu.utp.dentrix.dto.SaldoPaciente;
import pe.edu.utp.dentrix.excepcion.DentrixException;
import pe.edu.utp.dentrix.model.Pago;

import java.time.LocalDate;
import java.util.List;

public class ReporteService {

    private final ReporteDAOImpl reporteDAO;
    private final PagoDAO pagoDAO;

    public ReporteService(ReporteDAOImpl reporteDAO, PagoDAO pagoDAO) {
        this.reporteDAO = reporteDAO;
        this.pagoDAO = pagoDAO;
    }

    public KpiDia kpisDeFecha(LocalDate fecha) throws DentrixException {
        if (fecha == null) {
            throw new DentrixException("La fecha del reporte de KPI es obligatoria");
        }
        return reporteDAO.kpisDeFecha(fecha);
    }

    public KpiMes kpisDelMes(LocalDate desde, LocalDate hasta) throws DentrixException {
        if (desde == null || hasta == null) {
            throw new DentrixException("El periodo del reporte mensual es obligatorio (desde y hasta)");
        }
        if (desde.isAfter(hasta)) {
            throw new DentrixException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return reporteDAO.kpisDelMes(desde, hasta);
    }

    public List<ProductividadOdontologo> productividadOdontologos(LocalDate desde, LocalDate hasta)
            throws DentrixException {
        if (desde == null || hasta == null) {
            throw new DentrixException("El periodo del reporte es obligatorio (desde y hasta)");
        }
        return reporteDAO.productividadOdontologos(desde, hasta);
    }

    public List<SaldoPaciente> saldosPacientes() throws DentrixException {
        return reporteDAO.saldosPacientes();
    }

    public List<RankingTratamiento> rankingTratamientos() throws DentrixException {
        return reporteDAO.rankingTratamientos();
    }

    public List<Pago> documentosConSaldo() throws DentrixException {
        return pagoDAO.listarDocumentosConSaldo();
    }
}
