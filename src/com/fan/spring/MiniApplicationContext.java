package com.fan.spring;

import javax.annotation.PostConstruct;
import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiniApplicationContext {

    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public MiniApplicationContext(Class configClass) {
        this.configClass = configClass;

        //扫描bean
        //查看是否有对应的注解
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value();

            String pathWithLocalSeparator = path.replace(".", File.separator);
            path = path.replace(".", "/");
            System.out.println("path: " + path);
            ClassLoader classLoader = MiniApplicationContext.class.getClassLoader();

            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());
            System.out.println("path dictionary: " + file);

            if (file.isDirectory()) {
                File[] files = file.listFiles();

                //扫描bean
                for (File f : files) {
                    String fileName = f.getAbsolutePath();
                    try {
                        //获取class名字
                        String classname = fileName.substring(fileName.indexOf(pathWithLocalSeparator), fileName.indexOf(".class"));
                        classname = classname.replace(File.separator, ".");

                        System.out.println("classname: " + classname);
                        Class clazz = classLoader.loadClass(classname);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            //这是一个Bean
                            System.out.println("component: " + classname);

                            //处理BeanPostProcessor
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)){
                                BeanPostProcessor beanPostProcessor = null;
                                try {
                                    beanPostProcessor = (BeanPostProcessor) clazz.getConstructor().newInstance();
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                                beanPostProcessorList.add(beanPostProcessor);
                                continue;
                            }

                            //获取bean名字
                            Component componentAnnotation = (Component) clazz.getAnnotation(Component.class);
                            String beanName = componentAnnotation.value();
                            if (beanName.equals("")) {
                                beanName = Introspector.decapitalize(clazz.getSimpleName());
                            }

                            //创建BeanDefinition
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(clazz);
                            //
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = (Scope) clazz.getAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinitionMap.put(beanName, beanDefinition);
                        }
                        if (clazz.isAnnotationPresent(Configuration.class)){
                            System.out.println("Configuration 注解处理");
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }

                //创建单例bean

                for (String beanName : beanDefinitionMap.keySet()) {
                    BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
                    String scope = beanDefinition.getScope();
                    if ("singleton".equals(scope)) {
                        Object o = createBean(beanName, beanDefinition);
                        if (o != null) {
                            singletonObjects.put(beanName, o);
                        }
                    }
                }
            }

        }
    }

    /**
     * 创建bean
     *
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private synchronized Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        Object instance = null;
        try {
            instance = clazz.getConstructor().newInstance();

            //设置属性
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Autowired.class)) {
                    f.setAccessible(true);
                    f.set(instance, getBean(f.getName()));
                }
            }

            for(Method m:clazz.getDeclaredMethods()){
                if (m.isAnnotationPresent(PostConstruct.class)){
                    m.invoke(instance);
                }
            }

            //Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            for (BeanPostProcessor beanPostProcessor:beanPostProcessorList){
                instance = beanPostProcessor.postProcessBeforeInitialization(beanName,instance);
            }

            //初始化
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }

            for (BeanPostProcessor beanPostProcessor:beanPostProcessorList){
                instance = beanPostProcessor.postProcessAfterInitialization(beanName,instance);
            }

            //BeanPostProcessor 初始化后 AOP


        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return instance;
    }

    /**
     * 获取bean
     *
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
        //根据beanName找到类、再找到对象
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NullPointerException();
        } else {
            String scope = beanDefinition.getScope();
            if ("singleton".equals(scope)) {
                Object bean = singletonObjects.get(beanName);
                if (bean == null) {
                    bean = createBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, bean);
                }
                return bean;
            } else {
                return createBean(beanName, beanDefinition);
            }
        }
    }
}
