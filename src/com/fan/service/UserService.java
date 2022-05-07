package com.fan.service;

import com.fan.spring.*;

@Component("userService")
//@Scope("singleton")
@Scope("prototype")
public class UserService implements BeanNameAware, InitializingBean {

    @Autowired
    private OrderService orderService;

    private String beanName;

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {

    }

    public void testOrderService() {
        System.out.println("OrderService: " + orderService);
    }
}
