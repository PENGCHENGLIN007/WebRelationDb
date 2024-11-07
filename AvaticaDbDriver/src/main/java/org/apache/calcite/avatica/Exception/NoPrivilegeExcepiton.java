package org.apache.calcite.avatica.Exception;

import org.apache.calcite.avatica.Meta;

public class NoPrivilegeExcepiton  extends Exception{
    private static final long serialVersionUID = 1L;
    private final String sql;

    public NoPrivilegeExcepiton(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
