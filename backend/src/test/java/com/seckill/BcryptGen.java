package com.seckill;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println("admin123: " + enc.encode("admin123"));
        System.out.println("123456: " + enc.encode("123456"));
    }
}
