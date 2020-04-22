package com.leyou.upload.service;


import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Service
public class UploadService {

    private static final List<String> CONTENT_TYPE = Arrays.asList("image/jpeg","image/x-icon","image/gif");

    //日志信息
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);

    //上传客户端
    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {

        //获取上传文件的原始名称
        String originalFilename = file.getOriginalFilename();

        try {
            //校验上传的文件类型
            //获取文件的媒体类型
            String contentType = file.getContentType();
            if (!CONTENT_TYPE.contains(contentType)){
                LOGGER.error("文件类型不合法: {}"+originalFilename);
                return null;
            }
            //校验上传文件的内容
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream()); //得到二进制图片
            if (bufferedImage == null){
                LOGGER.info("文件内容不合法: {}"+originalFilename);
                return null;
            }

            //保存到服务器(重点记住)(域名加图片原始名称就能访问图片)
            //file.transferTo(new File("D:\\feiqiu\\img\\"+originalFilename));

            //返回url进行回显
            //return "http://image.leyou.com/"+originalFilename;

            //通过storageclient把图片上传到fastDFS
            String ext = StringUtils.substringAfterLast(originalFilename, ".");//后缀名
            StorePath storePath = this.storageClient.uploadFile(
                    file.getInputStream(), file.getSize(), ext,  null);
            //返回url进行回显
            return "http://image.leyou.com/"+storePath.getFullPath();//完整路径

        } catch (IOException e) {
            LOGGER.info("服务器累了，请待会儿再试!"+originalFilename);
            e.printStackTrace();
        }
        return null;
    }
}
