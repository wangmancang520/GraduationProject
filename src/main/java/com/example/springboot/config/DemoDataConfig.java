package com.example.springboot.config;

import com.example.springboot.entity.*;
import com.example.springboot.service.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;

@Configuration
@Profile("demo")
public class DemoDataConfig {
    @Bean
    public CommandLineRunner demoData(UserService users, ClassroomService classrooms) {
        return args -> {
            if (users.count() != 0) return;
            String[] logins = {"manager@example.com", "teacher@example.com", "student@example.com"};
            int[] roles = {4, 3, 1};
            for (int i = 0; i < logins.length; i++) {
                User user = new User();
                user.setULoginName(logins[i]); user.setUEmail(logins[i]);
                user.setUName(new String[]{"系统管理员", "教室管理员", "演示学生"}[i]);
                user.setUNickname(user.getUName()); user.setUType(roles[i]);
                user.setUPassword("Demo12345"); users.saveUser(user);
            }
            Classroom classroom = new Classroom();
            classroom.setCName("明理楼101"); classroom.setCVolume(60);
            classroom.setCBuilding("明理楼"); classroom.setCFloor("1");
            classroom.setCAdminId(2); classrooms.save(classroom);
        };
    }
}
