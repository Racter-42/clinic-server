package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.exception.BusinessException;
import com.itheima.clinicserver.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Tag(name = "文件上传", description = "文件上传相关接口")
@RestController
public class UploadController {

    // ========== 上传根目录：从配置文件读，不再写死 D:/uploads/ ==========
    // 为什么改成配置？写死 D 盘的话，换台电脑（没 D 盘）或部署到 Linux 服务器，
    // 目录不存在就直接崩溃。改成配置后，换环境只改配置，不用改代码重新编译
    @Value("${clinic.upload.path}")
    private String uploadPath;

    // ========== 允许的图片扩展名白名单 ==========
    // 为什么用白名单不用黑名单？黑名单要穷举所有危险类型（.jsp/.exe/.sh/.php...）永远列不完，
    // 白名单只放行这几种，漏掉的默认拒绝，安全性高一个量级
    private static final List<String> ALLOWED_EXT =
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Operation(summary = "图片上传")
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException {
        // @RequestParam("file")：参数名必须等于前端表单字段名
        // （Postman 里 Body 的 key 也必须叫 file，对不上会绑定失败）

        // ---------- ① 空文件校验 ----------
        // 不判空的话，file 为空时 getOriginalFilename() 返回 null，
        // 后面调 lastIndexOf(".") 就会空指针崩溃（500）
        if (file == null || file.isEmpty()) {
            throw new BusinessException(4001, "上传失败：文件为空");
        }

        String originalFilename = file.getOriginalFilename();

        // ---------- ② 文件名合法性校验 ----------
        // 有些浏览器/工具传来的文件名不带扩展名，甚至带完整路径
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(4001, "上传失败：文件名不合法");
        }

        // ---------- ③ 截取扩展名 ----------
        // lastIndexOf(".") 取最后一个点：文件名 "a.b.jpg" 要取 ".jpg" 而不是 ".b.jpg"
        // toLowerCase() 是因为用户可能传 "头像.JPG"，不转小写白名单匹配不上会被误杀
        String ext = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();

        // ---------- ④ 扩展名白名单校验 ----------
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(4001, "上传失败：仅支持 jpg/jpeg/png/gif/webp 格式");
        }

        // ---------- ⑤ 目录不存在则自动创建 ----------
        // 新环境第一次跑，./uploads/ 目录还不存在，直接 transferTo 会抛 IOException
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath());
        }

        // ---------- ⑥ UUID 重命名 ----------
        // 为什么不用原文件名？两个医生都传"头像.jpg"，第二个会覆盖第一个
        // UUID 是 128 位全球唯一标识，重复概率约等于买彩票连中三次
        String newFileName = UUID.randomUUID().toString() + ext;

        // ---------- ⑦ 写入磁盘 ----------
        // 用 new File(dir, newFileName) 而不是字符串拼接，能自动处理路径分隔符（/ 和 \）
        file.transferTo(new File(dir, newFileName));

        // ---------- ⑧ 返回完整访问路径 ----------
        // 必须带 /uploads/ 前缀：前端拿到后可以直接 <img src="http://host/uploads/xxx.jpg">
        // 不用自己拼路径（否则前端要硬编码前缀，以后改路径两边都得改）
        return Result.success("/uploads/" + newFileName);
    }
}
