package com.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.common.Constants;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Message;
import com.example.springboot.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Autowired private com.example.springboot.service.AccessService access;

    @Autowired
    MessageService messageService;

    /**
     * 新建信息
     *
     * @param message
     * @return
     */
    @PostMapping("new")
    public Result newMessage(@RequestBody Message message) {
        access.requireManager();
        if (messageService.save(message))
            return Result.success();
        else
            return Result.error(Constants.CODE_500, "系统错误(MessageController)");
    }

    /**
     * 根据接收人查询信息
     *
     * @param mReceiverId
     * @return
     */
    @GetMapping("get-by-receiver/{mReceiverId}")
    public Result getByReceiverId(@PathVariable Integer mReceiverId) {
        access.requireSelf(mReceiverId);
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("m_receiver_id", mReceiverId);
        queryWrapper.orderByDesc("m_create_time");
        return Result.success(messageService.list(queryWrapper));
    }

    /**
     * 查询当前未读信息数
     *
     * @param mReceiverId
     * @return
     */
    @GetMapping("/get-unread-num/{mReceiverId}")
    public Result getUnreadNum(@PathVariable Integer mReceiverId) {
        access.requireSelf(mReceiverId);
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("m_receiver_id", mReceiverId).eq("m_status", 1);
        queryWrapper.orderByDesc("m_create_time");
        return Result.success(messageService.list(queryWrapper).size());
    }

    @PostMapping("/read/{mId}")
    public Result readMessage(@PathVariable Integer mId) {
        access.requireMessage(mId);
        Message message = new Message();
        message.setMId(mId);
        message.setMStatus(2);
        if (messageService.saveOrUpdate(message))
            return Result.success();
        return Result.error(Constants.CODE_500, "系统错误：MessageController");
    }

    @PostMapping("/read-all/{u_id}")
    public Result readAll(@PathVariable Integer u_id) {
        access.requireSelf(u_id);
        return messageService.readAll(u_id);
    }
}
