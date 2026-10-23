package com.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Classroom;
import com.example.springboot.entity.User;
import com.example.springboot.mapper.ClassroomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClassroomService extends ServiceImpl<ClassroomMapper, Classroom> {

    @Autowired
    @org.springframework.context.annotation.Lazy
    UserService userService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    AppointmentService appointmentService;
    public boolean saveClassroom(Classroom classroom) {
        return saveOrUpdate(classroom);
    }

    /**
     * 通过教室id查询管理员信息
     * @param c_id
     * @return
     */
    public User getAdminByCid(Integer c_id) {
        return userService.getById(getById(c_id).getCAdminId());
    }

    public float getActiveRatio() {
        QueryWrapper<Appointment> appointmentQueryWrapper = new QueryWrapper<>();
        appointmentQueryWrapper.between("a_status", 1, 2);
        List<Appointment> appointmentList = appointmentService.list(appointmentQueryWrapper);
        long active = appointmentList.stream().map(Appointment::getCId).distinct().count();
        long total = count();
        return total == 0 ? 0 : (int) (active * 100 / total);
    }
}
