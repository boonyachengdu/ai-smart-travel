package com.boonya.business.trip.common.json;
import com.boonya.business.trip.common.entity.Passenger;
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
 * 乘客列表类型处理器（PostgreSQL JSON 格式存储）
 */
public class PassengerListTypeHandler extends BaseTypeHandler<List<Passenger>> {

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
    public void setParameter(PreparedStatement ps, int i, List<Passenger> passengers, JdbcType jdbcType) throws SQLException {
        try {
            if (passengers == null) {
                ps.setNull(i, jdbcType.TYPE_CODE);
            } else {
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(objectMapper.writeValueAsString(passengers));
                ps.setObject(i, pgObject);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting passenger list to JSON", e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Passenger> parameter, JdbcType jdbcType) throws SQLException {
        setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public List<Passenger> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<Passenger> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<Passenger> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<Passenger> parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Passenger.class));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing passenger list from JSON", e);
        }
    }
}
