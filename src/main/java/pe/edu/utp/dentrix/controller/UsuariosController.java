package pe.edu.utp.dentrix.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import pe.edu.utp.dentrix.config.AppContext;
import pe.edu.utp.dentrix.model.Rol;
import pe.edu.utp.dentrix.model.Usuario;
import pe.edu.utp.dentrix.service.UsuarioService;
import pe.edu.utp.dentrix.ui.Badges;
import pe.edu.utp.dentrix.ui.Navegador;
import pe.edu.utp.dentrix.ui.TareasFX;

import java.util.List;

public class UsuariosController {

    private static final Tooltip TOOLTIP_SESION =
            new Tooltip("No puedes desactivar tu propia sesión");

    private final UsuarioService servicio = AppContext.get().gestionUsuarios();

    private final ObservableList<Usuario> filas = FXCollections.observableArrayList();
    private final ObservableList<Rol> roles = FXCollections.observableArrayList();

    @FXML private TextField campoUsername;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoDni;
    @FXML private TextField campoColegiatura;
    @FXML private TextField campoEspecialidad;
    @FXML private PasswordField campoClave;
    @FXML private ComboBox<Rol> comboRol;
    @FXML private Label etiquetaModo;
    @FXML private Label etiquetaError;
    @FXML private Label etiquetaConteo;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colUsername;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colDni;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colColegiatura;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, Void> colAcciones;

    private Integer idEnEdicion;

    @FXML
    private void initialize() {
        comboRol.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Rol rol) {
                return rol == null ? "" : rol.getNombre();
            }

            @Override
            public Rol fromString(String string) {
                return null;
            }
        });
        colUsername.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));
        colNombre.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().nombreCompleto()));
        colDni.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDni() == null ? "" : d.getValue().getDni()));
        colRol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(rolDe(d.getValue())));
        colColegiatura.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getColegiatura() == null ? "—" : d.getValue().getColegiatura()));
        colEstado.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEstado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    setGraphic(Badges.estado(item));
                    setAlignment(Pos.CENTER);
                }
            }
        });
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button botonEditar = new Button("✏ Editar");
            private final Button botonEstado = new Button();
            private final HBox contenedor = new HBox(6, botonEditar, botonEstado);

            {
                botonEditar.getStyleClass().add("boton-mini");
                botonEstado.getStyleClass().addAll("boton-mini", "boton-peligro");
                botonEditar.setOnAction(e -> editar(getTableRow().getItem()));
                botonEstado.setOnAction(e -> alternarEstado(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Usuario usuario = getTableRow().getItem();
                    String estado = usuario.getEstado();
                    boolean activo = "ACTIVO".equals(estado);
                    boolean bloqueado = "BLOQUEADO".equals(estado);
                    botonEstado.setText(bloqueado ? "Desbloquear" : (activo ? "✖ Inactivar" : "✔ Activar"));
                    botonEstado.getStyleClass().removeAll("boton-peligro", "boton-primario");
                    botonEstado.getStyleClass().add(activo ? "boton-peligro" : "boton-primario");
                    boolean esSesionActual = sesionActual(usuario);
                    botonEstado.setDisable(esSesionActual);
                    botonEstado.setTooltip(esSesionActual ? TOOLTIP_SESION : null);
                    setGraphic(contenedor);
                }
            }
        });
        tablaUsuarios.setItems(filas);
        onNuevoClick();
        recargarAsync();
    }

    @FXML
    private void onVolverClick() {
        Navegador.ir(Navegador.INICIO);
    }

    @FXML
    private void onRefrescarClick() {
        recargarAsync();
    }

    @FXML
    private void onNuevoClick() {
        idEnEdicion = null;
        etiquetaModo.setText("NUEVO");
        campoUsername.clear();
        campoNombres.clear();
        campoApellidos.clear();
        campoDni.clear();
        campoColegiatura.clear();
        campoEspecialidad.clear();
        campoClave.clear();
        comboRol.getSelectionModel().clearSelection();
        etiquetaError.setText("");
    }

    @FXML
    private void onGuardarClick() {
        try {
            Usuario usuario = construir();
            String clave = campoClave.getText() == null ? "" : campoClave.getText();
            Integer idEdicion = idEnEdicion;
            TareasFX.ejecutar(() -> {
                if (idEdicion == null) {
                    return servicio.registrar(usuario, clave);
                }
                usuario.setId(idEdicion);
                Usuario existente = servicio.listar().stream()
                        .filter(u -> idEdicion.equals(u.getId()))
                        .findFirst()
                        .orElse(usuario);
                usuario.setPasswordHash(existente.getPasswordHash());
                usuario.setEstado(existente.getEstado());
                usuario.setIntentosFallidos(existente.getIntentosFallidos());
                usuario.setBloqueadoHasta(existente.getBloqueadoHasta());
                usuario.setUltimoAcceso(existente.getUltimoAcceso());
                servicio.editar(usuario);
                if (!clave.isBlank()) {
                    servicio.cambiarClave(idEdicion, clave);
                }
                return usuario;
            }, guardado -> {
                idEnEdicion = guardado.getId();
                etiquetaModo.setText("EDITAR (id " + guardado.getId() + ")");
                campoClave.clear();
                etiquetaError.setText("");
                recargarAsync();
            }, error -> etiquetaError.setText(TareasFX.mensaje(error)));
        } catch (Exception e) {
            etiquetaError.setText(e.getMessage());
        }
    }

    private void editar(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        idEnEdicion = usuario.getId();
        etiquetaModo.setText("EDITAR (id " + usuario.getId() + ")");
        campoUsername.setText(usuario.getUsername());
        campoNombres.setText(usuario.getNombres());
        campoApellidos.setText(usuario.getApellidos());
        campoDni.setText(usuario.getDni());
        campoColegiatura.setText(usuario.getColegiatura());
        campoEspecialidad.setText(usuario.getEspecialidad());
        campoClave.clear();
        comboRol.getItems().stream()
                .filter(r -> r.getId().equals(usuario.getRolId()))
                .findFirst()
                .ifPresent(r -> comboRol.getSelectionModel().select(r));
        etiquetaError.setText("");
    }

    private void alternarEstado(Usuario usuario) {
        if (usuario == null || sesionActual(usuario)) {
            return;
        }
        String nuevo = "ACTIVO".equals(usuario.getEstado()) ? "INACTIVO" : "ACTIVO";
        TareasFX.ejecutar(() -> {
            servicio.cambiarEstado(usuario.getId(), nuevo);
            return null;
        }, ok -> recargarAsync(), error -> etiquetaError.setText(TareasFX.mensaje(error)));
    }

    private Usuario construir() {
        Usuario usuario = new Usuario();
        usuario.setUsername(campoUsername.getText() == null ? "" : campoUsername.getText().trim());
        usuario.setNombres(campoNombres.getText() == null ? "" : campoNombres.getText().trim());
        usuario.setApellidos(campoApellidos.getText() == null ? "" : campoApellidos.getText().trim());
        String dni = campoDni.getText() == null ? "" : campoDni.getText().trim();
        usuario.setDni(dni.isEmpty() ? null : dni);
        usuario.setColegiatura(vacioANull(campoColegiatura.getText()));
        usuario.setEspecialidad(vacioANull(campoEspecialidad.getText()));
        Rol rol = comboRol.getSelectionModel().getSelectedItem();
        usuario.setRolId(rol == null ? null : rol.getId());
        return usuario;
    }

    private void recargarAsync() {
        TareasFX.ejecutar(() -> new DatosUsuarios(servicio.listarRoles(), servicio.listar()),
                this::aplicarDatos,
                error -> etiquetaError.setText("No se pudo cargar el padrón: " + TareasFX.mensaje(error)));
    }

    private void aplicarDatos(DatosUsuarios datos) {
        roles.setAll(datos.roles());
        comboRol.setItems(roles);
        filas.setAll(datos.usuarios());
        etiquetaConteo.setText(datos.usuarios().size() + " USUARIOS · M9.1");
    }

    private record DatosUsuarios(List<Rol> roles, List<Usuario> usuarios) {
    }

    private String rolDe(Usuario usuario) {
        return roles.stream()
                .filter(r -> r.getId().equals(usuario.getRolId()))
                .map(Rol::getNombre)
                .findFirst()
                .orElse("—");
    }

    private boolean sesionActual(Usuario usuario) {
        Usuario actual = AppContext.get().sesion().getUsuario();
        return actual != null && actual.getId() != null && actual.getId().equals(usuario.getId());
    }

    private String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
