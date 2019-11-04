package com.xajiusuo.busi.file.contain;

import com.xajiusuo.busi.file.base.SelectedVo;
import com.xajiusuo.utils.MD5FileUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传返回结果
 * Created by 杨勇 on 19-7-29.
 */
@FunctionalInterface
public interface FileResBean {

    static FileResBean to(FileResBean bean){
        return bean;
    }

    static FileResBean to(String md5,FileRoot.Md5File mf){
        return to(() -> {
            Map<String,Object> m = new HashMap();
            m.put("message","上传完成");
            m.put("fileName",mf.getFile().getName());
            m.put("md5",md5);
            m.put("fileSize",mf.getFile().length());
            m.put("proName",mf.getProName());
            m.put("type",mf.getType().path);
            m.put("filePath",mf.getType().path + "/" + mf.getFile().getParentFile().getName() + "/" + mf.getFile().getName());
            m.put("contentType", MD5FileUtil.getContentType(mf.getFile()));
            return m;
        });
    }

    static FileResBean to(String str){
        return to(() -> str);
    }

    //map 或 string
    Object obj();

    default int getStatusCode(){
        if(obj() instanceof Map){
            Object code = ((Map) obj()).get("statusCode");
            if(code instanceof Number){
                return ((Number) code).intValue();
            }
            return 200;
        }else if(obj() != null && !(obj() instanceof String)){
            return 200;
        }else{
            return 500;
        }
    }

    default String getMessage(){
        if(obj() instanceof Map){
            return (String) ((Map) obj()).get("message");
        }
        if(obj() != null && !(obj() instanceof String)){
            return "查询成功";
        }
        if(obj() instanceof String){
            return (String)obj();
        }
        return getStatusCode() == 200 ? "上传成功" : "上传失败";
    }

    default Object getData(){
        if(obj() instanceof Map){
            Map m = new HashMap((Map) obj());
            m.remove("message");
            m.remove("statusCode");
            return m;
        }
        if(obj() != null && !(obj() instanceof String)){
            return obj();
        }
        return Collections.emptyMap();
    }

}
