package com.example.springboot.controller.dto;
import com.example.springboot.entity.Appointment;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class AppointmentView extends Appointment {
    private String cName;
    private Integer cAdminId;
    private String uName;
    private String uStuNum;
}
