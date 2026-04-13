CREATE DATABASE IF NOT EXISTS seckill_mall DEFAULT CHARACTER SET utf8mb4;
USE seckill_mall;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `role` varchar(20) DEFAULT 'user',
  `status` tinyint DEFAULT 1,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
);

CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `parent_id` int DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `stock` int DEFAULT 0,
  `description` text,
  `image` varchar(500) DEFAULT NULL,
  `status` tinyint DEFAULT 1,
  `tag` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
);

CREATE TABLE `seckill_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `seckill_price` decimal(10,2) DEFAULT NULL,
  `seckill_stock` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` tinyint DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`start_time`,`end_time`)
);

CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `total_amount` decimal(10,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT '待支付',
  `pay_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_time` (`status`,`create_time`)
);

CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `seckill_event_id` bigint DEFAULT NULL,
  `seckill_flag` tinyint DEFAULT 0,
  `seckill_price` decimal(10,2) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
);

CREATE TABLE `user_action_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `action_type` varchar(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
);

CREATE TABLE `seckill_reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `seckill_time` datetime DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_seckill_time` (`seckill_time`,`status`)
);

CREATE TABLE `user_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `is_read` tinyint DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`)
);

CREATE TABLE `exception_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `detail` text,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
);
