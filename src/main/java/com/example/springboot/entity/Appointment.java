package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Integer aId;
    private Integer cId;
    private Integer uId;
    private Date aStartTime;
    private Date aEndTime;
    private Integer aStatus;
    private Integer aApprovalStatus;
}
