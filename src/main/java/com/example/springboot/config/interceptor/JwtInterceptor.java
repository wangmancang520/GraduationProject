package com.example.springboot.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.springboot.common.Constants;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("GET".equals(request.getMethod()) && request.getServletPath().matches("/file/[a-f0-9]{32}\\.[A-Za-z0-9]+"))
            return true;
        String token = request.getHeader("token");

        //如果不是映射到方法直接通过
        if(!(handler instanceof HandlerMethod)) {
            return true;
        }

        //执行认证
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "您还未登录，请先登录");
        }

        //获取token中的userId
        String userId;
        try {
            userId = JWT.decode(token).getAudience().get(0);
            Integer.parseInt(userId);
        } catch (RuntimeException e) {
            throw new ServiceException(Constants.CODE_401, "登录凭证失效，请重新登录");
        }

        //根据token中的userId查询数据库
        User user = userService.getById(userId);
        if(user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }

        //用户密码加签验证token
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getUPassword())).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new ServiceException(Constants.CODE_401, "登录凭证失效，请重新登录");
        }
        request.setAttribute("currentUser", user);
        return true;
    }
}
