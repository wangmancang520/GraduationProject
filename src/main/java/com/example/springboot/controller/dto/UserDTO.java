package com.example.springboot.controller.dto;

import lombok.Data;

/*
* 接受前端登录请求的参数
* */

@Data
public class UserDTO {
    private Integer uId;
    private String uLoginName;
    @lombok.ToString.Exclude
    private String uPassword;
    private String uNickname;
    private Integer uType;
    private String uAvatarPath;
    private String token;
}
