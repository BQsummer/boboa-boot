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
import java.util.Map;

/**
 * Map<String, Object> 与 PostgreSQL json 类型的转换器。
 */
@MappedTypes(Map.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgJsonObjectTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("json");
        pgObject.setValue(JSON.toJSONString(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toMap(rs.getObject(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toMap(rs.getObject(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toMap(cs.getObject(columnIndex));
    }

    private Map<String, Object> toMap(Object obj) throws SQLException {
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
            return JSON.parseObject(json);
        } catch (JSONException e) {
            throw new SQLException("Failed to parse json object: " + json, e);
        }
    }
}
