package com.fan.service;

import com.fan.spring.*;

@Component
//@Scope("singleton")
@Scope("prototype")
public class UserServiceImpl implements BeanNameAware, InitializingBean, UserService {

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

    @Override
    public void testOrderService() {
        System.out.println("OrderService: " + orderService);
    }
}
