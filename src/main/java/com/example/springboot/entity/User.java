package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer uId;
    private String uName;
    private String uNickname;
    private String uLoginName;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @lombok.ToString.Exclude
    private String uPassword;
    private String uPhone;
    private String uStuNum;
    private Integer uType;
    private String uEmail;
    private String uAddress;
    private String uAvatarPath;
}
