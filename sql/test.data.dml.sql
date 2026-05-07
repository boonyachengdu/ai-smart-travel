-- =====================================================
-- 智能商旅系统 - 初始化数据脚本 (PostgreSQL)
-- 生成日期：2026-04-01
-- 说明：为用户、公司、员工、订单、审批等表生成测试数据
-- =====================================================

-- 1. 清理现有数据（可选，生产环境请谨慎使用）
TRUNCATE TABLE approvals RESTART IDENTITY CASCADE;
TRUNCATE TABLE orders RESTART IDENTITY CASCADE;
TRUNCATE TABLE employees RESTART IDENTITY CASCADE;
TRUNCATE TABLE departments RESTART IDENTITY CASCADE;
TRUNCATE TABLE company RESTART IDENTITY CASCADE;
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- =====================================================
-- 2. 插入用户数据 (约 20 条)
-- =====================================================
INSERT INTO users (username, password, email, phone, roles, create_time, update_time, deleted) VALUES
                                                                                                   ('admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'admin@smarttravel.com', '13800138000', 'ADMIN', NOW(), NOW(), 0),
                                                                                                   ('zhangsan', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'zhangsan@company.com', '13800138001', 'USER', NOW(), NOW(), 0),
                                                                                                   ('lisi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'lisi@company.com', '13800138002', 'USER', NOW(), NOW(), 0),
                                                                                                   ('wangwu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'wangwu@company.com', '13800138003', 'USER', NOW(), NOW(), 0),
                                                                                                   ('zhaoliu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'zhaoliu@company.com', '13800138004', 'USER', NOW(), NOW(), 0),
                                                                                                   ('sunqi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'sunqi@company.com', '13800138005', 'USER', NOW(), NOW(), 0),
                                                                                                   ('zhouba', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'zhouba@company.com', '13800138006', 'USER', NOW(), NOW(), 0),
                                                                                                   ('zhengjiu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'zhengjiu@company.com', '13800138007', 'USER', NOW(), NOW(), 0),
                                                                                                   ('wushi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'wushi@company.com', '13800138008', 'USER', NOW(), NOW(), 0),
                                                                                                   ('liushier', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'liushier@company.com', '13800138009', 'USER', NOW(), NOW(), 0),
                                                                                                   ('chenyi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'chenyi@company.com', '13800138010', 'USER', NOW(), NOW(), 0),
                                                                                                   ('liner', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'liner@company.com', '13800138011', 'USER', NOW(), NOW(), 0),
                                                                                                   ('sansan', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'sansan@company.com', '13800138012', 'USER', NOW(), NOW(), 0),
                                                                                                   ('sisi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'sisi@company.com', '13800138013', 'USER', NOW(), NOW(), 0),
                                                                                                   ('wuwu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'wuwu@company.com', '13800138014', 'USER', NOW(), NOW(), 0),
                                                                                                   ('liuliu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'liuliu@company.com', '13800138015', 'USER', NOW(), NOW(), 0),
                                                                                                   ('qiqi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'qiqi@company.com', '13800138016', 'USER', NOW(), NOW(), 0),
                                                                                                   ('baba', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'baba@company.com', '13800138017', 'USER', NOW(), NOW(), 0),
                                                                                                   ('jiujiu', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'jiujiu@company.com', '13800138018', 'USER', NOW(), NOW(), 0),
                                                                                                   ('shishi', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 'shishi@company.com', '13800138019', 'USER', NOW(), NOW(), 0);

-- =====================================================
-- 3. 插入公司数据 (5 家)
-- =====================================================
INSERT INTO company (name, description, auto_approve, user_name, password, over_amount_must_audit, create_time, update_time, deleted) VALUES
                                                                                                                                          ('华为技术有限公司', '全球领先的 ICT 基础设施和智能终端提供商', false, 'huawei_admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 5000.00, NOW(), NOW(), 0),
                                                                                                                                          ('阿里巴巴集团', '让天下没有难做的生意', true, 'alibaba_admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 10000.00, NOW(), NOW(), 0),
                                                                                                                                          ('腾讯科技有限公司', '通过互联网服务提升人类生活品质', false, 'tencent_admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 8000.00, NOW(), NOW(), 0),
                                                                                                                                          ('字节跳动科技有限公司', '激发创造，丰富生活', false, 'bytedance_admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 6000.00, NOW(), NOW(), 0),
                                                                                                                                          ('美团科技有限公司', '帮大家吃得更好，生活更好', true, 'meituan_admin', '$2a$10$XoLvF5C2dz9.7VqPx8H.eeZT3K4lM6nR8W.yB5vN2Qp1Zx3Wy7UuO', 5000.00, NOW(), NOW(), 0);

-- =====================================================
-- 4. 插入部门数据 (每个公司 2-3 个部门，共 12 个)
-- =====================================================
INSERT INTO departments (company_id, parent_id, name, create_time, update_time, deleted) VALUES
                                                                                                               (1, NULL, '技术部', NOW(), NOW(), 0),
                                                                                                               (1, NULL, '市场部', NOW(), NOW(), 0),
                                                                                                               (1, NULL, '人力资源部', NOW(), NOW(), 0),
                                                                                                               (2, NULL, '研发部', NOW(), NOW(), 0),
                                                                                                               (2, NULL, '运营部', NOW(), NOW(), 0),
                                                                                                               (2, NULL, '财务部', NOW(), NOW(), 0),
                                                                                                               (3, NULL, '产品部', NOW(), NOW(), 0),
                                                                                                               (3, NULL, '技术部', NOW(), NOW(), 0),
                                                                                                               (4, NULL, '内容部', NOW(), NOW(), 0),
                                                                                                               (4, NULL, '技术部', NOW(), NOW(), 0),
                                                                                                               (5, NULL, '业务部', NOW(), NOW(), 0),
                                                                                                               (5, NULL, '客服部', NOW(), NOW(), 0);

-- =====================================================
-- 5. 插入员工数据 (每个用户对应一个员工，共 20 个)
-- =====================================================
INSERT INTO employees (user_id, company_id, department_id, employee_no, name, position, mobile, email, status, create_time, update_time, deleted) VALUES
                                                                                                                                                      (2, 1, 1, 'EMP001', '张三', '软件工程师', '13800138001', 'zhangsan@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (3, 1, 1, 'EMP002', '李四', '高级工程师', '13800138002', 'lisi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (4, 1, 2, 'EMP003', '王五', '市场经理', '13800138003', 'wangwu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (5, 1, 3, 'EMP004', '赵六', 'HR 专员', '13800138004', 'zhaoliu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (6, 2, 4, 'EMP005', '孙七', '技术专家', '13800138005', 'sunqi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (7, 2, 4, 'EMP006', '周八', '架构师', '13800138006', 'zhouba@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (8, 2, 5, 'EMP007', '郑九', '运营总监', '13800138007', 'zhengjiu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (9, 2, 6, 'EMP008', '吴十', '财务主管', '13800138008', 'wushi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (10, 3, 7, 'EMP009', '刘十二', '产品经理', '13800138009', 'liushier@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (11, 3, 7, 'EMP010', '陈一', '高级产品经理', '13800138010', 'chenyi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (12, 3, 8, 'EMP011', '林二', '技术总监', '13800138011', 'liner@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (13, 3, 8, 'EMP012', '三三', '开发工程师', '13800138012', 'sansan@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (14, 4, 9, 'EMP013', '四四', '内容编辑', '13800138013', 'sisi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (15, 4, 9, 'EMP014', '五五', '资深编辑', '13800138014', 'wuwu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (16, 4, 10, 'EMP015', '六六', '算法工程师', '13800138015', 'liuliu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (17, 4, 10, 'EMP016', '七七', '数据科学家', '13800138016', 'qiqi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (18, 5, 11, 'EMP017', '八八', '业务经理', '13800138017', 'baba@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (19, 5, 11, 'EMP018', '九九', '销售总监', '13800138018', 'jiujiu@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (20, 5, 12, 'EMP019', '十十', '客服主管', '13800138019', 'shishi@company.com', 1, NOW(), NOW(), 0),
                                                                                                                                                      (1, 1, 1, 'EMP000', '系统管理员', '管理员', '13800138000', 'admin@smarttravel.com', 1, NOW(), NOW(), 0);

-- =====================================================
-- 6. 插入订单数据 (每个场景 5 条，共 20 条)
-- =====================================================
-- 机票订单 (FLIGHT) - 5 条
INSERT INTO orders (company_id, order_no, user_id, username, order_type, amount, status, audit_status, pay_time, pay_type, refundable, refund_amount, journey, order_requirements, create_time, update_time, deleted) VALUES
                                                                                                                                                                                                                          (1, 'ORD20260401001', 2, 'zhangsan', 'FLIGHT', 1280.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_ALIPAY', true, 0.00, '[{"departure":"北京","arrival":"上海","departureTime":"2026-04-01 08:00:00","arrivalTime":"2026-04-01 10:30:00","flightNo":"CA1501"}]', '{"orderType":"FLIGHT","departureCity":"北京","arrivalCity":"上海","departureDate":"2026-04-01","budget":1500}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (1, 'ORD20260401002', 3, 'lisi', 'FLIGHT', 2350.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_WECHAT', true, 0.00, '[{"departure":"上海","arrival":"深圳","departureTime":"2026-04-01 09:00:00","arrivalTime":"2026-04-01 11:30:00","flightNo":"MU5401"}]', '{"orderType":"FLIGHT","departureCity":"上海","arrivalCity":"深圳","departureDate":"2026-04-01","budget":2500}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (2, 'ORD20260401003', 6, 'sunqi', 'FLIGHT', 1680.00, 'PENDING_PAYMENT', 'APPROVED', NULL, 'PERSONAL_ALIPAY', true, 0.00, '[{"departure":"广州","arrival":"成都","departureTime":"2026-04-01 14:00:00","arrivalTime":"2026-04-01 16:30:00","flightNo":"CZ3501"}]', '{"orderType":"FLIGHT","departureCity":"广州","arrivalCity":"成都","departureDate":"2026-04-01","budget":1800}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (3, 'ORD20260401004', 10, 'liushier', 'FLIGHT', 3200.00, 'DRAFT', 'PENDING', NULL, NULL, true, 0.00, '[{"departure":"北京","arrival":"广州","departureTime":"2026-04-01 10:00:00","arrivalTime":"2026-04-01 13:00:00","flightNo":"HU7801"}]', '{"orderType":"FLIGHT","departureCity":"北京","arrivalCity":"广州","departureDate":"2026-04-01","budget":3500}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (4, 'ORD20260401005', 14, 'sisi', 'FLIGHT', 1950.00, 'CANCELLED', 'REJECTED', NULL, NULL, false, 0.00, '[{"departure":"杭州","arrival":"西安","departureTime":"2026-04-01 11:00:00","arrivalTime":"2026-04-01 13:30:00","flightNo":"MU2201"}]', '{"orderType":"FLIGHT","departureCity":"杭州","arrivalCity":"西安","departureDate":"2026-04-01","budget":2000}', NOW(), NOW(), 0),

-- 酒店订单 (HOTEL) - 5 条
                                                                                                                                                                                                                          (1, 'ORD20260401006', 4, 'wangwu', 'HOTEL', 1800.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_ALIPAY', true, 0.00, '[{"city":"上海","hotelName":"希尔顿酒店","checkInTime":"2026-04-01 14:00:00","checkOutTime":"2026-04-03 12:00:00","roomType":"豪华大床房"}]', '{"orderType":"HOTEL","checkInCity":"上海","checkInDate":"2026-04-01","checkOutDate":"2026-04-03","budget":2000}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (2, 'ORD20260401007', 7, 'zhouba', 'HOTEL', 2400.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_WECHAT', true, 0.00, '[{"city":"北京","hotelName":"万豪酒店","checkInTime":"2026-04-01 14:00:00","checkOutTime":"2026-04-04 12:00:00","roomType":"商务套房"}]', '{"orderType":"HOTEL","checkInCity":"北京","checkInDate":"2026-04-01","checkOutDate":"2026-04-04","budget":2500}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (3, 'ORD20260401008', 11, 'liner', 'HOTEL', 1500.00, 'PENDING_PAYMENT', 'APPROVED', NULL, 'PERSONAL_ALIPAY', true, 0.00, '[{"city":"深圳","hotelName":"洲际酒店","checkInTime":"2026-04-01 14:00:00","checkOutTime":"2026-04-02 12:00:00","roomType":"标准间"}]', '{"orderType":"HOTEL","checkInCity":"深圳","checkInDate":"2026-04-01","checkOutDate":"2026-04-02","budget":1600}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (4, 'ORD20260401009', 15, 'wuwu', 'HOTEL', 3600.00, 'DRAFT', 'PENDING', NULL, NULL, true, 0.00, '[{"city":"三亚","hotelName":"亚特兰蒂斯","checkInTime":"2026-04-01 14:00:00","checkOutTime":"2026-04-05 12:00:00","roomType":"海景房"}]', '{"orderType":"HOTEL","checkInCity":"三亚","checkInDate":"2026-04-01","checkOutDate":"2026-04-05","budget":4000}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (5, 'ORD20260401010', 18, 'baba', 'HOTEL', 1200.00, 'CANCELLED', 'REJECTED', NULL, NULL, false, 0.00, '[{"city":"成都","hotelName":"香格里拉","checkInTime":"2026-04-01 14:00:00","checkOutTime":"2026-04-02 12:00:00","roomType":"豪华间"}]', '{"orderType":"HOTEL","checkInCity":"成都","checkInDate":"2026-04-01","checkOutDate":"2026-04-02","budget":1300}', NOW(), NOW(), 0),

-- 火车订单 (TRAIN) - 5 条
                                                                                                                                                                                                                          (1, 'ORD20260401011', 5, 'zhaoliu', 'TRAIN', 553.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_ALIPAY', true, 0.00, '[{"departure":"北京","arrival":"上海","departureTime":"2026-04-01 09:00:00","arrivalTime":"2026-04-01 13:30:00","trainNo":"G1","seatType":"二等座"}]', '{"orderType":"TRAIN","departureCity":"北京","arrivalCity":"上海","departureDate":"2026-04-01","budget":600}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (2, 'ORD20260401012', 8, 'zhengjiu', 'TRAIN', 892.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_WECHAT', true, 0.00, '[{"departure":"广州","arrival":"武汉","departureTime":"2026-04-01 10:00:00","arrivalTime":"2026-04-01 14:00:00","trainNo":"G1001","seatType":"一等座"}]', '{"orderType":"TRAIN","departureCity":"广州","arrivalCity":"武汉","departureDate":"2026-04-01","budget":900}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (3, 'ORD20260401013', 12, 'sansan', 'TRAIN', 423.00, 'PENDING_PAYMENT', 'APPROVED', NULL, 'PERSONAL_ALIPAY', true, 0.00, '[{"departure":"南京","arrival":"杭州","departureTime":"2026-04-01 11:00:00","arrivalTime":"2026-04-01 13:00:00","trainNo":"G7601","seatType":"二等座"}]', '{"orderType":"TRAIN","departureCity":"南京","arrivalCity":"杭州","departureDate":"2026-04-01","budget":500}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (4, 'ORD20260401014', 16, 'qiqi', 'TRAIN', 1250.00, 'DRAFT', 'PENDING', NULL, NULL, true, 0.00, '[{"departure":"北京","arrival":"广州","departureTime":"2026-04-01 08:00:00","arrivalTime":"2026-04-01 16:00:00","trainNo":"G79","seatType":"商务座"}]', '{"orderType":"TRAIN","departureCity":"北京","arrivalCity":"广州","departureDate":"2026-04-01","budget":1300}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (5, 'ORD20260401015', 19, 'jiujiu', 'TRAIN', 678.00, 'CANCELLED', 'REJECTED', NULL, NULL, false, 0.00, '[{"departure":"上海","arrival":"西安","departureTime":"2026-04-01 07:00:00","arrivalTime":"2026-04-01 13:00:00","trainNo":"G360","seatType":"一等座"}]', '{"orderType":"TRAIN","departureCity":"上海","arrivalCity":"西安","departureDate":"2026-04-01","budget":700}', NOW(), NOW(), 0),

-- 用车订单 (CAR) - 5 条
                                                                                                                                                                                                                          (1, 'ORD20260401016', 2, 'zhangsan', 'CAR', 380.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_ALIPAY', true, 0.00, '[{"city":"北京","useTime":"2026-04-01 09:00:00","destination":"首都机场","carType":"舒适型"}]', '{"orderType":"CAR","city":"北京","useTime":"2026-04-01","destination":"首都机场","budget":400}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (2, 'ORD20260401017', 6, 'sunqi', 'CAR', 520.00, 'COMPLETED', 'APPROVED', NOW(), 'PERSONAL_WECHAT', true, 0.00, '[{"city":"上海","useTime":"2026-04-01 14:00:00","destination":"浦东机场","carType":"商务型"}]', '{"orderType":"CAR","city":"上海","useTime":"2026-04-01","destination":"浦东机场","budget":600}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (3, 'ORD20260401018', 10, 'liushier', 'CAR', 280.00, 'PENDING_PAYMENT', 'APPROVED', NULL, 'PERSONAL_ALIPAY', true, 0.00, '[{"city":"深圳","useTime":"2026-04-01 10:00:00","destination":"宝安机场","carType":"经济型"}]', '{"orderType":"CAR","city":"深圳","useTime":"2026-04-01","destination":"宝安机场","budget":300}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (4, 'ORD20260401019', 14, 'sisi', 'CAR', 850.00, 'DRAFT', 'PENDING', NULL, NULL, true, 0.00, '[{"city":"广州","useTime":"2026-04-01 08:00:00","destination":"白云机场","carType":"豪华型"}]', '{"orderType":"CAR","city":"广州","useTime":"2026-04-01","destination":"白云机场","budget":900}', NOW(), NOW(), 0),
                                                                                                                                                                                                                          (5, 'ORD20260401020', 18, 'baba', 'CAR', 450.00, 'CANCELLED', 'REJECTED', NULL, NULL, false, 0.00, '[{"city":"成都","useTime":"2026-04-01 11:00:00","destination":"双流机场","carType":"舒适型"}]', '{"orderType":"CAR","city":"成都","useTime":"2026-04-01","destination":"双流机场","budget":500}', NOW(), NOW(), 0);

-- =====================================================
-- 7. 插入审批数据 (20 条，对应不同状态)
-- =====================================================
INSERT INTO approvals (order_no, company_id, applicant_id, approver_id, status, reason, remark, trip_items, create_time, update_time, deleted) VALUES
-- 待审批 (5 条)
('ORD20260401004', 3, 10, 11, 'PENDING', '参加技术峰会', NULL, '[{"type":"FLIGHT","description":"北京 - 广州往返","amount":3200}]', NOW(), NOW(), 0),
('ORD20260401009', 4, 15, 14, 'PENDING', '项目调研', NULL, '[{"type":"HOTEL","description":"三亚出差 4 天","amount":3600}]', NOW(), NOW(), 0),
('ORD20260401014', 5, 16, 18, 'PENDING', '商务洽谈', NULL, '[{"type":"TRAIN","description":"北京 - 广州商务座","amount":1250}]', NOW(), NOW(), 0),
('ORD20260401017', 2, 6, 5, 'PENDING', '客户拜访', NULL, '[{"type":"CAR","description":"上海市内用车","amount":520}]', NOW(), NOW(), 0),
('ORD20260401019', 4, 14, 13, 'PENDING', '紧急会议', NULL, '[{"type":"CAR","description":"广州接送机","amount":850}]', NOW(), NOW(), 0),

-- 已通过 (10 条)
('ORD20260401001', 1, 2, 4, 'APPROVED', '参加行业会议', '同意', '[{"type":"FLIGHT","description":"北京 - 上海机票","amount":1280}]', NOW(), NOW(), 0),
('ORD20260401002', 1, 3, 4, 'APPROVED', '技术交流', '同意', '[{"type":"FLIGHT","description":"上海 - 深圳机票","amount":2350}]', NOW(), NOW(), 0),
('ORD20260401003', 2, 6, 8, 'APPROVED', '市场调研', '同意', '[{"type":"FLIGHT","description":"广州 - 成都机票","amount":1680}]', NOW(), NOW(), 0),
('ORD20260401006', 1, 4, 2, 'APPROVED', '项目出差', '同意', '[{"type":"HOTEL","description":"上海酒店住宿","amount":1800}]', NOW(), NOW(), 0),
('ORD20260401007', 2, 7, 8, 'APPROVED', '业务培训', '同意', '[{"type":"HOTEL","description":"北京酒店住宿","amount":2400}]', NOW(), NOW(), 0),
('ORD20260401008', 3, 11, 12, 'APPROVED', '技术支持', '同意', '[{"type":"HOTEL","description":"深圳酒店住宿","amount":1500}]', NOW(), NOW(), 0),
('ORD20260401011', 1, 5, 4, 'APPROVED', '高铁出差', '同意', '[{"type":"TRAIN","description":"北京 - 上海高铁","amount":553}]', NOW(), NOW(), 0),
('ORD20260401012', 2, 8, 7, 'APPROVED', '客户服务', '同意', '[{"type":"TRAIN","description":"广州 - 武汉高铁","amount":892}]', NOW(), NOW(), 0),
('ORD20260401013', 4, 12, 14, 'APPROVED', '项目考察', '同意', '[{"type":"TRAIN","description":"南京 - 杭州高铁","amount":423}]', NOW(), NOW(), 0),
('ORD20260401016', 1, 2, 4, 'APPROVED', '机场接送', '同意', '[{"type":"CAR","description":"北京市内用车","amount":380}]', NOW(), NOW(), 0),

-- 已拒绝 (5 条)
('ORD20260401005', 4, 14, 13, 'REJECTED', '旅游申请', '不符合差旅规定', '[{"type":"FLIGHT","description":"杭州 - 西安机票","amount":1950}]', NOW(), NOW(), 0),
('ORD20260401010', 5, 18, 19, 'REJECTED', '度假申请', '非公务出行', '[{"type":"HOTEL","description":"成都酒店","amount":1200}]', NOW(), NOW(), 0),
('ORD20260401015', 5, 19, 18, 'REJECTED', '私人出行', '不予批准', '[{"type":"TRAIN","description":"上海 - 西安高铁","amount":678}]', NOW(), NOW(), 0),
('ORD20260401018', 3, 10, 11, 'REJECTED', '超预算', '预算过高', '[{"type":"CAR","description":"深圳市内用车","amount":280}]', NOW(), NOW(), 0),
('ORD20260401020', 5, 18, 17, 'REJECTED', '重复申请', '已有相同行程', '[{"type":"CAR","description":"成都市内用车","amount":450}]', NOW(), NOW(), 0);

-- =====================================================
-- 8. 更新序列值 (确保自增 ID 正确)
-- =====================================================
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('company_id_seq', (SELECT MAX(id) FROM company));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('employees_id_seq', (SELECT MAX(id) FROM employees));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('approvals_id_seq', (SELECT MAX(id) FROM approvals));

-- =====================================================
-- 数据统计
-- =====================================================
-- 用户数：20
-- 公司数：5
-- 部门数：12
-- 员工数：20
-- 订单数：20 (机票 5、酒店 5、火车 5、用车 5)
-- 审批数：20 (待审批 5、已通过 10、已拒绝 5)
-- =====================================================


-- 差标数据初始化（企业 ID=1）
INSERT INTO standards (company_id, name, content, description, flight_max_price, hotel_max_price, train_max_price, car_max_price, month_max_price, year_max_price)
VALUES
    (1, '普通员工差标', '适用于普通员工的差旅标准', '经济舱、快捷酒店、高铁二等座', 1500.00, 400.00, 600.00, 150.00, 8000.00, 50000.00),

    (1, '中层管理人员差标', '适用于部门经理等中层管理人员', '公务舱、四星级酒店、高铁一等座', 3000.00, 800.00, 1000.00, 300.00, 15000.00, 100000.00),

    (1, '高级管理人员差标', '适用于总监及以上高管', '头等舱、五星级酒店、飞机优先', 8000.00, 1500.00, 2000.00, 500.00, 30000.00, 200000.00),

    (1, '特殊项目差标', '适用于紧急项目或特殊任务', '可根据实际情况灵活调整', 5000.00, 1000.00, 1200.00, 400.00, 20000.00, 150000.00),

    (1, '实习生差标', '适用于实习生及试用期员工', '经济型交通和住宿', 800.00, 200.00, 300.00, 100.00, 3000.00, 20000.00);
