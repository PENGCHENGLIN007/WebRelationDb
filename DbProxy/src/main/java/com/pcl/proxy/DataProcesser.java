package com.pcl.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName DataProcesser
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/17 15:14
 * @Version 1.0
 **/
public class DataProcesser implements Runnable {

    private Socket psocket;
    private String dbIp;
    private int dbPort;


    public DataProcesser(Socket socket, String dbIp, int dbPort) {
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

            // 创建线程用于从目标服务器读取数据并转发到客户端
            Thread targetToClient = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = targetInput.read(buffer)) != -1) {
                        System.out.println();
                        System.out.println("响应长度：" + bytesRead);
                        clientOutput.write(buffer, 0, bytesRead);
                        clientOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            targetToClient.start();

            byte[] buffer = new byte[2];
            while (clientInput.read(buffer) != -1) {
                int reqLen = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
                byte[] reqb = new byte[reqLen];
                reqb[0] = buffer[0];
                reqb[1] = buffer[1];
                int tmpLen = 2;
                while (tmpLen < reqLen) {
                    tmpLen += clientInput.read(reqb, tmpLen, reqLen - tmpLen);
                }

                System.out.print("当前请求长度：" + reqLen);
                processRequest(reqb);
                targetOutput.write(reqb);
                targetOutput.flush();
            }
            targetToClient.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                psocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processRequest(byte[] request) {
        try{
            String packetType = PACKET_TYPE_MAP.get(request[4] & 0xFF);
            System.out.print(" packetType:"+packetType);
            if(packetType.equals("Connect")){
                String connInfo = new String(request, 8+42+20, request.length-70);
                System.out.println(" connInfo:"+connInfo);
            }
            if(packetType.equals("Data")){
                String sql = new String(request, 8+42+4+51, request.length-105);
                System.out.println(" sql:"+sql);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private Map<Integer, String> PACKET_TYPE_MAP = new HashMap<Integer, String>() {{
        put(0x01, "Connect");
        put(0x02, "Accept");
        put(0x03, "ACK");
        put(0x04, "Refute");
        put(0x05,"Redirect");
        put(0x06,"Data");
    }};
}
