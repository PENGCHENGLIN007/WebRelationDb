package com.pcl.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @ClassName DataProcesser
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/17 15:14
 * @Version F02SP03
 **/
public class DataProcesser2 implements Runnable{
    private Socket psocket;
    private String dbIp;
    private int dbPort;


    public DataProcesser2(Socket socket,String dbIp,int dbPort) {
        super();
        this.psocket = socket;
        this.dbIp = dbIp;
        this.dbPort = dbPort;
    }

    @Override
    public void run() {
        try (Socket targetSocket = new Socket(dbIp, dbPort);
                InputStream clientInput = psocket.getInputStream();
                OutputStream clientOutput = psocket.getOutputStream();
                InputStream targetInput = targetSocket.getInputStream();
                OutputStream targetOutput = targetSocket.getOutputStream()) {
            // 创建线程用于从客户端读取数据并转发到目标服务器
            Thread clientToTarget = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = clientInput.read(buffer)) != -1) {
                        targetOutput.write(buffer, 0, bytesRead);
                        targetOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // 创建线程用于从目标服务器读取数据并转发到客户端
            Thread targetToClient = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = targetInput.read(buffer)) != -1) {
                        clientOutput.write(buffer, 0, bytesRead);
                        clientOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            clientToTarget.start();
            targetToClient.start();

            // 等待线程结束
            clientToTarget.join();
            targetToClient.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                psocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
