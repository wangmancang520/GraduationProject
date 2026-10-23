package com.example.springboot.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.entity.MyFile;
import com.example.springboot.mapper.FileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${files.upload.path}")
    private String fileUploadPath;

    @Value("${files.upload.public-url}")
    private String publicUrl;

    @Resource
    private FileMapper fileMapper;

    /**
     * 文件上传接口
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws IOException{
        String originalFilename = file.getOriginalFilename();
        String type = FileUtil.extName(originalFilename);
        long size = file.getSize();
        if (file.isEmpty() || size > 2 * 1024 * 1024 || type == null
                || !type.toLowerCase(java.util.Locale.ROOT).matches("jpg|jpeg|png|webp"))
            throw new com.example.springboot.exception.ServiceException("400", "仅支持2MB以内的 JPG、PNG、WEBP 头像");

        //存储到磁盘
        File uploadParentFile = new File(fileUploadPath);

        //判断配置的文件目录是否存在，若不存在则创建新的文件目录
        if(!uploadParentFile.exists()) {
            java.nio.file.Files.createDirectories(uploadParentFile.toPath());
        }

        //定义一个文件唯一的标识码
        String fileUuid = IdUtil.fastSimpleUUID() + StrUtil.DOT + type;
        File uploadFile = new File(fileUploadPath, fileUuid);

        //获取文件的md5值，通过对比md5避免上传相同内容的文件
        String md5;
        try (java.io.InputStream input = file.getInputStream()) {
            md5 = SecureUtil.md5(input);
        }

        String url = null;

        //按照md5值在数据库中查找文件
        MyFile dbFile = getFileByMd5(md5);
        if (dbFile != null) {
            //若数据库中有该文件，直接将已有文件的url赋给新上传文件
            url = dbFile.getUrl();
        } else {
            //若数据库中没有该文件，则将获取到的文件存储至磁盘目录并生成url
            file.transferTo(uploadFile);
            url = publicUrl.replaceAll("/$", "") + "/" + fileUuid;
        }

        //存储文件信息至数据库
        MyFile myFile = new MyFile();
        myFile.setName(originalFilename);
        myFile.setType(type);
        myFile.setSize(size/1024);

        myFile.setUrl(url);
        myFile.setMd5(md5);
        fileMapper.insert(myFile);
        return url;
    }

    /**
     * 文件下载接口 http://localhost:8081/{fileUuid}
     * @param fileUuid
     * @param response
     * @throws IOException
     */
    @GetMapping("/{fileUuid}")
    public void download(@PathVariable String fileUuid,
                         HttpServletResponse response) throws IOException {
        if (!fileUuid.matches("[a-f0-9]{32}\\.[A-Za-z0-9]+")) {
            response.sendError(400); return;
        }
        //根据文件的唯一标识码获取文件
        File uploadFile = new File(fileUploadPath, fileUuid);
        if (!uploadFile.isFile()) { response.sendError(404); return; }
        //设置输出流格式
        ServletOutputStream stream = response.getOutputStream();
        response.addHeader("Content-Disposition",
                "attachment; filename = " + URLEncoder.encode(fileUuid, "UTF-8"));
        response.setContentType("application/octet-stream");

        //读取文件的字节流
        java.nio.file.Files.copy(uploadFile.toPath(), stream);
        stream.flush();
        stream.close();
    }

    /**
     * 按照md5查找文件
     * @param md5
     * @return
     */
    private MyFile getFileByMd5(String md5) {
        QueryWrapper<MyFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5", md5);
        List<MyFile> fileList = fileMapper.selectList(queryWrapper);
        return fileList.size() == 0 ? null : fileList.get(0);
    }
}
