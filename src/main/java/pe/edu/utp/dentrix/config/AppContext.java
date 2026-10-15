package pe.edu.utp.dentrix.config;

import pe.edu.utp.dentrix.dao.CierreCajaDAO;
import pe.edu.utp.dentrix.dao.CitaDAO;
import pe.edu.utp.dentrix.dao.ConfiguracionDAO;
import pe.edu.utp.dentrix.dao.ConsultorioDAO;
import pe.edu.utp.dentrix.dao.DiagnosticoCie10DAO;
import pe.edu.utp.dentrix.dao.FichaClinicaDAO;
import pe.edu.utp.dentrix.dao.FichaDiagnosticoDAO;
import pe.edu.utp.dentrix.dao.HallazgoDentalDAO;
import pe.edu.utp.dentrix.dao.PacienteDAO;
import pe.edu.utp.dentrix.dao.PagoDAO;
import pe.edu.utp.dentrix.dao.PlanTratamientoDAO;
import pe.edu.utp.dentrix.dao.RolDAO;
import pe.edu.utp.dentrix.dao.TratamientoDAO;
import pe.edu.utp.dentrix.dao.UsuarioDAO;
import pe.edu.utp.dentrix.dao.impl.CierreCajaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.CitaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConfiguracionDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ConsultorioDAOImpl;
import pe.edu.utp.dentrix.dao.impl.DiagnosticoCie10DAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaClinicaDAOImpl;
import pe.edu.utp.dentrix.dao.impl.FichaDiagnosticoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.HallazgoDentalDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PacienteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PagoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.PlanTratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.ReporteDAOImpl;
import pe.edu.utp.dentrix.dao.impl.RolDAOImpl;
import pe.edu.utp.dentrix.dao.impl.TratamientoDAOImpl;
import pe.edu.utp.dentrix.dao.impl.UsuarioDAOImpl;
import pe.edu.utp.dentrix.service.AgendaService;
import pe.edu.utp.dentrix.service.AutenticacionService;
import pe.edu.utp.dentrix.service.BackupService;
import pe.edu.utp.dentrix.service.CajaService;
import pe.edu.utp.dentrix.service.CatalogoService;
import pe.edu.utp.dentrix.service.FichaClinicaService;
import pe.edu.utp.dentrix.service.PacienteService;
import pe.edu.utp.dentrix.service.ReporteService;
import pe.edu.utp.dentrix.service.UsuarioService;

public final class AppContext {

    private static AppContext instancia;

    private final RolDAO rolDAO = new RolDAOImpl();
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAOImpl();
    private final ConsultorioDAO consultorioDAO = new ConsultorioDAOImpl();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();
    private final DiagnosticoCie10DAO diagnosticoCie10DAO = new DiagnosticoCie10DAOImpl();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAOImpl();
    private final CitaDAO citaDAO = new CitaDAOImpl();
    private final FichaClinicaDAO fichaClinicaDAO = new FichaClinicaDAOImpl();
    private final FichaDiagnosticoDAO fichaDiagnosticoDAO = new FichaDiagnosticoDAOImpl();
    private final HallazgoDentalDAO hallazgoDentalDAO = new HallazgoDentalDAOImpl();
    private final CierreCajaDAO cierreCajaDAO = new CierreCajaDAOImpl();
    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final PlanTratamientoDAO planTratamientoDAO = new PlanTratamientoDAOImpl();

    private final AutenticacionService autenticacion;
    private final CatalogoService catalogo;
    private final PacienteService pacientes;
    private final AgendaService agenda;
    private final FichaClinicaService fichas;
    private final CajaService caja;
    private final ReporteService reportes;
    private final UsuarioService usuariosService;
    private final BackupService respaldos;
    private final SesionActual sesion = new SesionActual();

    private AppContext() {
        autenticacion = new AutenticacionService(usuarioDAO, rolDAO);
        catalogo = new CatalogoService(tratamientoDAO, diagnosticoCie10DAO, consultorioDAO, configuracionDAO);
        pacientes = new PacienteService(pacienteDAO);
        agenda = new AgendaService(citaDAO, pacienteDAO, consultorioDAO, tratamientoDAO);
        fichas = new FichaClinicaService(fichaClinicaDAO, fichaDiagnosticoDAO, hallazgoDentalDAO,
                planTratamientoDAO, citaDAO, diagnosticoCie10DAO, tratamientoDAO);
        caja = new CajaService(pagoDAO, cierreCajaDAO, pacienteDAO, tratamientoDAO,
                fichaClinicaDAO, planTratamientoDAO, configuracionDAO);
        reportes = new ReporteService(new ReporteDAOImpl(), pagoDAO);
        usuariosService = new UsuarioService(usuarioDAO, rolDAO);
        respaldos = new BackupService();
    }

    public static AppContext get() {
        if (instancia == null) {
            instancia = new AppContext();
        }
        return instancia;
    }

    public AutenticacionService autenticacion() {
        return autenticacion;
    }

    public CatalogoService catalogo() {
        return catalogo;
    }

    public PacienteService pacientes() {
        return pacientes;
    }

    public AgendaService agenda() {
        return agenda;
    }

    public FichaClinicaService fichas() {
        return fichas;
    }

    public CajaService caja() {
        return caja;
    }

    public ReporteService reportes() {
        return reportes;
    }

    public UsuarioService gestionUsuarios() {
        return usuariosService;
    }

    public BackupService respaldos() {
        return respaldos;
    }

    public SesionActual sesion() {
        return sesion;
    }

    public UsuarioDAO usuarios() {
        return usuarioDAO;
    }

    public RolDAO roles() {
        return rolDAO;
    }
}
