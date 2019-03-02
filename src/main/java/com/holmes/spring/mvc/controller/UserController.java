package com.holmes.spring.mvc.controller;

import com.alibaba.fastjson.JSON;
import com.holmes.spring.annotation.Autowired;
import com.holmes.spring.annotation.Controller;
import com.holmes.spring.annotation.RequestMapping;
import com.holmes.spring.annotation.RequestParam;
import com.holmes.spring.mvc.entity.User;
import com.holmes.spring.mvc.service.UserService;

import java.util.List;

/**
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/getAll")
    public String getUserList() {

        System.out.println(userService);
        List<User> list = userService.getAll();
        System.out.println(JSON.toJSONString(list));
        return JSON.toJSONString(list);
    }

    /**
     * @param id
     * @return
     */
    @RequestMapping(value = "/get")
    public String getUser(@RequestParam("id") Integer id) {

        System.out.println("id:" + id);
        User user = userService.getUser(id);

        return JSON.toJSONString(user);
    }


}
