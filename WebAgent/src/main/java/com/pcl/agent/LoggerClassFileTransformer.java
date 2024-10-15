package com.pcl.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * @ClassName LoggerClassFileTransformer
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/11 16:30
 * @Version F02SP03
 **/
public class LoggerClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {

        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = null;
        if(className.equals("javax/servlet/http/HttpServlet")){
            try {
                //类的路径替 / 换成 .
                className = className.replace('/', '.');
                //获取到 ctClass
                ctClass = classPool.get(className);
                //拿方法
                CtMethod[] declaredMethods = ctClass.getMethods();
                for(CtMethod ctMethod : declaredMethods){
                    int m = ctMethod.getModifiers();
                    //判断方法名称
                    if(ctMethod.getName().equals("service") && m == 4){
                        //方法内部增加一行代码
                        ctMethod.insertBefore("StringBuffer url = req.getRequestURL();if(url.toString().contains(\"login\")){Thread.currentThread().setName(\"login thread\");}");
                        return ctClass.toBytecode();
                    }
                }
            } catch (NotFoundException | CannotCompileException | IOException e) {
                e.printStackTrace();
            }finally {
                if(ctClass != null){
                    ctClass.detach();
                }
            }
        }

        if(className.equals("com/mysql/cj/jdbc/ConnectionImpl")){
            try {
                //类的路径替 / 换成 .
                className = className.replace('/', '.');
                //获取到 ctClass
                ctClass = classPool.get(className);
                //拿方法
                CtMethod[] methods = ctClass.getMethods();
                for(CtMethod ctMethod : methods){
                    //判断方法名称 public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
                    if(ctMethod.getName().equals("prepareStatement") && ctMethod.getParameterTypes().length==3){
                        //方法内部增加一行代码
                        ctMethod.insertBefore("System.out.println(\"sql=====\"+sql);");
                        ctMethod.insertBefore("sql  =  \"/*\"+Thread.currentThread().getName()+\"*/\"+sql;");
                        return ctClass.toBytecode();
                    }
                }
            } catch (NotFoundException | CannotCompileException | IOException e) {
                e.printStackTrace();
            }finally {
                if(ctClass != null){
                    ctClass.detach();
                }
            }
        }


        return classfileBuffer;
    }
}
