package com.pcl.proxy;

/**
 * @ClassName ByteTest
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/24 18:34
 * @Version F02SP03
 **/
public class ByteTest {

    public static void main(String[] args) {
        byte b = -13;
        String binaryString = Integer.toBinaryString(b & 0xFF);
        System.out.println(binaryString);
        byte a = 9;
        System.out.println((a & 0xFF)<<8);
    }

}
