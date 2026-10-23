package com.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Constants;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Classroom;
import com.example.springboot.entity.Message;
import com.example.springboot.entity.User;
import com.example.springboot.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {

    @Autowired
    @org.springframework.context.annotation.Lazy
    ClassroomService classroomService;
    @Autowired
    @org.springframework.context.annotation.Lazy
    UserService userService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    MessageService messageService;

    /**
     * 新增预约
     *
     * @param uid
     * @param cid
     * @param date
     * @param startTime
     * @param endTime
     * @return
     * @throws ParseException
     */
    public Result makeNew(Integer uid, Integer cid, String date, String startTime, String endTime) throws ParseException {

        Classroom target = classroomService.getOne(new QueryWrapper<Classroom>().eq("c_id", cid).last("FOR UPDATE"));
        if (target == null || classroomService.getAdminByCid(cid) == null || userService.getById(uid) == null)
            return Result.error(Constants.CODE_400, "预约教室、管理员或用户不存在");
        Appointment newA = constructor(null, uid, cid, date, startTime, endTime);

        Date now = new Date();
        if (newA.getAStartTime().compareTo(now) <= 0)
            return Result.error(Constants.CODE_500, "预约开始时间不能早于当前时间");
        if (newA.getAStartTime().compareTo(newA.getAEndTime()) >= 0)
            return Result.error(Constants.CODE_500, "预约结束时间不能早于预约开始时间");
        if (validate(newA)) {
            if (!save(newA)) throw new IllegalStateException("预约保存失败");
            User cAdmin = classroomService.getAdminByCid(newA.getCId());
            User user = userService.getById(newA.getUId());
            Classroom classroom = classroomService.getById(newA.getCId());
            Message message = new Message(null, "您的预约申请已提交！",
                    "您在" + classroom.getCName() + "教室，开始于" + newA.getAStartTime() + "的预约申请已提交！" + "我们已发送申请至教室管理员，管理员会尽快处理。如果预约状态长时间没有变化，可以尝试联系教室管理员："
                            + cAdmin.getUName() + " " + cAdmin.getUPhone() + "。祝您学习生活愉快！", null, newA.getUId(), null, null);
            messageService.save(message);

            message.setMReceiverId(cAdmin.getUId());
            message.setMTitle("您收到了新的预约申请！");
            message.setMContent("有用户预约了您所管理的教室（" + classroom.getCName() +
                    "），请及时在”预约管理->新增预约“中处理。如有特殊情况，可以尝试联系用户：" + user.getUName() + " " + user.getUPhone() +
                    "。祝您生活愉快！");
            message.setMId(null);
            messageService.save(message);
            return Result.success();
        } else
            return Result.error(Constants.CODE_500, "预约时间与已有预约冲突");
    }

    /**
     * 将前台传回的字符串信息封装为Appointment类型
     *
     * @return 封装好的appointment对象
     */
    public Appointment constructor(Integer aid, Integer uid, Integer cid, String date, String startTime, String endTime) throws ParseException {

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        try {
            java.time.LocalDate selectedDate = date.length() == 10 ? java.time.LocalDate.parse(date)
                    : java.time.Instant.parse(date).atZone(zone).toLocalDate();
            Appointment existing = aid == null ? null : getById(aid);
            Date start = combineDate(selectedDate, startTime, existing == null ? null : existing.getAStartTime(), zone);
            Date end = combineDate(selectedDate, endTime, existing == null ? null : existing.getAEndTime(), zone);
            return new Appointment(aid, cid, uid, start, end, 1, 1);
        } catch (RuntimeException e) {
            throw new com.example.springboot.exception.ServiceException(Constants.CODE_400, "预约日期或时间格式不正确");
        }
    }

    private Date combineDate(java.time.LocalDate date, String input, Date fallback, java.time.ZoneId zone) {
        java.time.LocalTime time;
        if (input == null || input.isEmpty()) {
            if (fallback == null) throw new IllegalArgumentException("Missing time");
            time = fallback.toInstant().atZone(zone).toLocalTime();
        } else {
            time = java.time.Instant.parse(input).atZone(zone).toLocalTime();
        }
        return Date.from(date.atTime(time).atZone(zone).toInstant());
    }

    /**
     * 修改预约信息
     *
     * @param aid
     * @param uid
     * @param cid
     * @param date
     * @param startTime
     * @param endTime
     * @return Result对象
     */
    public Result edit(Integer aid, Integer uid, Integer cid, User operator, String date, String startTime, String endTime) throws ParseException {
        Classroom target = classroomService.getOne(new QueryWrapper<Classroom>().eq("c_id", cid).last("FOR UPDATE"));
        if (target == null || classroomService.getAdminByCid(cid) == null || userService.getById(uid) == null)
            return Result.error(Constants.CODE_400, "预约教室、管理员或用户不存在");
        Appointment newA = constructor(aid, uid, cid, date, startTime, endTime);
        Appointment oldA = getById(newA.getAId());
        boolean flag = true; //传入的预约信息是否和数据库中的预约信息一致
        boolean userChanged = !newA.getUId().equals(oldA.getUId()); //预约用户是否发生改变
        if (!newA.getUId().equals(oldA.getUId()))
            flag = false;
        if (!newA.getCId().equals(oldA.getCId()))
            flag = false;
        if (newA.getAStartTime().compareTo(oldA.getAStartTime()) != 0)
            flag = false;
        if (newA.getAEndTime().compareTo(oldA.getAEndTime()) != 0)
            flag = false;

        if (flag) {
            return new Result(Constants.CODE_403, "您并未进行任何修改", null);
        }
        Date now = new Date();
        if (newA.getAStartTime().compareTo(now) <= 0)
            return Result.error(Constants.CODE_500, "预约开始时间不能早于当前时间");
        if (newA.getAStartTime().compareTo(newA.getAEndTime()) >= 0)
            return Result.error(Constants.CODE_500, "预约结束时间不能早于预约开始时间");
        if (validate(newA)) {
            newA.setAApprovalStatus(1);
            saveOrUpdate(newA);
        } else
            return Result.error(Constants.CODE_500, "预约时间与已有预约冲突");

        Message message = new Message();
        String content = "";

        if (userChanged) {
            message.setMReceiverId(newA.getUId());
            message.setMTitle("您有新的预约！");
            content = "管理员给您分配了一条新的预约，预约教室为：" + classroomService.getById(newA.getCId()).getCName() +
            "，开始时间为：" + newA.getAStartTime() + "。您可以在”我的预约“菜单中查看详细信息，祝您学习生活愉快！";
            message.setMContent(content);
            messageService.save(message);
            message.setMId(null);
            message.setMReceiverId(oldA.getUId());
            message.setMTitle("您有一条预约被取消。");
            content = "很抱歉，您在" + classroomService.getById(oldA.getCId()).getCName() +
                    "教室，开始于" + oldA.getAStartTime() + "的预约已经被管理员取消。操作人：" + operator.getUName() + " " + operator.getUPhone() +
                    " 对此造成的不便，我们深感抱歉。";
            message.setMContent(content);
            messageService.save(message);
        } else {
            message.setMReceiverId(uid);
            User cAdmin = userService.getById(classroomService.getById(newA.getCId()).getCAdminId());
            if (operator.getUId().equals(uid)) {
                message.setMTitle("您修改了预约请求。");
                message.setMContent("您已成功修改您在" + classroomService.getById(oldA.getCId()).getCName() +
                        "，开始于" + oldA.getAStartTime() + " 的预约请求。教室管理员会尽快处理您的预约请求，如果预约状态长时间没有更新，请尝试联系管理员：" +
                        cAdmin.getUName() + " " + cAdmin.getUPhone() + " 祝您学习生活愉快！");
                messageService.save(message);
                message.setMId(null);
            } else {
                message.setMTitle("您的预约已被修改。");
                message.setMContent("管理员修改了您在" + classroomService.getById(oldA.getCId()).getCName() +
                        "，开始于" + oldA.getAStartTime() + " 的预约请求。您可以在“我的预约”菜单中查看预约详情。如有疑问，请联系管理员："
                        + operator.getUName() + " " + operator.getUPhone() + " 祝您学习生活愉快！");
                messageService.save(message);
            }
        }
        return Result.success();
    }

    /**
     * 检验预约是否跟已有预约时间冲突
     * 若传入的appointment含有aid，则在校验时会跳过数据库中aid字段相同的预约（更新）
     *
     * @param appointment
     * @return 布尔变量，true为合法，false为非法
     */
    public boolean validate(Appointment appointment) {

        //查询出目前所有本教室的预约
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("c_id", appointment.getCId());

//        仅查询待审核和已通过的预约
        queryWrapper.between("a_approval_status", 1, 2);

//        仅查询尚未过期的预约
        queryWrapper.between("a_status", 1, 2);
        List<Appointment> appointmentList = list(queryWrapper);

        //如果当前教室无任何预约信息，则跳过验证直接成功
        if (appointmentList.toArray().length == 0)
            return true;

        //若查询出当前教室存在预约信息，则逐条验证时间是否冲突
        int i1, i2, i3, i4;
        for (Appointment oldA : appointmentList) {
            i1 = appointment.getAStartTime().compareTo(oldA.getAStartTime());
            i2 = appointment.getAStartTime().compareTo(oldA.getAEndTime());
            i3 = appointment.getAEndTime().compareTo(oldA.getAStartTime());
            i4 = appointment.getAEndTime().compareTo(oldA.getAEndTime());

            if (i3 <= 0 || i2 >= 0 || (appointment.getAId() != null && appointment.getAId().equals(oldA.getAId()))) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param u_id
     * @param c_admin_id
     * @param a_approval_status
     * @param u_name
     * @param c_name
     * @param a_date
     * @param a_start_time
     * @param a_end_time
     * @return
     */
    public Result findPage(Integer pageNum,
                           Integer pageSize,
                           Integer u_id,
                           Integer c_admin_id,
                           Integer a_approval_status,
                           String u_name,
                           String c_name,
                           String a_date,
                           String a_start_time,
                           String a_end_time) {




        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        Date date = null;
        Date startTime = null;
        Date endTime = null;

        try {

            if (!a_date.equals("")) {
                date = dateFormat.parse(a_date);
            }

            if (!a_start_time.equals("")) {
                startTime = timeFormat.parse(a_start_time);
            }

            if (!a_end_time.equals("")) {
                endTime = timeFormat.parse(a_end_time);
            }

        } catch (ParseException e) {
            return Result.error(Constants.CODE_400, "筛选日期或时间格式不正确");
        }




/**
 * 接下来的if-else代码块将根据传入的日期、开始时间和结束时间拼装成合理、便于数据库查询的带日期的开始时间与结束时间
 */
//        如果日期不为空
        if (date != null) {
//            开始时间不为空
            if (startTime != null) {
//                则把开始时间的日期设为传入的日期
                setFullDate(date, startTime);
//                如果结束时间也不为空
                if (endTime != null) {
//                    就把结束时间的日期也设为传入的日期
                    setFullDate(date, endTime);
//                    如果结束时间为空，
                } else {
//                    那就设为跟开始时间一样
                    endTime = startTime;
                }
//                如果开始时间为空
            } else {
//                且结束时间不为空
                if (endTime != null) {
//                    那就把结束时间的日期设为传入的日期
                    setFullDate(date, endTime);
//                    同时把开始时间设为跟结束时间相同
                    startTime = endTime;
//                    如果开始时间和结束时间都为空，则直接将日期赋予
                } else {
                    startTime = new Date();
                    endTime = new Date();

                    startTime.setTime(date.getTime());
                    endTime.setTime(date.getTime());
//                        为了避免查不到预约，我们把赋予了日期了的时间调整至覆盖全天的预约
                    startTime.setHours(7);
                    endTime.setHours(23);
                }
            }
//            如果传入的日期为空
        } else {
//            但传入的开始时间不为空
            if (startTime != null) {
//                则将当前时间的日期赋给开始时间
                setFullDate(new Date(), startTime);
//                如果结束时间不为空
                if (endTime != null) {
//                    就将当前时间的日期赋给结束时间
                    setFullDate(new Date(), endTime);
//                    如果结束时间为空
                } else {
//                    就令结束时间跟开始时间相同
                    endTime = startTime;
                }
//                如果开始时间为空
            } else {
//                且结束时间不为空
                if (endTime != null) {
//                    则将当前时间的日期赋给结束时间
                    setFullDate(new Date(), endTime);
//                    并令开始时间等于结束时间
                    startTime = endTime;
                }
            }
        }




        IPage<Appointment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Appointment> appointmentQueryWrapper = new QueryWrapper<>();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        QueryWrapper<Classroom> classroomQueryWrapper = new QueryWrapper<>();

        classroomQueryWrapper.select("c_id");
        userQueryWrapper.select("u_id");

        List<User> userList;
        List<Integer> u_ids = new ArrayList<>();
        List<Classroom> classroomList;
        List<Integer> c_ids = new ArrayList<>();
//        如果传入了开始时间和结束时间，则加入筛选条件
        if (startTime != null && endTime != null) {
            appointmentQueryWrapper.le("a_start_time", endTime).ge("a_end_time", startTime);
        }

//        如果传入了用户id，则说明是用户在调用此接口，结果仅返回此用户创建的预约
        if (u_id != null)
            appointmentQueryWrapper.eq("u_id", u_id);

//        如果传入了教室管理员id，则说明是教室管理员在调用此接口，结果仅返回预约教室为管理员所管理的教室的预约

        if (c_admin_id != null) {
            classroomQueryWrapper.eq("c_admin_id", c_admin_id);
            classroomList = classroomService.list(classroomQueryWrapper);
            if (classroomList.size() == 0)
                return Result.error(Constants.CODE_500, "您没有正在管理的教室，请联系系统管理员");

            c_ids = new ArrayList<>();
            for (Classroom classroom : classroomList) {
                c_ids.add(classroom.getCId());
            }

            appointmentQueryWrapper.in("c_id", c_ids);
        }

//        如果传入了审核状态，那么仅返回符合条件的预约
        if (a_approval_status != null) {
            appointmentQueryWrapper.eq("a_approval_status", a_approval_status);
        }

        if (u_name.length() > 0) {

            //在用户表中读出相关用户的u_id
            userQueryWrapper.like("u_name", u_name);
            userList = userService.list(userQueryWrapper);

            //如果查询不到则返回错误
            if (userList.size() == 0)
                return Result.error(Constants.CODE_500, "结果中不存在姓名类似“" + u_name + "”的用户");

            //查询到u_id后将查询到的u_id放入u_ids中
            u_ids = new ArrayList<>();
            for (User user : userList) {
                u_ids.add(user.getUId());
            }

            //使用QueryWapper.in查询所有传入用户相关的预约信息
            appointmentQueryWrapper.in("u_id", u_ids);
        }

        //教室查询逻辑跟用户查询类似
        if (c_name.length() > 0) {
            classroomQueryWrapper.like("c_name", c_name);
            classroomList = classroomService.list(classroomQueryWrapper);

            if (classroomList.size() == 0)
                return Result.error(Constants.CODE_500, "结果中不存在名称类似“" + c_name + "”的教室");

            c_ids = new ArrayList<>();
            for (Classroom classroom : classroomList) {
                c_ids.add(classroom.getCId());
            }

            appointmentQueryWrapper.in("c_id", c_ids);
        }

        //全部查询结束后，结果按照预约开始时间排序
        appointmentQueryWrapper.orderByDesc("a_create_time");
        IPage<Appointment> result = page(page, appointmentQueryWrapper);
        if (result.getRecords().isEmpty()) return Result.success(result);
        java.util.Set<Integer> roomIds = result.getRecords().stream().map(Appointment::getCId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Integer> userIds = result.getRecords().stream().map(Appointment::getUId).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Integer, Classroom> rooms = classroomService.listByIds(roomIds).stream()
                .collect(java.util.stream.Collectors.toMap(Classroom::getCId, room -> room));
        java.util.Map<Integer, User> users = userService.listByIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getUId, user -> user));
        return Result.success(result.convert(appointment -> {
            com.example.springboot.controller.dto.AppointmentView view = new com.example.springboot.controller.dto.AppointmentView();
            cn.hutool.core.bean.BeanUtil.copyProperties(appointment, view);
            Classroom room = rooms.get(appointment.getCId());
            User user = users.get(appointment.getUId());
            if (room != null) { view.setCName(room.getCName()); view.setCAdminId(room.getCAdminId()); }
            if (user != null) { view.setUName(user.getUName()); view.setUStuNum(user.getUStuNum()); }
            return view;
        }));
    }

    public void setFullDate(Date originalDate, Date targetDate) {
        targetDate.setYear(originalDate.getYear());
        targetDate.setMonth(originalDate.getMonth());
        targetDate.setDate(originalDate.getDate());
    }

    public Date setFullTime(Date originalDate, Date targetDate) {
        targetDate.setHours(originalDate.getHours());
        targetDate.setMinutes(originalDate.getMinutes());
        targetDate.setSeconds(originalDate.getSeconds());
        return targetDate;
    }


    /**
     * 获取近几天的教室利用率
     * @param days 天数
     * @return 占用百分比
     */
    public float getOccupationRatio(int days) {
        if (days <= 0 || classroomService.count() == 0) return 0;
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        double occupied = 0;
        for (int day = 0; day < days; day++) {
            long start = today.plusDays(day).atTime(7, 0).atZone(zone).toInstant().toEpochMilli();
            long end = today.plusDays(day).atTime(23, 0).atZone(zone).toInstant().toEpochMilli();
            List<Appointment> appointments = list(new QueryWrapper<Appointment>().eq("a_approval_status", 2)
                    .lt("a_start_time", new Date(end)).gt("a_end_time", new Date(start)));
            for (Appointment appointment : appointments)
                occupied += Math.max(0, Math.min(end, appointment.getAEndTime().getTime())
                        - Math.max(start, appointment.getAStartTime().getTime()));
        }
        return (float) Math.min(100, occupied * 100 / (days * 16.0 * 3600000 * classroomService.count()));
    }
}
