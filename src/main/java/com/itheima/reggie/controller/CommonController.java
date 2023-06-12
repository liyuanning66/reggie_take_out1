package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件的上传和下载
 */
@RestController
@Slf4j
@RequestMapping("/common")
public class CommonController {
    @Value("${reggie.path}")
    private String basePath;
    /**
     * 文件上传
     * @param file
     * @return
     */
   @PostMapping("/upload")
   public R<String> upload(MultipartFile file){//注意这里的参数名字file不能随便写要与前端参数保持一致
       //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
       log.info(file.toString());

       //使用原始文件名后缀
       String originalFilename = file.getOriginalFilename();
       String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

       //使用UUID重新生成文件名吗，防止文件名称重复造成文件覆盖
       //这样写代码就很死板，所以这样写就不好，最好需要代码具有动态性
//       String fileName = UUID.randomUUID().toString() + ".jpg";
       String fileName = UUID.randomUUID().toString() + suffix;

       /**
        * 这里我们还需要进行判断，当前目录文件是否存在
        */
       //首先创建一个文件对象
       File dir = new File(basePath);
       if(!dir.exists()){
           //目录不存在，需要创建
           dir.mkdirs();
       }
       try {
           //我们通过放到配置文件里面来增加代码的灵活性
           file.transferTo(new File(basePath + fileName));
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
       return R.success(fileName);
   }

    /**
     * 文件下载
     * @param name
     * @param response
     */
   @GetMapping("/download")
   public void download(String name, HttpServletResponse response){
       try {
           //输入流，通过输入流读取文件内容
           FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));

           //输出流，通过输出流将文件写回浏览器，在浏览器展示图片
           ServletOutputStream outputStream = response.getOutputStream();

           response.setContentType("image/jpeg");
           int len = 0;
           byte[] bytes = new byte[1024];
           while((len = fileInputStream.read(bytes)) != -1){
               outputStream.write(bytes, 0, len);
               outputStream.flush();
           }
           //关闭资源
           outputStream.close();
           fileInputStream.close();
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
   }
}
