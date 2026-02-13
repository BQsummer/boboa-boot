package com.bqsummer.framework.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Float数组与PostgreSQL vector类型的转换器
 * 用于支持pgvector-rs扩展的向量类型
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@MappedTypes(float[].class)
public class FloatArrayTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) 
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        
        // 将 float[] 转换为 vector 格式字符串: "[0.1,0.2,0.3,...]"
        StringBuilder sb = new StringBuilder("[");
        for (int j = 0; j < parameter.length; j++) {
            if (j > 0) sb.append(",");
            sb.append(parameter[j]);
        }
        sb.append("]");
        String vectorStr = sb.toString();
        pgObject.setValue(vectorStr);
        
        ps.setObject(i, pgObject);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toFloatArray(rs.getObject(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toFloatArray(rs.getObject(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toFloatArray(cs.getObject(columnIndex));
    }

    /**
     * 将数据库返回的对象转换为float数组
     */
    private float[] toFloatArray(Object obj) throws SQLException {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof PGobject) {
            PGobject pgObject = (PGobject) obj;
            String value = pgObject.getValue();
            
            if (value == null || value.isEmpty()) {
                return null;
            }
            
            // 移除前后的方括号并分割
            String trimmed = value.substring(1, value.length() - 1);
            if (trimmed.isEmpty()) {
                return new float[0];
            }
            
            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            
            return result;
        }
        
        throw new SQLException("Unexpected type for vector: " + obj.getClass().getName());
    }
}
