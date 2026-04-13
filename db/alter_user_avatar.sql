USE seckill_mall;
ALTER TABLE `user` ADD COLUMN `avatar` varchar(500) DEFAULT NULL AFTER `phone`;
