package com.itheima.clinicserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepseekService {

    @Value("${deepseek.api-key}")           // 从 application.properties 读真实 Key（不入库）
    private String apiKey;

    @Value("${deepseek.api-url}")           // 从配置读接口地址
    private String apiUrl;

    // final：这个实例只允许赋值一次，防止后续代码误改指向
    // RestTemplate 本身线程安全，全局一个就够
    private final RestTemplate restTemplate = new RestTemplate();

    // 降级文案：API 挂了/超时/Key 错了，都返回这句话
    private static final String FALLBACK = "系统繁忙,请前往导诊台咨询";


    public String recommend(String symptom) {
        try {
            // ===== ① 请求头：告诉对方"我是 JSON + 这是我的身份证" =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);   // Content-Type: application/json
            headers.setBearerAuth(apiKey);                        // Authorization: Bearer sk-xxx

            // ===== ② 请求体：按 Deepseek 规定的格式拼 =====
            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");       // 模型名，固定
            body.put("stream", false);                // false = 一次性返回，不用处理流式

            List<Map<String, String>> messages = new ArrayList<>();

            // 第一条：system 角色 —— 给 AI 定人设和输出格式
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是医院导诊助手。根据患者描述的症状，只推荐一个最合适的科室名称。"
                    + "只返回科室名称本身，不要任何解释、标点或多余文字。");
            messages.add(systemMsg);

            // 第二条：user 角色 —— 用户实际输入的症状
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", symptom);
            messages.add(userMsg);

            body.put("messages", messages);


            // ===== ③ 打包：请求体 + 请求头 合成一个 HttpEntity =====
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // ===== ④ 发送：postForEntity(去哪, 发什么, 响应转什么类型) =====
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);


            // ===== ⑤ 拆响应：choices[0].message.content =====
            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                return FALLBACK;                              // 响应体为空，降级
            }

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return FALLBACK;                              // 没有 choices，降级
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                return FALLBACK;                              // AI 回了空字符串，降级
            }
            return content.trim();                            // 去掉首尾空格再返回

        } catch (Exception e) {
            // ===== ⑥ 兜底：网络超时 / Key 错误 / 限流 / JSON 解析失败，全部降级 =====
            return FALLBACK;
        }
    }
}