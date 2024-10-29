package com.pcl.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @ClassName ProxyMain
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/17 15:10
 * @Version F02SP03
 **/

public class ProxyMain {
    public static void main(String[] args) {
        int proxyPort = Integer.parseInt(args[0]);
        String dbIp = args[1];
        int dbPort = Integer.parseInt(args[2]);
        int corePoolSize = 300;
        int keepAliveTime = 30000;

        ExecutorService pool = new ThreadPoolExecutor(corePoolSize, Integer.MAX_VALUE,
                keepAliveTime, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());

        ServerSocket serverSocket=null;
        try {
            int count = 0;
            while (count<30){
                count++;
                try{
                    serverSocket=new ServerSocket(proxyPort,50, InetAddress.getByName("10.66.38.166"));
                    break;
                }catch (Exception e){
                    try{
                        Thread.sleep(10000);
                    } catch (InterruptedException interruptedException) {
                        e.printStackTrace();
                    }

                }
            }
            //通过死循环开启长连接，开启线程去处理消息
            while(true){
                assert serverSocket != null;
                Socket socket = serverSocket.accept();
                pool.execute(new DataProcesser(socket,dbIp,dbPort));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket!=null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
