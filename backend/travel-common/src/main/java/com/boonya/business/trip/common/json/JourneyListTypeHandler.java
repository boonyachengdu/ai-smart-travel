package com.boonya.business.trip.common.json;

import com.boonya.business.trip.common.entity.Journey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 行程列表类型处理器（PostgreSQL JSONB 格式存储）
 */
public class JourneyListTypeHandler extends BaseTypeHandler<List<Journey>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 注册 Java 8 时间模块
    static {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 自定义 LocalDateTime 序列化格式（可选）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        objectMapper.registerModule(javaTimeModule);
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, List<Journey> journeys, JdbcType jdbcType) throws SQLException {
        try {
            if (journeys == null) {
                ps.setNull(i, jdbcType.TYPE_CODE);
            } else {
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(objectMapper.writeValueAsString(journeys));
                ps.setObject(i, pgObject);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting journey list to JSON", e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Journey> parameter, JdbcType jdbcType) throws SQLException {
        setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public List<Journey> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<Journey> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<Journey> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<Journey> parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Journey.class));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing journey list from JSON", e);
        }
    }
}
