package com.example.springboot.service;

import com.example.springboot.common.Constants;
import com.example.springboot.entity.*;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class AccessService {
    @Autowired private AppointmentService appointments;
    @Autowired private ClassroomService classrooms;
    @Autowired private MessageService messages;

    public User current() {
        User user = TokenUtils.getCurrentUser();
        if (user == null) throw new ServiceException(Constants.CODE_401, "请先登录");
        return user;
    }
    public boolean manager() { return Integer.valueOf(4).equals(current().getUType()); }
    public void requireManager() { if (!manager()) denied(); }
    public void requireSelf(Integer uid) {
        if (!manager() && !Objects.equals(current().getUId(), uid)) denied();
    }
    public void requireClassroom(Integer cid) {
        Classroom classroom = classrooms.getById(cid);
        if (classroom == null) throw new ServiceException(Constants.CODE_400, "教室不存在");
        if (!manager() && !(Integer.valueOf(3).equals(current().getUType())
                && Objects.equals(classroom.getCAdminId(), current().getUId()))) denied();
    }
    public void requireAppointment(Integer aid, boolean review) {
        Appointment appointment = appointments.getById(aid);
        if (appointment == null) throw new ServiceException(Constants.CODE_400, "预约不存在");
        if (manager()) return;
        if (!review && Objects.equals(appointment.getUId(), current().getUId())) return;
        requireClassroom(appointment.getCId());
    }
    public void requireMessage(Integer mid) {
        Message message = messages.getById(mid);
        if (message == null) throw new ServiceException(Constants.CODE_400, "消息不存在");
        requireSelf(message.getMReceiverId());
    }
    private void denied() { throw new ServiceException(Constants.CODE_403, "无权执行此操作"); }
}
