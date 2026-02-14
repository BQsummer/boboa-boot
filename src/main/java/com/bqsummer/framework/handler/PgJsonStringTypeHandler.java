package com.bqsummer.framework.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * String 与 PostgreSQL json 类型的转换器。
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgJsonStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("json");
        pgObject.setValue(parameter);
        ps.setObject(i, pgObject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toJsonString(rs.getObject(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toJsonString(rs.getObject(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toJsonString(cs.getObject(columnIndex));
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof PGobject) {
            return ((PGobject) obj).getValue();
        }
        return String.valueOf(obj);
    }
}
