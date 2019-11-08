package com.xajiusuo.busi.file.contain;

import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件系统根路径
 * Created by 杨勇 on 19-7-16.
 */
@FunctionalInterface
public interface FileRoot {

    SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String root();  //根绝对路径

    /***
     * 获取 Home 下 Content 所有路径
     *
     * @param home 对应Home路径
     * @param type
     *@param content 对应Content路径  @return
     */
    default List<Md5File> paths(Home home, Type type, Content content){
        File rootFileDir = rootFileParentCreate(home);
        if(!rootFileDir.isDirectory() || rootFileDir.list().length == 0){
            return Collections.emptyList();
        }
        List<Md5File> md5FileList = new ArrayList(rootFileDir.list().length * Type.values().length);
        for(File f:rootFileDir.listFiles()){//下一级为项目名称
            for (Type type1 : Type.values()) {
                if(type != null && type != type1)
                    continue;
                File md5Dir = fileParentCreate(f,type1,content);
                if(md5Dir.isDirectory() && md5Dir.list().length > 0){
                    md5FileList.add(new Md5File(f.getName(), home, type1,md5Dir));
                }
            }
        }
        return md5FileList;
    }

    /***
     * 备份目录创建
     * @return
     */
    default File pathBackCreate(){
        //新建目录
        File backDir = rootFileParentCreate(Home.BACK);
        MD5FileUtil.deleteChildren(backDir);
        dirCreate(backDir,Content.DATA);
        dirCreate(backDir,Content.MD5);
        return backDir;
    }

    /***
     * 获取一级总目录
     * @param home
     * @return
     */
    default File path(Home home){
        return rootFileParentCreate(home);
    }


    /***
     * 数据文件的绝对路径
     * @param fileName 文件名
     * @param mf 文件类型
     * @param date 文件日期yyyy-MM-dd
     * @return
     */
    default File pathFile(String fileName, Md5File mf, String date){
        return rootFileParentCreate(mf.getHome(),mf.getProName(),mf.getType(),Content.DATA,date,fileName);
    }


    /***
     * 数据文件的绝对路径
     * @param fileName 文件名
     * @param mf 文件类型
     * @return
     */
    default File pathFile(String fileName, Md5File mf){
        return pathFile(fileName,mf,ymd.format(new Date(Long.parseLong(fileName.substring(0,fileName.indexOf("_"))))));
    }

    default File pathUpload(String f){
        return rootFileParentCreate(Home.UPLOADING,f);
    }

    /***
     * 记录
     * @param md5
     * @param f
     * @param mf
     */
    default void logMd5(String md5, File f, Md5File mf){//获取日志流
        String time;
        try{
            time = ymdhms.format(new Date(Long.parseLong(f.getName().substring(0,f.getName().indexOf("_")))));
        }catch (Exception e){
            time= ymdhms.format(new Date());
        }
        Md5OutContain.out(f,mf, this).println(md5 + "\t" + f.getName() + "\t" + f.length() + "\t" + MD5FileUtil.getContentType(f) + "\t" + time);
    }

    default File findMd5(File f, Md5File mf){
        String d = f.getParentFile().getName();
        File file =  rootDirCreate(mf.getHome(),mf.getProName(),mf.getType(),Content.MD5);
        for(File f0:file.listFiles()){
            if(f0.getName().startsWith(d)){
                return f0;
            }
        }
        return fileCreate(file,d + "_" + StringUtils.random(3) + ".md5");
    }

///////////////////////////////////////////////文件目录路径方法///////////////////////////////////////////////////////////

    default File rootFileParentCreate(Object... a){
        return fileParentCreate(root(),a);
    }

    default File rootDirCreate(Object... a){
        return dirCreate(root(),a);
    }

    default File rootFileCreate(Object... a){
        return fileCreate(root(),a);
    }

    default File rootFileDefine(Object... a){
        return fileDefine(root(),a);
    }


    /***
     * 按照层级生成文件,创建上级目录
     *
     * @param o 支持File String Home Type Content 否则返回空
     * @param a 支持String Home Type Content
     * @return
     */
    static File fileParentCreate(Object o, Object... a){
        File f = fileDefine(o,a);
        if(f != null){
            f.getParentFile().mkdirs();
        }
        return f;
    }


    static File dirCreate(Object o, Object... a){
        File f = fileDefine(o,a);
        if(f != null){
            f.mkdirs();
        }
        return f;
    }

    static File fileCreate(Object o, Object... a){
        File f = fileParentCreate(o,a);
        if(f != null){
            try {
                f.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return f;
    }


    /***
     * 只定义文件/目录,不做创建
     * @param o
     * @param a
     * @return
     */
    static File fileDefine(Object o, Object... a){
        File f;
        if(o instanceof String){
            f = new File((String)o);
        }else if(o instanceof File){
            f = (File) o;
        }else if(o instanceof Path){
            f = new File(((Path) o).getPath());
        }else {
            return null;
        }
        for (Object o1 : a) {
            if(o1 instanceof String){
                f = new File(f,(String)o1);
            }else if(o1 instanceof Path){
                f = new File(f,((Path) o1).getPath());
            }
        }
        return f;
    }

/////////////--------------------------------------路径定义--------------------------------------------------////////////

    interface Path{
        String getPath();
    }

    //根目录下路径
    enum Home implements Path{
        FILE(),//总路径
        LOG(),//总日志
        BACK(),//备份
        USER(),//用户信息
        UPLOADING();//上传中
        String path;
        Home(){
            this.path = this.name().toLowerCase();
        }

        @Override
        public String getPath() {
            return path;
        }
    }

    /***
     * @see Home 中FILE项目分类下文件夹
     */
    enum Type implements Path{
        STATIC(),//永久文件
        RECORD(),//三个月
        TEMP();//一天
        String path;
        Type(){
            this.path = this.name().toLowerCase();
        }
        @Override
        public String getPath() {
            return path;
        }
    }

    /***
     * @see Type 对应但两个文件,其中data存放具体文件内容
     */
    enum Content implements Path{
        MD5(),//MD5字典路径
        DATA();//文件存放路径
        String path;
        Content(){
            this.path = this.name().toLowerCase();
        }
        @Override
        public String getPath() {
            return path;
        }
    }


/////////////--------------------------------------分类定义--------------------------------------------------////////////

    class Md5File{

        private MF mf;//文件属类

        File f;//对应文件

        private static Map<String,MF> m = Collections.synchronizedMap(new HashMap(200));

        public Md5File(String proName, Home home, Type type, File file){
            String k = proName + home.path + type.path;
            mf = m.get(k);
            if(mf == null){
                mf = new MF();
                mf.pn = proName;
                mf.h = home;
                mf.y = type;
                m.put(k, mf);
            }
            this.f = file;
        }

        public String getProName() {
            return mf.pn;
        }

        public Home getHome() {
            return mf.h;
        }

        public Type getType() {
            return mf.y;
        }

        public File getFile() {
            return f;
        }

        class MF{//文件分类
            private String pn;//项目名称
            private Home h;//第一目录
            private Type y;//md5/data
        }

    }

}