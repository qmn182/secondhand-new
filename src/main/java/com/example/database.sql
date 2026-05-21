-- ======================================================
-- 二手交易平台数据库初始化脚本（完整版）
-- 数据库名: secondhand
-- 字符集: utf8mb4
-- 适用环境: MySQL 8.0+
-- ======================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `secondhand` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `secondhand`;

-- ======================================================
-- 1. 用户表 (user)
-- ======================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `gender` int DEFAULT NULL COMMENT '性别（1男 2女）',
  `bank_account` varchar(20) DEFAULT NULL COMMENT '银行卡号',
  `role` int NOT NULL DEFAULT '1' COMMENT '角色（1普通用户 2商家 3管理员）',
  `status` int DEFAULT '0' COMMENT '审核状态（0待审核 1已通过）',
  `business_license` varchar(255) DEFAULT NULL COMMENT '营业执照图片路径',
  `id_card_image` varchar(255) DEFAULT NULL COMMENT '身份证图片路径',
  `shop_name` varchar(50) DEFAULT NULL COMMENT '店铺名称',
  `level` int DEFAULT '1' COMMENT '商家等级（1-5）',
  `wallet` decimal(12,2) DEFAULT '0.00' COMMENT '钱包余额',
  `points` int DEFAULT '0' COMMENT '积分',
  `merchant_status` int DEFAULT '0' COMMENT '商家申请状态：0未申请/1待审核/2已通过/3拒绝',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ======================================================
-- 2. 商品表 (product)
-- ======================================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '发布商家ID',
  `shop_name` varchar(100) DEFAULT NULL COMMENT '店铺名称（冗余）',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `category` varchar(50) DEFAULT NULL COMMENT '商品分类',
  `price` decimal(10,2) NOT NULL COMMENT '当前售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `discount` decimal(3,2) DEFAULT '1.00' COMMENT '折扣率',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存数量',
  `sold` int DEFAULT '0' COMMENT '历史销量',
  `description` text COMMENT '商品描述',
  `image_url` varchar(255) DEFAULT NULL COMMENT '封面图片路径',
  `images` text COMMENT '多张图片路径(JSON数组)',
  `negotiable` tinyint(1) DEFAULT '0' COMMENT '是否允许议价',
  `condition` varchar(20) DEFAULT NULL COMMENT '新旧程度',
  `status` int DEFAULT '1' COMMENT '状态:1上架 2下架 3已售罄',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ======================================================
-- 3. 购物车表 (cart)
-- ======================================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ======================================================
-- 4. 订单主表 (orders)
-- ======================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号(唯一)',
  `user_id` bigint NOT NULL COMMENT '买家ID',
  `total_amount` decimal(12,2) NOT NULL COMMENT '订单总金额',
  `original_amount` decimal(12,2) DEFAULT NULL COMMENT '原总金额',
  `points_deduct` int DEFAULT '0' COMMENT '使用的积分抵扣额',
  `status` int DEFAULT '1' COMMENT '订单状态:1待付款 2待发货 3待收货 4已完成 5已取消 6退货中 7已退款',
  `remark` varchar(255) DEFAULT NULL COMMENT '买家备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ======================================================
-- 5. 订单商品明细表 (order_item)
-- ======================================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称(快照)',
  `price` decimal(10,2) NOT NULL COMMENT '购买时单价',
  `quantity` int NOT NULL COMMENT '购买数量',
  `total` decimal(12,2) NOT NULL COMMENT '小计',
  `status` int DEFAULT '1' COMMENT '子订单状态:1正常 2已评价 3退货中 4已退货',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单商品明细表';

-- ======================================================
-- 6. 商品评价表 (evaluation)
-- ======================================================
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '评价用户ID(买家)',
  `rating` int NOT NULL COMMENT '评分:1-5星',
  `comment` text COMMENT '评价内容',
  `images` text COMMENT '评价图片(JSON数组)',
  `reply` text COMMENT '商家回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价表';

-- ======================================================
-- 7. 退货申请表 (refund)
-- ======================================================
DROP TABLE IF EXISTS `refund`;
CREATE TABLE `refund` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_item_id` bigint NOT NULL COMMENT '订单明细ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID(买家)',
  `reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `amount` decimal(12,2) NOT NULL COMMENT '申请退款金额',
  `status` int DEFAULT '1' COMMENT '状态:1待审核 2同意 3拒绝 4已完成',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `complete_time` datetime DEFAULT NULL COMMENT '退款完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货申请表';

-- ======================================================
-- 8. 交易流水表 (transaction)
-- ======================================================
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE `transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `amount` decimal(12,2) NOT NULL COMMENT '变动金额(正为增加,负为减少)',
  `type` varchar(20) NOT NULL COMMENT '类型:充值,消费,退款,手续费,积分兑换',
  `balance` decimal(12,2) NOT NULL COMMENT '变动后钱包余额',
  `link_id` bigint DEFAULT NULL COMMENT '关联ID(如订单ID)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易流水表';

-- ======================================================
-- 9. 商家等级费率配置表 (level_config)
-- ======================================================
DROP TABLE IF EXISTS `level_config`;
CREATE TABLE `level_config` (
  `level` int NOT NULL COMMENT '商家等级(1-5)',
  `fee_rate` decimal(5,4) NOT NULL COMMENT '交易费率(如0.001即0.1%)',
  `description` varchar(100) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家等级费率配置';

-- 初始化等级费率数据
INSERT INTO `level_config` (`level`, `fee_rate`, `description`) VALUES
(1, 0.001, '1级商家费率0.1%'),
(2, 0.002, '2级商家费率0.2%'),
(3, 0.005, '3级商家费率0.5%'),
(4, 0.0075, '4级商家费率0.75%'),
(5, 0.010, '5级商家费率1%');

-- ======================================================
-- 10. 初始化管理员账号（密码:123456，BCrypt加密）
-- ======================================================
INSERT INTO `user` (`username`, `password`, `role`, `status`, `create_time`) 
SELECT 'admin', '$2a$10$NkMp1xNq2RwE.2qRvYfXJO5E0Iq1JgkL8LlJGyZ5rG6h5tJ0RqLtO', 3, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin');

-- ======================================================
-- 11. 新增表：首页轮播图表 (banner)
-- ======================================================
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `image_url` varchar(255) NOT NULL COMMENT '图片地址',
  `link_url` varchar(255) DEFAULT NULL COMMENT '点击跳转链接（商品详情/店铺/活动页）',
  `type` tinyint DEFAULT '1' COMMENT '类型：1商品 2店铺 3外部链接',
  `target_id` bigint DEFAULT NULL COMMENT '关联ID（商品ID或店铺ID）',
  `sort_order` int DEFAULT '0' COMMENT '排序（越小越靠前）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页轮播图表';

-- ======================================================
-- 12. 新增表：黑名单表 (blacklist)
-- ======================================================
DROP TABLE IF EXISTS `blacklist`;
CREATE TABLE `blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '被拉黑的用户ID',
  `seller_id` bigint DEFAULT NULL COMMENT '拉黑的商家ID（为NULL表示平台全局拉黑）',
  `reason` varchar(255) DEFAULT NULL COMMENT '拉黑原因',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（管理员或商家）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_seller` (`user_id`, `seller_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单表（平台/商家拉黑用户）';

-- ======================================================
-- 13. 新增表：商家惩罚记录表 (seller_punishment)
-- ======================================================
DROP TABLE IF EXISTS `seller_punishment`;
CREATE TABLE `seller_punishment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL COMMENT '商家用户ID',
  `punish_type` tinyint NOT NULL COMMENT '惩罚类型：1禁止发布商品 2店铺全部下架 3禁止登录',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '惩罚原因',
  `operator_id` bigint NOT NULL COMMENT '操作管理员ID',
  `status` tinyint DEFAULT '1' COMMENT '状态：1生效中 2已解除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_time_status` (`end_time`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家惩罚封禁记录表';

-- ======================================================
-- 14. 新增表：商家服务评价表 (merchant_evaluation)
-- ======================================================
DROP TABLE IF EXISTS `merchant_evaluation`;
CREATE TABLE `merchant_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `seller_id` bigint NOT NULL COMMENT '商家ID',
  `buyer_id` bigint NOT NULL COMMENT '买家ID',
  `service_rating` int NOT NULL COMMENT '服务评分：1-5星',
  `service_comment` text COMMENT '服务评价内容',
  `reply` text COMMENT '商家回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_seller` (`order_id`, `seller_id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家服务评价表';

-- ======================================================
-- 15. 新增表：积分变动明细表 (points_record)
-- ======================================================
DROP TABLE IF EXISTS `points_record`;
CREATE TABLE `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `points` int NOT NULL COMMENT '变动积分（正为增加，负为扣减）',
  `balance` int NOT NULL COMMENT '变动后总积分',
  `type` varchar(20) NOT NULL COMMENT '类型：消费得积分、订单抵扣、管理员调整',
  `link_id` bigint DEFAULT NULL COMMENT '关联ID（订单ID等）',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动明细表';

-- ======================================================
-- 16. 字段修改：商品表 status 字段（增加待审核状态）
-- ======================================================
ALTER TABLE `product` 
MODIFY COLUMN `status` int DEFAULT '0' COMMENT '商品状态：0待审核 1已上架 2已下架 3已售罄';

-- ======================================================
-- 17. 字段扩展：订单表增加资金托管相关字段
-- ======================================================
ALTER TABLE `orders` 
ADD COLUMN `platform_fee` decimal(12,2) DEFAULT '0.00' COMMENT '平台手续费' AFTER `total_amount`,
ADD COLUMN `seller_income` decimal(12,2) DEFAULT '0.00' COMMENT '卖家实收金额（扣费后）' AFTER `platform_fee`,
ADD COLUMN `auto_confirm_deadline` datetime DEFAULT NULL COMMENT '自动确认收货截止时间（发货时间+7天）' AFTER `confirm_time`,
ADD COLUMN `escrow_status` tinyint DEFAULT '1' COMMENT '资金托管状态：1待支付 2已托管（支付后） 3已结算（确认收货后）' AFTER `status`;


ALTER TABLE `product` 
ADD COLUMN `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注（拒绝理由）' AFTER `status`;

ALTER TABLE `orders` 
ADD COLUMN `delivery_company` varchar(50) DEFAULT NULL COMMENT '物流公司',
ADD COLUMN `delivery_no` varchar(50) DEFAULT NULL COMMENT '运单号';

CREATE TABLE IF NOT EXISTS `seller_level_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL,
  `old_level` int NOT NULL,
  `new_level` int NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `product` ADD COLUMN `avg_rating` DECIMAL(2,1) DEFAULT 0.0 COMMENT '平均评分';

CREATE TABLE `buyer_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `buyer_id` bigint NOT NULL COMMENT '买家ID',
  `seller_id` bigint NOT NULL COMMENT '商家ID',
  `rating` int NOT NULL COMMENT '评分1-5',
  `comment` varchar(255) DEFAULT NULL COMMENT '评价内容',
  `reply` varchar(255) DEFAULT NULL COMMENT '买家回复（可选）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `user` ADD COLUMN `buyer_rating` DECIMAL(2,1) DEFAULT 0.0 COMMENT '买家平均评分';

ALTER TABLE `refund` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注（拒绝理由等）';