package com.pcl.proxy;


import java.sql.SQLException;
import java.util.Properties;
import org.apache.calcite.avatica.remote.Driver.Serialization;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.server.HttpServer;

/**
 * @ClassName ${NAME}
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/11/4 18:31
 * @Version 1.0
 **/
public class Main {

    public static void main(String[] args) throws SQLException {
        String url = args[0];
        String user = args[1];
        String password = args[2];
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        System.out.println(url);
        System.out.println(properties);
        final CustomJdbcMeta meta = new CustomJdbcMeta(url, properties);
        final LocalService service = new LocalService(meta);
        final HttpServer server = new HttpServer.Builder<>()
                .withPort(9999)
                .withHandler(service, Serialization.JSON)
                .build();
        server.start();
        new Thread(() -> {
            try {
                server.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}