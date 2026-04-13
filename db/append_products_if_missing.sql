-- 若您已执行过旧版 data.sql（仅 3 个商品），可单独执行本脚本补齐商品到 12 个（不重复插入用户/场次）。
USE seckill_mall;

INSERT INTO `product` (`name`, `category_id`, `price`, `stock`, `description`, `image`, `status`, `tag`, `create_time`) VALUES
('快充移动电源', 2, 129.00, 600, '20000mAh 双向快充', 'https://picsum.photos/seed/p4/400/400', 1, '电子产品', NOW()),
('智能手环 Pro', 2, 399.00, 320, '心率睡眠监测', 'https://picsum.photos/seed/p5/400/400', 1, '电子产品', NOW()),
('机械键盘 K8', 2, 459.00, 150, '热插拔 RGB', 'https://picsum.photos/seed/p6/400/400', 1, '电子产品', NOW()),
('4K 显示器 27寸', 2, 1899.00, 90, 'IPS 高色域', 'https://picsum.photos/seed/p7/400/400', 1, '电子产品', NOW()),
('进口巧克力礼盒', 4, 88.00, 800, '零食礼盒', 'https://picsum.photos/seed/p8/400/400', 1, '食品', NOW()),
('每日坚果混合装', 4, 59.00, 1200, '30 小包', 'https://picsum.photos/seed/p9/400/400', 1, '食品', NOW()),
('燕麦牛奶饮品', 4, 39.90, 2000, '早餐即饮', 'https://picsum.photos/seed/p10/400/400', 1, '食品', NOW()),
('冻干咖啡粉', 3, 69.00, 450, '冷萃即溶', 'https://picsum.photos/seed/p11/400/400', 1, '食品', NOW()),
('牛肉干 香辣味', 4, 45.00, 900, '休闲零食', 'https://picsum.photos/seed/p12/400/400', 1, '食品', NOW());
