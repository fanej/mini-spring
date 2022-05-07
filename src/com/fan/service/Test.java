package com.fan.service;

import com.fan.spring.MiniApplicationContext;

public class Test {
    public static void main(String[] args) {
        MiniApplicationContext applicationContext = new MiniApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");

        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("userService"));

        System.out.println(applicationContext.getBean("orderService"));
        System.out.println(applicationContext.getBean("orderService"));
        System.out.println(applicationContext.getBean("orderService"));

        userService.testOrderService();
    }
}
