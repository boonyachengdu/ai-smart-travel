//package com.boonya.business.trip.rag.service;
//
//import com.boonya.business.trip.common.prompt.Scene;
//import com.boonya.business.trip.common.entity.PolicyDocument;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * RagService 单元测试
// *
// * @description: 测试 RAG 服务的核心功能，包括向量检索、问答增强和差标校验
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("RAG 服务测试")
//class RagServiceTest {
//
//    @Mock
//    private JdbcTemplate jdbcTemplate;
//
//    @Mock
//    private EmbeddingService embeddingService;
//
//    @Mock
//    private ChatClient chatClient;
//
//    @InjectMocks
//    private RagService ragService;
//
//    private PolicyDocument policyDocument1;
//    private PolicyDocument policyDocument2;
//    private float[] mockEmbedding;
//
//    @BeforeEach
//    void setUp() {
//        // 准备模拟数据
//        mockEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
//
//        policyDocument1 = new PolicyDocument();
//        policyDocument1.setId(1L);
//        policyDocument1.setCompanyId(1L);
//        policyDocument1.setDeptId(null);
//        policyDocument1.setScene(Scene.GENERAL);
//        policyDocument1.setContent("机票标准：经济舱单程不超过 1500 元，一线城市不超过 2000 元。");
//        policyDocument1.setEnabled(true);
//
//        policyDocument2 = new PolicyDocument();
//        policyDocument2.setId(2L);
//        policyDocument2.setCompanyId(1L);
//        policyDocument2.setDeptId(null);
//        policyDocument2.setScene(Scene.FLIGHT);
//        policyDocument2.setContent("酒店标准：标准间/大床房不超过 500 元/晚。");
//        policyDocument2.setEnabled(true);
//    }
//
//    @Test
//    @DisplayName("测试 RAG 增强查询 - 成功检索到政策")
//    void testEnhanceWithRag_Success() {
//        // Arrange
//        String query = "机票报销标准是多少？";
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);
//
//        List<PolicyDocument> mockResults = List.of(policyDocument1, policyDocument2);
//
//        String sql = """
//            SELECT id, company_id, dept_id, content, metadata, scene, enabled,
//                   1 - (embedding <=> ?::vector) AS similarity
//            FROM policy_documents
//            WHERE company_id = ?
//              AND enabled = TRUE
//            ORDER BY similarity DESC
//            LIMIT ?
//        """;
//
//        when(jdbcTemplate.query(eq(sql), any(), any(Object[].class))).thenReturn(mockResults);
//
//        // Act
//        String result = ragService.enhanceWithRag(query, companyId);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.contains("根据企业差旅政策"));
//        assertTrue(result.contains("机票标准"));
//        assertTrue(result.contains("酒店标准"));
//
//        verify(embeddingService).generateEmbedding(query);
//        verify(jdbcTemplate).query(eq(sql), any(), any(Object[].class));
//    }
//
//    @Test
//    @DisplayName("测试 RAG 增强查询 - 未检索到政策")
//    void testEnhanceWithRag_NoResults() {
//        // Arrange
//        String query = "不存在的政策问题";
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class))).thenReturn(new ArrayList<>());
//
//        // Act
//        String result = ragService.enhanceWithRag(query, companyId);
//
//        // Assert
//        assertNull(result);
//        verify(embeddingService).generateEmbedding(query);
//        verify(jdbcTemplate).query(anyString(), any(), any(Object[].class));
//    }
//
//    @Test
//    @DisplayName("测试 RAG 问答 - 有上下文增强")
//    void testGenerateAnswerWithRag_WithContext() {
//        // Arrange
//        String query = "出差住宿标准是多少？";
//        Long companyId = 1L;
//        Long deptId = null;
//
//        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(List.of(policyDocument2));
//
//        String mockAnswer = "根据公司差旅政策，酒店标准间/大床房不超过 500 元/晚。";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(query)).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(mockAnswer);
//
//        // Act
//        String result = ragService.generateAnswerWithRag(query, companyId, deptId);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(mockAnswer, result);
//        verify(embeddingService).generateEmbedding(query);
//        verify(jdbcTemplate).query(anyString(), any(), any(Object[].class));
//        verify(chatClient).prompt(query);
//    }
//
//    @Test
//    @DisplayName("测试 RAG 问答 - 无上下文时使用基础模型")
//    void testGenerateAnswerWithRag_NoContext() {
//        // Arrange
//        String query = "未知问题";
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class))).thenReturn(new ArrayList<>());
//
//        String baseAnswer = "抱歉，我没有相关信息。";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(query)).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(baseAnswer);
//
//        // Act
//        String result = ragService.generateAnswerWithRag(query, companyId, null);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(baseAnswer, result);
//        verify(chatClient).prompt(query);
//    }
//
//    @Test
//    @DisplayName("测试 RAG 问答 - 异常降级处理")
//    void testGenerateAnswerWithRag_ExceptionHandling() {
//        // Arrange
//        String query = "测试问题";
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(query)).thenThrow(new RuntimeException("嵌入生成失败"));
//
//        String fallbackAnswer = "基础模型回答";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(query)).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(fallbackAnswer);
//
//        // Act
//        String result = ragService.generateAnswerWithRag(query, companyId, null);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.contains("基础回答"));
//        verify(chatClient).prompt(query);
//    }
//
//    @Test
//    @DisplayName("测试差标校验问答 - 有部门政策")
//    void testQueryWithStandardCheck_WithDeptPolicy() {
//        // Arrange
//        String question = "北京出差住宿能报多少钱？";
//        Long companyId = 1L;
//        Long deptId = 10L;
//
//        String deptPolicy = "技术部员工北京地区住宿标准为 600 元/晚。";
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(companyId), eq(deptId)))
//                .thenReturn(deptPolicy);
//
//        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(List.of(policyDocument2));
//
//        String mockAnswer = "技术部员工在北京出差住宿标准为 600 元/晚。";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(mockAnswer);
//
//        // Act
//        String result = ragService.queryWithStandardCheck(question, companyId, deptId);
//
//        // Assert
//        assertNotNull(result);
//        verify(jdbcTemplate).queryForObject(anyString(), eq(String.class), eq(companyId), eq(deptId));
//    }
//
//    @Test
//    @DisplayName("测试差标校验问答 - 无部门政策使用公司政策")
//    void testQueryWithStandardCheck_NoDeptPolicy() {
//        // Arrange
//        String question = "一般城市住宿标准？";
//        Long companyId = 1L;
//        Long deptId = null;
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyLong(), isNull()))
//                .thenReturn(null);
//
//        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(List.of(policyDocument2));
//
//        String mockAnswer = "公司通用标准为 500 元/晚。";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(mockAnswer);
//
//        // Act
//        String result = ragService.queryWithStandardCheck(question, companyId, deptId);
//
//        // Assert
//        assertNotNull(result);
//        verify(jdbcTemplate).queryForObject(anyString(), eq(String.class), eq(companyId), isNull());
//    }
//
//    @Test
//    @DisplayName("测试差标校验问答 - 异常降级")
//    void testQueryWithStandardCheck_ExceptionFallback() {
//        // Arrange
//        String question = "测试问题";
//        Long companyId = 1L;
//        Long deptId = 10L;
//
//        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any()))
//                .thenThrow(new RuntimeException("数据库查询失败"));
//
//        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(List.of(policyDocument2));
//
//        String mockAnswer = "使用通用政策回答。";
//
//        // Mock ChatClient 调用链
//        ChatClient.RequestSpec requestSpec = mock(ChatClient.RequestSpec.class);
//        ChatClient.ResponseSpec responseSpec = mock(ChatClient.ResponseSpec.class);
//
//        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
//        when(requestSpec.call()).thenReturn(responseSpec);
//        when(responseSpec.content()).thenReturn(mockAnswer);
//
//        // Act
//        String result = ragService.queryWithStandardCheck(question, companyId, deptId);
//
//        // Assert
//        assertNotNull(result);
//    }
//
//    @Test
//    @DisplayName("测试合规检查 - 简单场景")
//    void testIsCompliant_SimpleCase() {
//        // Arrange
//        String orderType = "HOTEL";
//        Double amount = 400.0;
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(List.of(policyDocument2));
//
//        // Act
//        boolean result = ragService.isCompliant(orderType, amount, companyId);
//
//        // Assert
//        assertTrue(result);
//        verify(embeddingService).generateEmbedding(anyString());
//    }
//
//    @Test
//    @DisplayName("测试合规检查 - 无政策返回 false")
//    void testIsCompliant_NoPolicy() {
//        // Arrange
//        String orderType = "CAR";
//        Double amount = 300.0;
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
//        when(jdbcTemplate.query(anyString(), any(), any(Object[].class)))
//                .thenReturn(new ArrayList<>());
//
//        // Act
//        boolean result = ragService.isCompliant(orderType, amount, companyId);
//
//        // Assert
//        assertFalse(result);
//    }
//
//    @Test
//    @DisplayName("测试合规检查 - 异常处理")
//    void testIsCompliant_Exception() {
//        // Arrange
//        String orderType = "FLIGHT";
//        Double amount = 2000.0;
//        Long companyId = 1L;
//
//        when(embeddingService.generateEmbedding(anyString()))
//                .thenThrow(new RuntimeException("嵌入服务异常"));
//
//        // Act
//        boolean result = ragService.isCompliant(orderType, amount, companyId);
//
//        // Assert
//        assertFalse(result);
//    }
//
//    /**
//     * 辅助方法：浮点数组转字符串（用于验证 SQL 参数）
//     */
//    private String arrayToString(float[] array) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//        for (int i = 0; i < array.length; i++) {
//            if (i > 0) {
//                sb.append(",");
//            }
//            sb.append(String.format("%.6f", array[i]));
//        }
//        sb.append("]");
//        return sb.toString();
//    }
//}
