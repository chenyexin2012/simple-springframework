package com.holmes.spring.mvc.core;

import com.holmes.spring.annotation.Autowired;
import com.holmes.spring.annotation.Controller;
import com.holmes.spring.annotation.RequestMapping;
import com.holmes.spring.annotation.Service;
import com.holmes.spring.mvc.controller.UserController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
public class DispatcherServlet extends HttpServlet {

    private List<String> classNames = new LinkedList<>();

    private Map<String, Object> beanMap = new HashMap<>();

    private Map<String, Method> handlerMap = new HashMap<>();


    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        doScanPackage("com.holmes.spring.mvc");

        doInstance();

        doAutowired();

        doUrlMapping();
    }

    private void doScanPackage(String path) {

        // 扫描包路径
        URL url = this.getClass().getClassLoader().getResource("/" + path.replaceAll("\\.", "//"));
        System.out.println("url : " + url);
        File file = new File(url.getFile());

        File[] fileList = file.listFiles();

        for (File f : fileList) {
            if (f.isDirectory()) {
                doScanPackage(path + "." + f.getName());
            } else {
                classNames.add(path + "." + f.getName());
            }
        }
    }

    private void doInstance() {

        // 创建对象
        for (String className : classNames) {
            // 获取类的全路径名
            className = className.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                // 查找是否有@Controller注解
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object instance = clazz.newInstance();
                    // 查找是否有@RequestMapping注解
                    if (clazz.isAnnotationPresent(RequestMapping.class)) {
                        // 获取RequestMapping注解值
                        RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
                        String key = mapping.value();
                        // 将控制类对象加入map中
                        beanMap.put(key, instance);
                    }
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    // 查找是否有@Service注解
                    Object instance = clazz.newInstance();
                    Service service = clazz.getAnnotation(Service.class);
                    String key = service.value();
                    // value为空，则直接使用类名，并将首字母转小写
                    if ("".equals(key)) {
                        key = clazz.getSimpleName();
                        key = (new StringBuilder()).append(Character.toLowerCase(key.charAt(0))).append(key.substring(1)).toString();
                    }
                    // 将Service类对象加入map中
                    beanMap.put(key, instance);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private void doAutowired() {
        try {
            for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
                Object instance = entry.getValue();
                Class<?> clazz = instance.getClass();
                // 判断是否是控制层
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.isAnnotationPresent(Autowired.class)) {
                            Autowired autowired = field.getAnnotation(Autowired.class);
                            String value = autowired.value();
                            if ("".equals(value)) {
                                value = field.getName();
                            }
                            // 获取依赖对象
                            Object dependencyObj = beanMap.get(value);
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            // 将依赖对象注入
                            field.set(instance, dependencyObj);
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doUrlMapping() {

        for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            // 判断是否是控制层
            if (clazz.isAnnotationPresent(Controller.class)) {
                String classPath = "";
                // 获取类的mapping路径
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
                    classPath = mapping.value();
                }
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    // 获取方法的mapping路径
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                        String methodPath = mapping.value();
                        String path = classPath + methodPath;
                        // 将路径方法映射加入map
                        handlerMap.put(path, method);
                    }
                }
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 获取请求路径
        String uri = req.getRequestURI();
        System.out.println("uri : " + uri);
        String contextPath = "/test";
        String path = uri.replace(contextPath, "");
        System.out.println("path : " + path);

        Method method = handlerMap.get(path);
        if (null != method) {
            System.out.println(path.split("/")[1]);
            UserController instance = (UserController) beanMap.get("/" + path.split("/")[1]);
            System.out.println(instance);
            try {
                Object[] args = hand(req, resp, method);
                System.out.println(args.length);
                System.out.println(method.getName());
                Object result = method.invoke(instance, args);
                resp.getWriter().write((String) result);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private Object[] hand(HttpServletRequest req, HttpServletResponse resp, Method method) {

        Class<?>[] paramClasses = method.getParameterTypes();
        Object[] args = new Object[paramClasses.length];

        int arg_index = 0;
        int index = 0;
        for (Class<?> paramClass : paramClasses) {
            if (ServletRequest.class.isAssignableFrom(paramClass)) {
                args[arg_index++] = req;
            }
            if (ServletResponse.class.isAssignableFrom(paramClass)) {
                args[arg_index++] = resp;
            }

            Annotation[] paramAns = method.getParameterAnnotations()[index];
            for (Annotation paramAn : paramAns) {
                if (RequestMapping.class.isAssignableFrom(paramAn.getClass())) {
                    RequestMapping mapping = (RequestMapping) paramAn;
                    args[arg_index++] = req.getParameter(mapping.value());
                }
            }
            index++;
        }
        return args;
    }
}
