package com.xajiusuo.busi.file.contain;

import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * md5字典日志流
 * Created by 杨勇 on 19-7-23.
 */
@Component
public class Md5OutContain {

    private static Map<String,Out> m = Collections.synchronizedMap(new HashMap(50));

    private Md5OutContain(){
    }

    public static void remove(Out o){
        if(o != null){
            o.close();
            m.values().remove(o);
        }
    }

    /***
     * 取文件对应的md5字典输出流
     * @param file 实际数据文件
     * @param mf 文件目录类
     * @param route 文件
     * @return
     */
    public static Out out(File file, FileRoot.Md5File mf, FileRoot route){
        synchronized (m){
            String k = mf.getProName() + mf.getType().path + file.getParentFile().getName();
            Out o = m.get(k);
            if(o == null || (o.out == null && o.out1 == null)){
                try {
                    m.put(k,o = new Out(route.findMd5(file,mf)));
                } catch (Exception e) {
                    throw  new RuntimeException(e);
                }
            }
            o.optTime = System.currentTimeMillis();
            return o;
        }
    }

    /***
     * 获取输出流
     * @param name
     * @param md5
     * @param allSize
     * @return
     */
    public static Out out(String name, String md5, long allSize){
        synchronized (m){
            String md51 = "md5_" + md5 + ".bat";
            Out o = m.get(md51);
            if(o == null){//获取上次上传文件
                if(StringUtils.isNotBlank(name)){
                    o = m.get(name);
                }
            }

            if(o == null){//首次上传进行文件初始
                FileSysContent content = FileSysAgent.getContent(allSize);//获取足够大磁盘
                File fileMd5 = null;
                File fileTmp = null;
                if(StringUtils.isNotBlank(md5)){
                    fileMd5 = content.root.pathUpload(md51);
                }
                if(StringUtils.isBlank(name)){
                    name = "temp_" + content.fn(null) + ".bat";
                }
                fileTmp = content.root.pathUpload(name);
                try {
                    o = new Out((fileMd5 != null && fileMd5.exists()) ? fileMd5 : fileTmp);
                    o.content = content;
                    o.fileSize = allSize;
                    if(o.file != fileTmp){//md5文件存在
                        m.put(md51,o);
                        o.md5 = md5;
                    }else{
                        m.put(name,o);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            o.optTime = System.currentTimeMillis();
            return o;
        }
    }

    public static Out close(String k){
        synchronized (m){
            Out o = m.get(k);
            if(o != null){
                o.close();
                return o;
            }
            return null;
        }
    }

    public static void cancel(String k){
        Out o = close(k);
        if(o == null){
            throw new RuntimeException("该文件未在上传列表中");
        }
        o.content.useing.remove(o.fileSize);
        o.file.delete();
        m.values().remove(o);
    }

    public static void closeAll(){
        new ArrayList<String>(m.keySet()).stream().forEach(Md5OutContain::close);
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    private void clear(){
        new ArrayList<String>(m.keySet()).stream().filter(k -> System.currentTimeMillis() - m.get(k).optTime >= 10 * 60 * 1000).forEach(Md5OutContain::close);
    }

    public static class Out{
        private long optTime = System.currentTimeMillis();
        private PrintWriter out;
        private OutputStream out1;

        FileSysContent content;
        private File file;
        private String md5;
        boolean finsh = false;
        private long fileSize;


        Out(File file){
            this.file = file;
        }

        void close(){
            MD5FileUtil.close(out,out1);
            out = null;
            out1 = null;
        }

        boolean toMd5(String name, String md5, String proName, FileRoot.Type type, String realName, long allSize){
            synchronized (this){
                if(StringUtils.isBlank(this.md5) && StringUtils.isNotBlank(md5)){//对md5进行
                    FileRoot.Md5File mf = new FileRoot.Md5File(proName, FileRoot.Home.FILE,type,new File(realName));
                    boolean res = FileSysAgent.saveLink(md5, mf);//文件系统容器进行硬链接创建
                    if(res){//传入md5后发现有该文件
                        this.file.delete();//删除残留文件 恢复大小
                        this.file = mf.f;
                        this.finsh = true;
                        this.md5 = md5;
                        content.useing.remove(allSize);
                        remove(this);
                        return true;
                    }
                    this.md5 = md5;
                    String md51 = "md5_" + md5 + ".bat";
                    m.put(md51,m.remove(name));
                    File f = content.root.pathUpload(md51);//获取md5文件
                    close();
                    if(f.exists()){
                        file.delete();
                    }else{
                        file.renameTo(f);
                    }
                    file = f;
                }
                return false;
            }
        }

        private OutputStream getOutPutStream() {
            synchronized (this){
                if(out != null){
                    throw new RuntimeException("该文件已被字符流占用");
                }
                if(out1 == null){
                    try {
                        out1 = new FileOutputStream(file,true);
                    } catch (Exception e) {
                        out1 = null;
                        throw new RuntimeException(e);
                    }
                }
                return out1;
            }
        }

        private PrintWriter getWrite() {
            synchronized (this){
                if(out1 != null){
                    throw new RuntimeException("该文件已被字节流占用");
                }
                if(out == null){
                    try {
                        out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file,true),"utf-8"));
                    } catch (Exception e) {
                        out = null;
                        throw new RuntimeException(e);
                    }
                }
                return out;
            }
        }

        public void println(String str){
            PrintWriter out = getWrite();
            synchronized (out){
                out.print(str + "\r\n");
                out.flush();
            }
        }

        /***
         * 写入文件片段.
         * @param file 待上传文件片段或整个文件
         * @param pos 文件上传起始标记
         * @param allSize 整体文件大小
         * @param proName 项目名称
         * @param type 文件存储类型
         * @param md5 文件对应md5
         * @return
         */
        public boolean write(MultipartFile file, long pos, long allSize, String proName, FileRoot.Type type, String md5){
            OutputStream out = getOutPutStream();
            synchronized (out){
                try {
                    if(pos == this.file.length()){//正常文件上传片段或首次上传
                        MD5FileUtil.copyf(file.getInputStream(),out);
                        out.flush();
                        if(allSize == this.file.length()){//上传完成
                            String md51 = MD5FileUtil.getFileMD5String(this.file);
                            content.useing.remove(allSize);
                            if(StringUtils.isNotBlank(md5) && !md51.equals(md5)){
                                this.file.delete();
                                remove(this);
                                throw new RuntimeException("文件md5校验异常,请重新上传");
                            }
                            this.md5 = md51;

                            FileRoot.Md5File mf = new FileRoot.Md5File(proName, FileRoot.Home.FILE,type,this.file);
                            File f0 = content.root.pathFile(FileSysContent.fn(file.getOriginalFilename()),mf);
                            this.file.renameTo(f0);
                            mf.f = f0;
                            if(!FileSysAgent.saveLink(this.md5, mf)){//文件系统容器进行硬链接创建,失败则表示无该文件
                                content.logMd5(this.md5,null,mf);
                                content.avail -= allSize;
                            }
                            this.file = mf.f;
                            this.finsh = true;
                            remove(this);
                            return true;
                        }else if(allSize < this.file.length()){
                            content.useing.remove(allSize);
                            this.file.delete();
                            remove(this);
                            throw new RuntimeException("文件大小异常,请重新上传");
                        }
                    }else if(pos > this.file.length() || pos < 0) {//单次小于1M
                        content.useing.remove(allSize);
                        remove(this);
                        this.file.delete();
                        throw new RuntimeException("文件上传片段异常");
                    }else{//
                        //文件校验
                        FileInputStream in = new FileInputStream(this.file);
                        in.skip(pos);
                        byte[] buff = new byte[(int) file.getSize()];
                        int len = 0;
                        while (len < file.getSize()){
                            len += in.read(buff,len, (int) (file.getSize() - len));
                        }
                        in.close();
                        String md51 = MD5FileUtil.getFileMD5String(file.getInputStream());
                        String md52 = MD5FileUtil.getMD5String(buff);
                        if(!md51.equals(md52)){
                            content.useing.remove(allSize);
                            this.file.delete();
                            remove(this);
                            throw new RuntimeException("断点上传文件校验失败.");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        public File getFile(){
            return file;
        }

        public String getMd5(){
            return md5;
        }

    }
}
