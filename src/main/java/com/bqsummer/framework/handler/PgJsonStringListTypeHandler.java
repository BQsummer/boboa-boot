package com.bqsummer.framework.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * List<String> 与 PostgreSQL json 类型的转换器。
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgJsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("json");
        pgObject.setValue(JSON.toJSONString(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toStringList(rs.getObject(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toStringList(rs.getObject(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toStringList(cs.getObject(columnIndex));
    }

    private List<String> toStringList(Object obj) throws SQLException {
        if (obj == null) {
            return null;
        }

        String json;
        if (obj instanceof PGobject) {
            json = ((PGobject) obj).getValue();
        } else {
            json = String.valueOf(obj);
        }

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return JSON.parseArray(json, String.class);
        } catch (JSONException e) {
            throw new SQLException("Failed to parse json list: " + json, e);
        }
    }
}
