package com.pcl.proxy;

import java.sql.SQLException;
import java.util.Properties;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.metrics.noop.NoopMetricsSystem;

/**
 * @ClassName CustomJdbcMeta
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/11/4 18:33
 * @Version F02SP03
 **/
public class CustomJdbcMeta extends JdbcMeta {

    public CustomJdbcMeta(String url) throws SQLException {
        super(url);
    }

    public CustomJdbcMeta(String url, Properties info) throws SQLException {
        super(url,info);
    }


}
