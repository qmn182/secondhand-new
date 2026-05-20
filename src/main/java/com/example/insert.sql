USE `secondhand`;
-- 插入管理员用户

INSERT INTO `user` (
    `username`, `password`, `phone`, `email`, `city`, `gender`, `bank_account`, 
    `role`, `status`, `business_license`, `id_card_image`, `shop_name`, 
    `level`, `wallet`, `points`, `merchant_status`, `create_time`, `buyer_rating`
) VALUES (
    'admin', 
    '$2a$10$8GzYwR.WZ3nKzVx5c7nK1uLKpXwDqOvE6kL8nFqJvJ3xHlNcVnFq',  -- 密码：12345
    '13800000000', 
    'admin@example.com', 
    '北京', 
    1, 
    '6222000000000000000', 
    0,                      -- role = 0 管理员
    1,                      -- status = 1 启用
    NULL, 
    NULL, 
    '官方旗舰店', 
    5, 
    10000.00, 
    1000, 
    1,                      -- merchant_status = 1 商家
    NOW(), 
    5.00
);

-- 插入普通用户
-- 2. 插入普通用户（role = 1 普通用户，status = 1 启用，merchant_status = 0 非商家）
INSERT INTO `user` (
    `username`, `password`, `phone`, `email`, `city`, `gender`, `bank_account`, 
    `role`, `status`, `business_license`, `id_card_image`, `shop_name`, 
    `level`, `wallet`, `points`, `merchant_status`, `create_time`, `buyer_rating`
) VALUES 
    ('zhangsan', '$2a$10$8GzYwR.WZ3nKzVx5c7nK1uLKpXwDqOvE6kL8nFqJvJ3xHlNcVnFq', '13811111111', 'zhangsan@qq.com', '上海', 1, '6222001111111111111', 1, 1, NULL, NULL, NULL, 1, 500.00, 200, 0, NOW(), 4.50),
    ('lisi',     '$2a$10$8GzYwR.WZ3nKzVx5c7nK1uLKpXwDqOvE6kL8nFqJvJ3xHlNcVnFq', '13822222222', 'lisi@qq.com',     '广州', 0, '6222002222222222222', 1, 1, NULL, NULL, NULL, 2, 1200.00, 500, 0, NOW(), 4.80),
    ('wangwu',   '$2a$10$8GzYwR.WZ3nKzVx5c7nK1uLKpXwDqOvE6kL8nFqJvJ3xHlNcVnFq', '13833333333', 'wangwu@163.com', '深圳', 1, '6222003333333333333', 1, 1, NULL, NULL, NULL, 1, 200.00, 80, 0, NOW(), 4.20);

-- 3. 插入商家用户（role = 1 普通用户，但 merchant_status = 1 表示商家）
INSERT INTO `user` (
    `username`, `password`, `phone`, `email`, `city`, `gender`, `bank_account`, 
    `role`, `status`, `business_license`, `id_card_image`, `shop_name`, 
    `level`, `wallet`, `points`, `merchant_status`, `create_time`, `buyer_rating`
) VALUES (
    'merchant1', 
    '$2a$10$8GzYwR.WZ3nKzVx5c7nK1uLKpXwDqOvE6kL8nFqJvJ3xHlNcVnFq', 
    '13966666666', 
    'shop@example.com', 
    '杭州', 
    0, 
    '6222008888888888888', 
    1,                     -- role: 1=普通用户（商家身份由 merchant_status 区分）
    1,                     -- status: 启用
    '91330100MA12345678', 
    'https://example.com/license.jpg', 
    '二手数码专营店', 
    3, 
    5000.00, 
    800, 
    1,                     -- merchant_status: 1=商家
    NOW(), 
    4.90
);