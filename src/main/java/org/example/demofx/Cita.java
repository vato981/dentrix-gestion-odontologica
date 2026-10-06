package org.example.demofx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cita {

    private final StringProperty horario = new SimpleStringProperty();
    private final StringProperty consultorio = new SimpleStringProperty();
    private final StringProperty paciente = new SimpleStringProperty();
    private final StringProperty tratamiento = new SimpleStringProperty();
    private final StringProperty doctor = new SimpleStringProperty();

    public Cita(String horario, String consultorio, String paciente, String tratamiento, String doctor) {
        this.horario.set(horario);
        this.consultorio.set(consultorio);
        this.paciente.set(paciente);
        this.tratamiento.set(tratamiento);
        this.doctor.set(doctor);
    }

    public String getHorario() { return horario.get(); }
    public StringProperty horarioProperty() { return horario; }

    public String getConsultorio() { return consultorio.get(); }
    public StringProperty consultorioProperty() { return consultorio; }

    public String getPaciente() { return paciente.get(); }
    public StringProperty pacienteProperty() { return paciente; }

    public String getTratamiento() { return tratamiento.get(); }
    public StringProperty tratamientoProperty() { return tratamiento; }

    public String getDoctor() { return doctor.get(); }
    public StringProperty doctorProperty() { return doctor; }
}
