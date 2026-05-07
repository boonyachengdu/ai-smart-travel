package com.boonya.business.trip.common.json;

import com.boonya.business.trip.common.models.approval.ApprovalTripItem;
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
 * 审批出行项列表类型处理器（PostgreSQL JSON 格式存储）
 */
public class ApprovalTripItemListTypeHandler extends BaseTypeHandler<List<ApprovalTripItem>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(formatter));
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(formatter));

        objectMapper.registerModule(javaTimeModule);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ApprovalTripItem> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            ps.setObject(i, pgObject);
        } catch (JsonProcessingException e) {
            throw new SQLException("序列化审批出行项列表失败", e);
        }
    }

    @Override
    public List<ApprovalTripItem> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<ApprovalTripItem> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<ApprovalTripItem> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private List<ApprovalTripItem> parseJson(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("反序列化审批出行项列表失败", e);
        }
    }
}
