package com.pcl.simpleweb.controller;

import com.pcl.simpleweb.mapper.UserMapper;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Code that Changed the World
 * Pro said
 * Created by Pro on 2017-12-19.
 */
@RestController
public class LoginController {

    @Autowired
    UserMapper userMapper;

    @RequestMapping(value = "/logintest", method = RequestMethod.GET)
    public String logintest(HttpServletRequest request) {
        System.out.println("Thread-name:"+Thread.currentThread().getName());
        String username = userMapper.getUserName();
        System.out.println(username);
        return "loginabc";
    }

}
