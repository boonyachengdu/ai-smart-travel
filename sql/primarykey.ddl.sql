-- 连接数据库
\c smart_travel


-- ================================================
-- 序列起始值校准（重要！防止新 ID 与现有数据冲突）
-- ================================================

SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 0));
SELECT setval('company_id_seq', COALESCE((SELECT MAX(id) FROM company), 0));
SELECT setval('departments_id_seq', COALESCE((SELECT MAX(id) FROM departments), 0));
SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 0));
SELECT setval('approvals_id_seq', COALESCE((SELECT MAX(id) FROM approvals), 0));
SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 0));
--TRUNCATE TABLE tickets, journeys, passengers RESTART IDENTITY CASCADE;
SELECT setval('journeys_id_seq', COALESCE((SELECT MAX(id) FROM journeys), 0));
SELECT setval('passengers_id_seq', COALESCE((SELECT MAX(id) FROM passengers), 0));
SELECT setval('tickets_id_seq', COALESCE((SELECT MAX(id) FROM tickets), 0));
SELECT setval('standards_id_seq', COALESCE((SELECT MAX(id) FROM standards), 0));
SELECT setval('prompts_id_seq', COALESCE((SELECT MAX(id) FROM prompts), 0));
SELECT setval('chat_sessions_id_seq', COALESCE((SELECT MAX(id) FROM chat_sessions), 0));
SELECT setval('chat_messages_id_seq', COALESCE((SELECT MAX(id) FROM chat_messages), 0));
SELECT setval('rag_files_id_seq', COALESCE((SELECT MAX(id) FROM rag_files), 0));

-- ================================================
-- 验证修改结果
-- ================================================
SELECT
    table_name,
    column_name,
    is_identity,
    identity_generation
FROM information_schema.columns
WHERE table_name IN (
                     'users', 'company', 'departments', 'employees',
                     'approvals', 'orders', 'journeys', 'passengers',
                     'tickets', 'standards', 'prompts', 'chat_sessions',
                     'chat_messages', 'rag_files'
    )
  AND column_name = 'id'
ORDER BY table_name;
