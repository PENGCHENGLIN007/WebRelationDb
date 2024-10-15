package com.pcl.simpleweb;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pcl.simpleweb.mapper")
public class SimpleWebApplication {

    public static void main(String[] args) {
        ClassPool.getDefault().appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        SpringApplication.run(SimpleWebApplication.class, args);
    }

}
