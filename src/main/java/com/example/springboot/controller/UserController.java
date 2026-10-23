package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Constants;
import com.example.springboot.common.Result;
import com.example.springboot.controller.dto.UserDTO;
import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")


public class UserController {
    @Autowired private com.example.springboot.service.AccessService access;

    @Autowired
    private UserService userService;

    @PostMapping("/change-password")
    public Result changePassword(@RequestBody com.example.springboot.controller.dto.PasswordChangeDTO change) {
        access.requireSelf(change.getUid());
        return userService.changePassword(change.getUid(), change.getOldPassword(), change.getNewPassword());
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        String username = userDTO.getULoginName();
        String password = userDTO.getUPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password))
            return Result.error(Constants.CODE_400, "用户名或密码为空！");
        else
            return userService.register(userDTO);
    }

    /**
     * 新增或编辑
     * @param user
     * @return
     */
    @PostMapping
    public boolean save(@RequestBody User user) { //新增或更新
        access.requireSelf(user.getUId());
        if (!access.manager()) {
            User existing = userService.getById(user.getUId());
            user.setUType(existing.getUType());
            user.setULoginName(existing.getULoginName());
            user.setUPassword(null); // Password changes must verify the old password through the dedicated endpoint.
        }
        return userService.saveUser(user);
    }

    /**
     * 查询所有数据
     * @return
     */
    @GetMapping
    public List<User> findAll() {
        access.requireManager();
        List<User> list = userService.list();
        return list;
    }

    @GetMapping("/num")
    public Result getTotalNum() {
        List<User> list = userService.list();
        int total = list.size();
        return Result.success(total);
    }

    /**
     * 按u_id删除
     * @param u_id
     * @return
     */
    @DeleteMapping("/{u_id}")
    public boolean delete(@PathVariable Integer u_id) {
        access.requireManager();
        return userService.removeById(u_id);
    }

    /**
     * 批量删除
     * @param c_ids
     * @return
     */
    @PostMapping("/del/batch")
    public boolean deleteBatch(@RequestBody List<Integer> c_ids) {
        access.requireManager();
        return userService.removeBatchByIds(c_ids);
    }

    /**
     * 分页查询-MyBatis-Plus
     * @param pageNum
     * @param pageSize
     * @param u_name
     * @param u_stu_num
     * @param u_nickname
     * @param u_phone
     * @param u_email
     * @param u_type
     * @param u_login_name
     * @return
     */
    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                                @RequestParam Integer pageSize,
                                @RequestParam(defaultValue = "") Integer u_id,
                                @RequestParam(defaultValue = "") String u_name,
                                @RequestParam(defaultValue = "") String u_stu_num,
                                @RequestParam(defaultValue = "") String u_nickname,
                                @RequestParam(defaultValue = "") String u_phone,
                                @RequestParam(defaultValue = "") String u_email,
                                @RequestParam(defaultValue = "") String u_type,
                                @RequestParam(defaultValue = "") String u_login_name) {
        if (!access.manager() && !Integer.valueOf(3).equals(access.current().getUType()))
            u_id = access.current().getUId();
        if (pageNum < 1 || pageSize < 1 || pageSize > 100)
            return Result.error(Constants.CODE_400, "分页参数不正确");
        IPage<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();


        //模糊查询
        if (!(u_id == null)) queryWrapper.eq("u_id", u_id);
        if (!u_name.equals("")) queryWrapper.like("u_name", u_name);
        if (!u_stu_num.equals("")) queryWrapper.eq("u_stu_num", u_stu_num);
        if (!u_nickname.equals("")) queryWrapper.like("u_nickname", u_nickname);
        if (!u_phone.equals("")) queryWrapper.eq("u_phone", u_phone);
        if (!u_email.equals("")) queryWrapper.eq("u_email", u_email);
        if (!u_type.equals("")) queryWrapper.eq("u_type", u_type);
        if (!u_login_name.equals("")) queryWrapper.eq("u_login_name", u_login_name);
        queryWrapper.orderByDesc("u_create_time");
        return Result.success(userService.page(page, queryWrapper));
    }

    /**
     * 重置用户密码
     * @param u_id
     * @return
     */
    @PostMapping("/reset/pwd/{u_id}")
    public boolean resetPwd(@PathVariable Integer u_id) {
        access.requireManager();
        User user = new User();
        user.setUId(u_id);
        user.setUPassword("12345678");
        return userService.saveUser(user);
    }

    /**
     * 通过id查询单个用户信息
     * @param u_id
     * @return
     */
    @GetMapping("/{u_id}")
    public User getById(@PathVariable Integer u_id) {
        if (!Integer.valueOf(3).equals(access.current().getUType())) access.requireSelf(u_id);
        return userService.getById(u_id);
    }

    /**
     * 通过id查询用户姓名
     * @param u_id
     * @return
     */
    @GetMapping("/name/{u_id}")
    public Result getNameById(@PathVariable Integer u_id) {
        return Result.success(userService.getNameById(u_id));
    }

    /**
     * 通过u_id查询用户电话
     * @param u_id
     * @return
     */
    @GetMapping("/phone/{u_id}")
    public String getPhoneById(@PathVariable Integer u_id) { return userService.getById(u_id).getUPhone(); }

    /**
     * 查询所有教室管理员id信息(u_id == 3)
     * @return
     */
    @GetMapping("/classroom-admin")
    public Result getAllClassroomAdmin() {
        List<User> adminList = userService.list(new QueryWrapper<User>().select("u_id", "u_name").eq("u_type", 3));
        return Result.success(adminList);
    }

    @GetMapping("/active-ratio")
    public Result getActiveRatio() {
        return Result.success(userService.getActiveRatio());
    }
}
