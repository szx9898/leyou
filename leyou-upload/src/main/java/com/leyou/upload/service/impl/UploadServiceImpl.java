package com.leyou.upload.service.impl;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.upload.service.UploadService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadServiceImpl implements UploadService {
    private static final List<String> CONTENT_TYPES = Arrays.asList("image/gif", "image/jpeg");
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);
    @Autowired
    private FastFileStorageClient fileStorageClient;

    @Override
    public String uploadImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        //检验文件类型
        String contentType = file.getContentType();
        if (!CONTENT_TYPES.contains(contentType)) {
            LOGGER.info("文件类型不合法:{}", originalFilename);
            return null;
        }
        try {
            //检验文件内容
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                LOGGER.info("文件内容不合法:{}", originalFilename);
                return null;
            }
            //保存到服务器
            //file.transferTo(new File("D:\\leyoupics\\"+originalFilename));
            String ext = StringUtils.substringAfterLast(originalFilename, ".");
            StorePath storePath = fileStorageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);

            //返回url，进行回显
            //return "http://image.leyou.com/"+originalFilename;
            return "http://image.leyou.com/"+storePath.getFullPath();
        } catch (IOException e) {
            LOGGER.info("服务器内部错误:"+originalFilename);
            e.printStackTrace();
        }
        return null;
    }
}
