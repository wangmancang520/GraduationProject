package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Integer cId;
    private String cName;
    private Integer cVolume;
    private String cBuilding;
    private String cFloor;
    private String cAddress;
    private Integer cAdminId;
}
