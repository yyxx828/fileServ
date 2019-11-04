package com.xajiusuo.busi.file.contain;

import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 杨勇 on 19-7-24.
 * 文件保存工具
 */
public class FileSaveUtil {

    /***
     * 完整文件上传
     * @param proName 项目名称
     * @param type 文件类型
     * @param file 上传的文件
     * @return
     */
    public static FileResBean uploadWholeFile(String proName, FileRoot.Type type, MultipartFile file){
        FileSysContent content = FileSysAgent.getContent(file.getSize());
        return content.saveFile(proName,type,file);
    }

    /***
     * 完整文件上传
     * @param proName 项目名称
     * @param type 文件类型
     * @param file 上传的文件
     * @return
     */
    public static FileResBean uploadWholeFile(String proName, FileRoot.Type type, File file){
        FileSysContent content = FileSysAgent.getContent(file.length());
        return content.saveFile(proName,type,file);
    }

    /***
     * 片段文件上传
     * @param proName 项目名称
     * @param type 文件类型
     * @param file 上传的文件
     * @param fileName
     * @param md5 @return
     * @param allSize 文件整体大小     */
    public static FileResBean uploadPartFile(String proName, FileRoot.Type type, MultipartFile file, long pos, String fileName, String md5, long allSize){
        if(pos == 0 && StringUtils.isNotBlank(fileName)){
            throw new RuntimeException("上传文件名非后台定义.首次上传文件名[fileName]必须为空");
        }
        if(pos != 0 && StringUtils.isBlank(fileName)){
            throw new RuntimeException("续传文件名[fileName]不能为空");
        }
        //判断是否存在正在上传的文件
        Md5OutContain.Out out = Md5OutContain.out(fileName,md5,allSize);//进行获取文件输出流,并不进行文件保存操作
        if(!(out.toMd5(out.getFile().getName(),md5, proName, type, file.getOriginalFilename(), allSize))){//临时文件转md5名称文件,存在秒传文件返回mf,判断磁盘大小
            out.write(file, pos, allSize, proName, type, md5);//文件系统容器进行硬链接创建并md5全保存
        }

        return FileResBean.to(() -> {
            Map<String,Object> m = new HashMap();
            m.put("fileName",out.getFile().getName());
            m.put("md5",out.getMd5());
            m.put("pos",pos + file.getSize());
            m.put("finsh", out.finsh);
            if(out.finsh){
                m.put("message","上传完成");
                m.put("size",out.getFile().length());
                m.put("pos",out.getFile().length());
                m.put("proName",proName);
                m.put("type",type.path);
                m.put("contentType", MD5FileUtil.getContentType(out.getFile()));
            }
            return m;
        });
    }

}