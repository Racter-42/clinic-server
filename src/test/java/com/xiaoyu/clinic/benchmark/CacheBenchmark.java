package com.xiaoyu.clinic.benchmark;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

// 缓存性能对比：冷缓存（每轮先删缓存，真走 MySQL）vs 热缓存（命中 Redis）
// 跑法：先把项目启动起来，再运行 main，参数传登录账号密码：CacheBenchmark admin 123456
public class CacheBenchmark {

    private static final String URL = "http://localhost:8080/doctor/list";
    private static final String CACHE_KEY = "doctor:list";
    private static final int ROUNDS = 10;
    private static final int WARMUP = 5;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法：CacheBenchmark <用户名> <密码>");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", login(restTemplate, args[0], args[1]));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 删缓存必须在代码里做：只在开头删一次的话，同一个循环里后 9 次请求全命中缓存，
        // 算出来的"走数据库"平均其实是 1 次数据库 + 9 次缓存，数字是假的
        StringRedisTemplate redis = redisTemplate();

        // 预热：前几次请求里混着类加载、连接池初始化、JIT 编译，不能算进结果
        for (int i = 0; i < WARMUP; i++) {
            restTemplate.exchange(URL, HttpMethod.GET, entity, String.class);
        }

        long[] cold = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            redis.delete(CACHE_KEY);
            cold[i] = oneRequest(restTemplate, entity);
        }

        long[] hot = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            hot[i] = oneRequest(restTemplate, entity);
        }

        System.out.println("【冷缓存】平均 " + avg(cold) + "ms  最快 " + min(cold) + "ms  最慢 " + max(cold) + "ms");
        System.out.println("【热缓存】平均 " + avg(hot) + "ms  最快 " + min(hot) + "ms  最慢 " + max(hot) + "ms");
    }

    // 打一次请求并返回这次的耗时（端到端：HTTP + Controller + 序列化 + MySQL/Redis）
    private static long oneRequest(RestTemplate restTemplate, HttpEntity<String> entity) {
        long start = System.currentTimeMillis();
        restTemplate.exchange(URL, HttpMethod.GET, entity, String.class);
        return System.currentTimeMillis() - start;
    }

    private static StringRedisTemplate redisTemplate() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    private static String login(RestTemplate restTemplate, String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> req = new HttpEntity<>(
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "http://localhost:8080/login", req, String.class);
        return resp.getBody();   // LoginController 直接返回 token 字符串（非 JSON）
    }

    private static long avg(long[] arr) {
        long sum = 0;
        for (long v : arr) {
            sum += v;
        }
        return sum / arr.length;
    }

    private static long min(long[] arr) {
        long m = arr[0];
        for (long v : arr) {
            if (v < m) {
                m = v;
            }
        }
        return m;
    }

    private static long max(long[] arr) {
        long m = arr[0];
        for (long v : arr) {
            if (v > m) {
                m = v;
            }
        }
        return m;
    }
}
