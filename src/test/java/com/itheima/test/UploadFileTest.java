package com.itheima.test;

import org.junit.jupiter.api.Test;

import java.util.UUID;

public class UploadFileTest {
    @Test
    public void test1() {
        String originalFilename = "hello.jpg";
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;
        System.out.println(fileName);
    }
}
