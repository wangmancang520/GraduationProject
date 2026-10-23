package com.example.springboot.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.entity.Appointment;
import com.example.springboot.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;
import java.util.List;

@Configuration
@EnableScheduling
public class ScheduleTaskConfig {
    @Autowired
    AppointmentService appointmentService;

    @Scheduled(cron = "0 */1 * * * ?")
    private void configureTasks() {
        System.out.println("开始了定时任务：");
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("a_status", 1, 3);
        List<Appointment> appointmentList = appointmentService.list(queryWrapper);
        for (Appointment appointment: appointmentList) {
            if (appointment.getAApprovalStatus() == 3) {
                System.out.println("检测到aid为" + appointment.getAId() + "的预约已被拒绝，将状态更新为已失效（4）");
                appointment.setAStatus(4);
                appointmentService.saveOrUpdate(appointment);
            }
        }
        queryWrapper = new QueryWrapper<>();
        queryWrapper.between("a_status", 1, 2);
        appointmentList = appointmentService.list(queryWrapper);
        Date now = new Date();
        for(Appointment appointment: appointmentList) {
            if (appointment.getAEndTime().compareTo(now) < 0) {
                appointment.setAStatus(3);
                System.out.println("检测到aid为" + appointment.getAId() + "的预约已经过期，将状态更新为已过期（3）");
                appointmentService.saveOrUpdate(appointment);
            } else if (appointment.getAStatus() == 1 && appointment.getAStartTime().compareTo(now) < 0) {
                appointment.setAStatus(2);
                System.out.println("检测到aid为" + appointment.getAId() + "的预约已过开始时间，将状态更新为已开始（2）");
                appointmentService.saveOrUpdate(appointment);
            }
        }
    }
}
