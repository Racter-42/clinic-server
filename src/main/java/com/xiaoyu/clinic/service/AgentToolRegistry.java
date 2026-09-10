package com.xiaoyu.clinic.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {
    private final DoctorService doctorService;
    private final ScheduleService scheduleService;
    private final ObjectMapper objectMapper;

    public AgentToolRegistry(DoctorService doctorService,
                             ScheduleService  scheduleService,
                             ObjectMapper objectMapper){
        this.doctorService = doctorService;
        this.objectMapper = objectMapper;
        this.scheduleService = scheduleService;
        
    }

    // ========== 菜单：把项目里两个查询能力报给 AI ==========
    public List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // ----- 工具1：查各科室在岗医生数（没有参数）-----
        Map<String, Object> fn1 = new HashMap<>();
        // 菜名 = 方法名，第四步分发执行全靠这个名字对上号
        fn1.put("name", "getDeptStats");
        // 说明书写给模型看：它靠这句话判断该不该点这道菜
        fn1.put("description",
                "查询医院所有科室及各科室在岗医生数量，"
                        + "患者问有哪些科室时用这个");
        // 没有参数也要把三件套写全：properties 空对象、
        // required 空数组。省掉 parameters 字段有的 API 也认，
        // 但写全最稳，不踩兼容性的坑
        fn1.put("parameters", Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        ));
        // 套上外壳（type=function）再上菜单
        tools.add(Map.of("type", "function", "function", fn1));

        // ----- 工具2：按科室查未来 N 天排班（两个参数）-----
        Map<String, Object> fn2 = new HashMap<>();
        fn2.put("name", "getScheduleByDept");
// shiftType 在数据库里存的是数字，模型看不懂 1/2/3
// 是上午下午还是晚班，所以在说明书里把含义写清楚
        fn2.put("description",
                "按科室查未来 N 天的医生排班，返回医生姓名、"
                        + "日期、时段（shiftType：1上午 2下午 3晚班）");
        fn2.put("parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                        // 科室 ID 是联查的钥匙，没它查不了
                        "deptId", Map.of(
                                "type", "integer",
                                "description", "科室ID，从 getDeptStats "
                                        + "返回结果里的 deptId 取"
                        ),
                        // days 可选：description 里写明不传默认 1，
                        // 模型大多时候就不再乱传了
                        "days", Map.of(
                                "type", "integer",
                                "description", "查未来几天，不传默认 1"
                        )
                ),
                // 只有 deptId 必填；days 有默认值，不进 required
                "required", List.of("deptId")
        ));
        tools.add(Map.of("type", "function", "function", fn2));

        return tools;   // 第三步会在 return 前加第二道菜
    }

    // ========== 后厨：AI 点了哪道菜，就去真正执行对应方法 ==========
// name：AI 点的工具名（对应菜单里的 name 字段）
// argsJson：AI 传来的参数。注意是一段 JSON 文本，不是 Map
// 返回 String：执行结果要变回文本喂给 AI，它只认文本
    public String execute(String name, String argsJson) {
        // 执行出错也返回一句人话，让 AI 能自己组织语言安抚患者；
        // 要是把异常往外抛，整个导诊接口就 500 了
        try {
            if ("getDeptStats".equals(name)) {
                // 无参工具，argsJson 是啥不用管，直接查
                return toJson(doctorService.countByDept());
            }
            if ("getScheduleByDept".equals(name)) {
                Map<String, Object> args = parseArgs(argsJson);
                // deptId 是必填的，AI 没传就回一句提示，
                // 它下一轮会把参数补上
                if (args.get("deptId") == null) {
                    return "缺少参数 deptId";
                }
                // 先转 Number 再取 int 最稳：AI 偶尔把整数
                // 写成 1.0，直接强转 (Integer) 会当场炸掉
                int deptId = ((Number) args.get("deptId")).intValue();
                // days 不是必填，没传就默认 1（只看明天）
                int days = 1;
                if (args.get("days") != null) {
                    days = ((Number) args.get("days")).intValue();
                }
                return toJson(scheduleService.queryByDept(deptId, days));
            }
            // AI 点了菜单上没有的菜，回一句让它知道点错了
            return "没有叫 " + name + " 的工具";
        } catch (Exception e) {
            return "工具执行失败：" + e.getMessage();
        }
    }

    // 参数 JSON 文本变回 Map，才能按名字取参数
// AI 对无参工具可能传空串也可能干脆不传，统一当空对象处理
    private Map<String, Object> parseArgs(String argsJson) throws Exception {
        if (argsJson == null || argsJson.isBlank()) {
            return new HashMap<>();
        }
        // readValue：把 JSON 文本按 key=value 拆回 Map
        return objectMapper.readValue(argsJson, Map.class);
    }

    // 执行结果转 JSON 文本，喂回模型的就是这段字符串
// 转失败给个空数组兜底，别让整条链路崩掉
    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "[]";
        }
    }
}
