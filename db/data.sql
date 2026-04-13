USE seckill_mall;

INSERT INTO `user` (`username`, `password`, `phone`, `role`, `status`) VALUES
('admin', '$2a$10$dHWpKntxQ7UYJIS.xTDc6Ov2.LHMJK1jjwmn30d6RP.jLf3TadJca', '13800000000', 'admin', 1),
('user1', '$2a$10$Eb.REVj3mhXZ1JkMy4SImOQsZ6IiAAaDWORy1vdFWWMjlqJ83ZufS', '13900000001', 'user', 1);

INSERT INTO `category` (`id`, `name`, `parent_id`) VALUES
(1, '数码', 0),
(2, '手机', 1),
(3, '食品', 0),
(4, '零食', 3);

INSERT INTO `product` (`name`, `category_id`, `price`, `stock`, `description`, `image`, `status`, `tag`, `create_time`) VALUES
('演示手机 A', 2, 1999.00, 200, '高性价比演示商品', 'https://picsum.photos/seed/p1/400/400', 1, '电子产品', NOW()),
('演示耳机 B', 2, 299.00, 500, '无线蓝牙耳机', 'https://picsum.photos/seed/p2/400/400', 1, '电子产品', NOW()),
('秒杀款手机 S', 2, 2999.00, 80, '参与秒杀的机型', 'https://picsum.photos/seed/p3/400/400', 1, '电子产品', NOW()),
('快充移动电源', 2, 129.00, 600, '20000mAh 双向快充', 'https://picsum.photos/seed/p4/400/400', 1, '电子产品', NOW()),
('智能手环 Pro', 2, 399.00, 320, '心率睡眠监测', 'https://picsum.photos/seed/p5/400/400', 1, '电子产品', NOW()),
('机械键盘 K8', 2, 459.00, 150, '热插拔 RGB', 'https://picsum.photos/seed/p6/400/400', 1, '电子产品', NOW()),
('4K 显示器 27寸', 2, 1899.00, 90, 'IPS 高色域', 'https://picsum.photos/seed/p7/400/400', 1, '电子产品', NOW()),
('进口巧克力礼盒', 4, 88.00, 800, '零食礼盒', 'https://picsum.photos/seed/p8/400/400', 1, '食品', NOW()),
('每日坚果混合装', 4, 59.00, 1200, '30 小包', 'https://picsum.photos/seed/p9/400/400', 1, '食品', NOW()),
('燕麦牛奶饮品', 4, 39.90, 2000, '早餐即饮', 'https://picsum.photos/seed/p10/400/400', 1, '食品', NOW()),
('冻干咖啡粉', 3, 69.00, 450, '冷萃即溶', 'https://picsum.photos/seed/p11/400/400', 1, '食品', NOW()),
('牛肉干 香辣味', 4, 45.00, 900, '休闲零食', 'https://picsum.photos/seed/p12/400/400', 1, '食品', NOW());

INSERT INTO `seckill_event` (`product_id`, `seckill_price`, `seckill_stock`, `start_time`, `end_time`, `status`) VALUES
(3, 999.00, 50, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1),
(2, 99.00, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1);

INSERT INTO `user_action_log` (`user_id`, `item_id`, `action_type`, `create_time`) VALUES
(2, 1, 'view', NOW()),
(2, 2, 'view', NOW());
