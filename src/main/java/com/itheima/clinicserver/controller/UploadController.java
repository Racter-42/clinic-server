package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController                                   // 接口类，返回 JSON
public class UploadController {

    // 上传路径：统一定义成常量，方便以后改
    private static final String UPLOAD_DIR = "D:/uploads/";

    @PostMapping("/upload")                       // 接口地址：POST /upload
    public Result upload(MultipartFile file) throws IOException {
        // 参数名必须叫 file，前端表单里的字段名也要叫 file，两边要一致

        String originalFilename = file.getOriginalFilename();   // 取原始文件名，如 "头像.jpg"
        int index = originalFilename.lastIndexOf(".");          // 找最后一个点的位置
        String ext = originalFilename.substring(index);          // 从点截到末尾 → ".jpg"

        String newFileName = UUID.randomUUID().toString() + ext; //  UUID + 扩展名，保证不重名

        file.transferTo(new File(UPLOAD_DIR + newFileName));     //  把文件内容写到磁盘

        return Result.success(newFileName);                      // 返回新文件名给前端保存
    }
}
