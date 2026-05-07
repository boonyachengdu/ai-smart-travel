-- ================================================
-- RAG升级 Phase 1: 全文检索支持
-- 为 policy_documents 表添加 tsvector 列和 GIN 索引
-- ================================================

-- 1. 添加 tsvector 列（存储分词后的全文索引）
ALTER TABLE policy_documents ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- 2. 创建 GIN 索引（加速全文检索）
CREATE INDEX IF NOT EXISTS idx_policy_content_tsv ON policy_documents USING GIN(content_tsv);

-- 3. 创建触发器函数：自动更新 tsvector 列
CREATE OR REPLACE FUNCTION policy_documents_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.content_tsv := to_tsvector('simple', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4. 创建触发器（INSERT/UPDATE时自动刷新tsvector）
DROP TRIGGER IF EXISTS trg_policy_tsv ON policy_documents;
CREATE TRIGGER trg_policy_tsv
    BEFORE INSERT OR UPDATE OF content ON policy_documents
    FOR EACH ROW
    EXECUTE FUNCTION policy_documents_tsv_trigger();

-- 5. 对已有数据回填 tsvector
UPDATE policy_documents SET content_tsv = to_tsvector('simple', COALESCE(content, '')) WHERE content_tsv IS NULL;
