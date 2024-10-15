package com.pcl.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * @ClassName Agent
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/11 16:05
 * @Version F02SP03
 **/
public class Agent {
    public static void premain(String arg, Instrumentation instrumentation){

        System.out.println("MyAgent start run ! , arg is " + arg);

        instrumentation.addTransformer(new LoggerClassFileTransformer());

    }

    /**
     * 动态 attach 方式启动，运行此方法
     *
     * @param agentArgs
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("agentmain");
    }

    }
