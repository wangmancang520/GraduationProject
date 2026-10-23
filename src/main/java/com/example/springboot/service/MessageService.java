package com.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Message;
import com.example.springboot.mapper.MessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    public Result readAll(Integer u_id) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("m_receiver_id", u_id).eq("m_status", 1);
        List<Message> unreadMessages = list(queryWrapper);
        for (Message message: unreadMessages) {
            message.setMStatus(2);
            saveOrUpdate(message);
        }
        return Result.success();
    }
}
