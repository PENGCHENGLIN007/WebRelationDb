import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        String proxyIp = args[0];
        int proxyPort = Integer.parseInt(args[1]);
        String dbIp = args[2];
        int dbPort = Integer.parseInt(args[3]);
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
                    serverSocket=new ServerSocket(proxyPort,50, InetAddress.getByName(proxyIp));
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
                pool.execute(new DataProcesser2(socket,dbIp,dbPort));
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

class DataProcesser2 implements Runnable{
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
