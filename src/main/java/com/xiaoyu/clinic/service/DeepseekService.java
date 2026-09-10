package com.xiaoyu.clinic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepseekService {

    private static final Logger log = LoggerFactory.getLogger(DeepseekService.class);

    // 降级文案：API 挂了/超时/Key 错了，都返回这句话，至少别让用户对着错误页发呆
    private static final String FALLBACK = "系统繁忙,请前往导诊台咨询";

    private final String apiKey;
    private final String apiUrl;

    // RestTemplate 本身线程安全，全局一个就够。
    // 但直接 new 出来是没有超时的：对方服务器万一卡住，这个请求会一直挂着，
    // 把 Tomcat 的线程池占满，别的患者挂号也跟着遭殃——所以必须在工厂里设好超时。
    private final RestTemplate restTemplate;

    // Agent 导诊用：向它取工具菜单，把模型点名的工具交给它执行
    private final AgentToolRegistry registry;

    public DeepseekService(@Value("${deepseek.api-key}") String apiKey,
                           @Value("${deepseek.api-url}") String apiUrl,
                           @Value("${deepseek.connect-timeout:3000}") int connectTimeout,
                           @Value("${deepseek.read-timeout:5000}") int readTimeout,
                           AgentToolRegistry registry) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 3 秒还连不上对方服务器，别等了（对方可能整个挂了）
        factory.setConnectTimeout(connectTimeout);
        // 连上了但 5 秒没回话，也别等了（多半是对方处理不过来）
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
        this.registry = registry;
    }


    public String recommend(String symptom) {
        // 网络这种事儿偶尔会抖一下，所以超时了值得再试一次；
        // 但要是 Key 无效、被限流这种"试多少次都一样"的错误，就别浪费那几秒钟了。
        try {
            return doRecommend(symptom);
        } catch (ResourceAccessException e) {
            // ResourceAccessException = 连接超时/读超时这类网络问题，重试有意义
            log.warn("导诊接口第一次调用超时，休息 300ms 后重试：{}", e.getMessage());
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();   // 把中断状态还回去，别吞掉
            }
            try {
                return doRecommend(symptom);          // 第二次尝试
            } catch (Exception retryEx) {
                log.error("导诊接口重试仍失败，走降级文案", retryEx);
                return FALLBACK;
            }
        } catch (Exception e) {
            // 业务侧错误（4xx/5xx/参数被拒）重试也没用，直接降级
            log.error("导诊接口调用失败，走降级文案", e);
            return FALLBACK;
        }
    }

    public String chatWithTools(String symptom) {
        // 网络抖动值得重试一次；Key 错、被限流这种试也白试
        // 的情况直接降级——判断逻辑和旧版 recommend 完全一样
        try {
            return doChatWithTools(symptom);
        } catch (ResourceAccessException e) {
            // 连接超时/读超时这类网络问题，重试有意义
            log.warn("Agent 导诊第一次调用超时，休息 300ms 后重试：{}",
                    e.getMessage());
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();  // 中断状态还回去
            }
            try {
                return doChatWithTools(symptom);      // 第二次尝试
            } catch (Exception retryEx) {
                log.error("Agent 导诊重试仍失败，走降级文案", retryEx);
                return FALLBACK;
            }
        } catch (Exception e) {
            // 业务侧错误（4xx/5xx/参数被拒）重试也没用
            log.error("Agent 导诊调用失败，走降级文案", e);
            return FALLBACK;
        }
    }

    // 真正发请求 + 跑循环。异常不在这处理，抛给外壳决定重试/降级
    private String doChatWithTools(String symptom) {
        // ① 请求头：JSON + 身份证（和旧版一样）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // ② messages 循环外创建：定人设 + 患者症状
        // 这个列表只会越加越长，每轮全量重发给模型
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是医院导诊助手。回答患者前，"
                + "先用提供的工具查询真实科室和排班数据，"
                + "再根据查询结果回答。");
        messages.add(systemMsg);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", symptom);
        messages.add(userMsg);

        // ③ 循环：最多 3 轮。我们的场景最多点两次菜
        // （查科室 → 查排班），第 3 轮该出结论了
        for (int round = 0; round < 3; round++) {
            // 请求体每轮重新拼：messages 变了；tools 也每轮都带，
            // 漏了它模型下一轮就"忘"了菜单，不会再点菜
            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");
            body.put("stream", false);
            body.put("messages", messages);
            body.put("tools", registry.buildTools());

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(apiUrl, entity, Map.class);

            // 拆响应老三样（和旧版相同）：body → choices → message
            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                return FALLBACK;
            }
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return FALLBACK;
            }
            Map<String, Object> message =
                    (Map<String, Object>)
                            choices.get(0).get("message");

            // 先看信号灯：这轮模型是"说话"还是"点菜"
            String finishReason =
                    (String) choices.get(0).get("finish_reason");
            log.info("Agent 第 {} 轮，finish_reason={}",
                    round, finishReason);

            // ---- 出口①：stop，模型开口了，返回最终回答 ----
            if ("stop".equals(finishReason)) {
                String content = (String) message.get("content");
                if (content == null || content.isBlank()) {
                    return FALLBACK;   // 回了空话，当异常处理
                }
                return content.trim(); // 唯一带答案离开的出口
            }

// ---- 出口②：tool_calls，模型点菜了 ----
            if ("tool_calls".equals(finishReason)) {
                List<Map<String, Object>> toolCalls =
                        (List<Map<String, Object>>)
                                message.get("tool_calls");
                if (toolCalls == null || toolCalls.isEmpty()) {
                    return FALLBACK;   // 说要点菜却没给单，降级
                }
                // 简化：一轮点多道菜时只处理第一道（够用）
                Map<String, Object> toolCall = toolCalls.get(0);
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function =
                        (Map<String, Object>) toolCall.get("function");
                String name = (String) function.get("name");
                // arguments 是一段 JSON 文本不是对象！
                // execute 第二个参数收的就是文本，直接传
                String arguments = (String) function.get("arguments");

                log.info("Agent 第 {} 轮模型点菜：{}", round, name);

                // 执行工具。execute 内部已把业务失败转成错误文本，
                // 这层再包一次是防注册器本身出意外——
                // 反正异常不能冲出循环炸掉整个请求
                String toolResult;
                try {
                    toolResult = registry.execute(name, arguments);
                } catch (Exception e) {
                    log.error("工具 {} 执行出错", name, e);
                    toolResult = "工具执行失败";
                }

                // 追加两条消息，模型下一轮才看得见刚才发生的事：
                // 1) 点菜的那条 assistant 消息，原样放回
                // 2) role=tool 的结果消息，tool_call_id 必须对上
                messages.add(message);
                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", toolCallId);
                toolMsg.put("content", toolResult);
                messages.add(toolMsg);

                continue;   // 端着菜回炉，进入下一轮
            }

// ---- 出口③：其他值（length / content_filter 等），降级 ----
            return FALLBACK;
        }
        return FALLBACK;
    }

    /** 真正发一次请求：拼参数 → 发送 → 解析出科室名。异常不在这里处理，抛给上层决定要不要重试 */
    private String doRecommend(String symptom) {
        // ① 请求头：告诉对方"我发的是 JSON + 这是我的身份证"
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);   // Content-Type: application/json
        headers.setBearerAuth(apiKey);                        // Authorization: Bearer sk-xxx

        // ② 请求体：按 Deepseek 规定的格式拼
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

        // ③ 把请求头和请求体打包成一个整体发出去
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

        // ④ 拆响应：AI 的回答在 choices[0].message.content 里
        Map<String, Object> respBody = response.getBody();
        if (respBody == null) {
            return FALLBACK;                              // 响应体为空，当降级处理
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) respBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            return FALLBACK;                              // 没有 choices，当降级处理
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            return FALLBACK;                              // AI 回了空字符串，当降级处理
        }
        return content.trim();                            // 去掉首尾空格再返回
    }
}
