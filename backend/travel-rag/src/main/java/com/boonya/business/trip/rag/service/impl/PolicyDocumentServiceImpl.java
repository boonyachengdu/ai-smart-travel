package com.boonya.business.trip.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.rag.mapper.PolicyDocumentMapper;
import com.boonya.business.trip.rag.service.EmbeddingService;
import com.boonya.business.trip.rag.service.PolicyDocumentService;
import com.boonya.business.trip.rag.service.RagFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDocumentServiceImpl extends ServiceImpl<PolicyDocumentMapper, PolicyDocument> implements PolicyDocumentService {

    public static final int CHUNK_SIZE = 800;
    public static final int OVERLAP = 200;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final RagFileService ragFileService;

    @Value("classpath:prompts/policy-qa.st")
    private Resource policyPromptTemplate;
    private final Parser parser = new AutoDetectParser();

    /**
     * 保存政策文档（带向量）- 支持企业/部门隔离
     */
    @Transactional
    @Override
    public void store(PolicyDocument doc) {
        try {
            log.info("正在保存政策文档，companyId: {}, deptId: {}, scene: {}",
                    doc.getCompanyId(), doc.getDeptId(), doc.getScene());

            // 1. 生成向量
            float[] embedding = embeddingService.generateEmbedding(doc.getContent());
            String embeddingStr = arrayToString(embedding);

            // 2. 插入数据库并返回ID
            String sql = """
            INSERT INTO policy_documents 
            (company_id, dept_id, content, embedding, metadata, scene, enabled, create_time, update_time)
            VALUES (?, ?, ?, ?::vector, ?::jsonb, ?::varchar, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
        """;

            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                    doc.getCompanyId(),
                    doc.getDeptId(),
                    doc.getContent(),
                    embeddingStr,
                    doc.getMetadata(),
                    doc.getScene() != null ? doc.getScene().name() : null,
                    doc.getEnabled() != null ? doc.getEnabled() : true
            );

            doc.setId(id);
            log.info("政策文档保存成功，ID: {}", id);

        } catch (DataAccessException e) {
            log.error("数据库操作失败", e);
            throw new RuntimeException("保存政策文档失败：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("向量生成或序列化失败", e);
            throw new RuntimeException("处理文档内容失败：" + e.getMessage(), e);
        }
    }

    /**
     * 上传政策文档并向量化存储（支持多格式）
     *
     * @param file 政策文档（txt/pdf/word/md 等）
     * @param companyId 企业 ID
     * @param deptId 部门 ID（可选，null 表示公司级通用政策）
     * @param scene 场景类型
     */
    @Override
    public void upload(MultipartFile file, Long companyId, Long deptId, Scene scene) {
        try {
            UserHolder userHolder = UserHolder.get();
            if (userHolder == null) {
                throw new IllegalStateException("用户未登录");
            }

            if (companyId == null) {
                companyId = userHolder.getCompanyId();
            } else if (!userHolder.isSuperAdmin() && !companyId.equals(userHolder.getCompanyId())) {
                log.error("权限拒绝：用户{}尝试为企业{}上传文件", userHolder.getUserId(), companyId);
                throw new SecurityException("无权为该企业管理政策文档");
            }

            if (deptId != null && !userHolder.isSuperAdmin()) {
                log.warn("非管理员用户指定deptId，将被忽略");
                deptId = null;
            }

            ragFileService.uploadOss(scene, companyId, file);

            log.info("1.开始上传文档：{}, companyId: {}, deptId: {}, scene: {}",
                    file.getOriginalFilename(), companyId, deptId, scene);

            String content = readFileContent(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn("文件内容为空，跳过上传");
                throw new IllegalArgumentException("文件内容为空");
            }

            List<String> chunks = splitIntoChunks(content, CHUNK_SIZE, OVERLAP);
            log.info("2.文档分割为 {} 个片段", chunks.size());

            Map<String, Object> baseMetadata = new HashMap<>();
            baseMetadata.put("filename", file.getOriginalFilename());
            baseMetadata.put("file_size", file.getSize());
            baseMetadata.put("mime_type", file.getContentType());
            baseMetadata.put("upload_time", LocalDateTime.now().toString());
            baseMetadata.put("source_type", "policy_document");
            baseMetadata.put("company_id", companyId);
            baseMetadata.put("dept_id", deptId);
            baseMetadata.put("scene", scene != null ? scene.name() : "GENERAL");
            baseMetadata.put("uploaded_by", userHolder.getUserId());
            log.info("3.准备元数据：{}", convertMapToJson(baseMetadata));

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                Map<String, Object> chunkMetadata = new HashMap<>(baseMetadata);
                chunkMetadata.put("chunk_index", i);
                chunkMetadata.put("chunk_total", chunks.size());

                Document doc = new Document(chunk, chunkMetadata);
                documents.add(doc);
            }
            log.info("4.创建 Document 列表成功，共 {} 个 Document", documents.size());

            vectorStore.add(documents);
            log.info("5.向量库添加成功，共 {} 个向量", documents.size());

            // 保存文档元数据记录（不含embedding，向量已由 vectorStore.add 统一写入）
            // 避免双重写入：不再调用 store()，store() 会再次生成 embedding 并 INSERT
            PolicyDocument policyDoc = new PolicyDocument();
            policyDoc.setCompanyId(companyId);
            policyDoc.setDeptId(deptId);
            policyDoc.setScene(scene);
            policyDoc.setContent(content);
            policyDoc.setMetadata(convertMapToJson(baseMetadata));
            policyDoc.setEnabled(true);
            save(policyDoc);  // MyBatis-Plus save，仅保存元数据，不生成向量

            log.info("6.文档上传成功：{} (共 {} 块，文件大小：{} bytes)",
                    file.getOriginalFilename(), chunks.size(), file.getSize());

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档上传失败：{}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档处理失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取文件内容（使用 Tika 支持多格式）
     * 支持：TXT, PDF, Word (.doc/.docx), Markdown, HTML, Excel, PowerPoint 等
     */
    @Override
    public String readFileContent(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        log.info("解析文件：{}", filename);

        try (InputStream inputStream = file.getInputStream()) {
            // 使用 Tika 自动检测文件类型并解析
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 表示不限制长度
            ParseContext context = new ParseContext();

            // 添加文件名到 metadata
            metadata.set(Metadata.TIKA_MIME_FILE, filename);

            parser.parse(inputStream, handler,metadata, context);

            String content = handler.toString();

            if (content.trim().isEmpty()) {
                log.warn("Tika 解析结果为空，尝试直接读取文本");
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            log.info("文件解析成功，内容长度：{} 字符", content.length());
            return content;
        }
    }

    /**
     * 文本分块（重叠滑动窗口）
     *
     * @param text 原始文本
     * @param chunkSize 每块大小（字符数）
     * @param overlap 重叠部分大小
     */
    public List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尽量在句子或段落边界切分
            if (end < text.length()) {
                // 查找最近的换行符
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start += (chunkSize - overlap);
            if (start >= text.length()) {
                break;
            }
        }

        log.info("文本分块完成：总长度={}, 块数={}, 每块平均长度={}",
                text.length(), chunks.size(), text.length() / Math.max(chunks.size(), 1));
        return chunks;
    }

    /**
     * 浮点数组转为字符串（PostgreSQL vector 格式）
     */
    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Map 转为 JSON 字符串
     */
    private String convertMapToJson(Map<String, Object> map) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Map 转 JSON 失败", e);
            return "{}";
        }
    }
}
