package com.pcl.proxy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @ClassName Client
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/11/7 11:27
 * @Version F02SP03
 **/
public class Client {

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.apache.calcite.avatica.remote.Driver");
        Properties prop = new Properties();
        prop.put("serialization", "protobuf");
        try (Connection conn = DriverManager.getConnection("jdbc:avatica:remote:url=http://localhost:9999", prop)) {
            // 查询数据
            final Statement stmt = conn.createStatement();
            final ResultSet rs = stmt.executeQuery("SELECT * FROM testdb.alldata");
            while (rs.next()){
                System.out.print(rs.getString(1));
                System.out.print(" ");
                System.out.println(rs.getString(2));
            }
        }
    }

}
