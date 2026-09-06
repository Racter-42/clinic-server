package com.xiaoyu.clinic.utils;   // utils 包：JWT 工具与基准测试

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CacheBenchmark {   // 缓存性能基准测试：实测"走数据库 vs 走缓存"的耗时差距（README 中 126ms → 6ms 的数据来源）

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();   // Spring 的 HTTP 客户端，直接 new 即可用

        // 1. 登录拿 token（/doctor/list 是需要登录态的接口，LoginInterceptor 校验请求头 token）
        String token = login(restTemplate);
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);                       // 把 token 放进请求头
        HttpEntity<String> entity = new HttpEntity<>(headers);  // 把 headers 包装成 entity，exchange 第三个参数需要

        // 2. 先清一次缓存，保证第一次测的是"走数据库"的耗时
        //    对应 Redis 命令：DEL doctor:list
        //    也可以在代码里调 StringRedisTemplate.delete("doctor:list")，但演示时 redis-cli 更直观

        // 3. 测"走数据库"（缓存没命中，第一次查走 MySQL）
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {                    // 跑 10 次取平均，与 README 中"10 次取平均"对应
            restTemplate.exchange("http://localhost:8080/doctor/list",
                    HttpMethod.GET, entity, String.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("【走数据库】10 次平均：" + (end - start) / 10 + "ms");

        // 4. 现在缓存已经有数据了（第 3 步的第一次写入），直接返回 = 走缓存
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            restTemplate.exchange("http://localhost:8080/doctor/list",
                    HttpMethod.GET, entity, String.class);
        }
        long end2 = System.currentTimeMillis();
        System.out.println("【走缓存】10 次平均：" + (end2 - start2) / 10 + "ms");
    }

    // ========== 登录拿 JWT（admin / 123456 是 LoginController 的固定测试账号）==========
    private static String login(RestTemplate restTemplate) {
        HttpHeaders h = new HttpHeaders();
        h.set("Content-Type", "application/json");          // 告诉服务端请求体是 JSON
        HttpEntity<String> req = new HttpEntity<>(
                "{\"username\":\"admin\",\"password\":\"123456\"}", h);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "http://localhost:8080/login", req, String.class);
        return resp.getBody();                              // LoginController 直接返回 token 字符串（非 JSON）
    }
}