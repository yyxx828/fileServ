package com.xajiusuo.busi.file.contain;

import com.xajiusuo.busi.file.base.SelectedVo;
import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by 杨勇 on 19-7-15.
 * 文件系统容器
 */
@Component
public class FileSysAgent {

    private final static List<FileSysContent> dirsList = new ArrayList();//目录容器
    private final static Set<String> parent = new HashSet();//挂载路径例如 linux:/home,window:D:\\

    private static boolean loadMd5 = false;//加载MD5
    private static boolean merge = false;//MD5合并
    private static boolean back = false;//文件备份
    static boolean cycle = false;

    @Value("${file.delete.record.retainDay}")
    Integer recoredRetainDays;

    @Value("${file.admin.accout}")
    String admin;

    @Value("${file.admin.password}")
    String password;

    final static int tempRetainDays = 7;


/////////////--------------------------------------系统方法--------------------------------------------------////////////
    /***
     * 初始文件系统,对于挂载同一文件系统进行忽略
     * 读取文件系统的可用大小/总大小
     */
    public static void loadPath(String path) throws IOException {
        MD5FileUtil.deleteChildren(new File(path, FileRoot.Home.UPLOADING.path));//清理未上传文件
        if(System.getProperty("os.name").startsWith("Linux")){//linux
            Process p = Runtime.getRuntime().exec("df " + path);
            BufferedReader in = new BufferedReader(new InputStreamReader( p.getInputStream()));
            in.readLine();
            String line = in.readLine();
            in.close();

            if(StringUtils.isNotBlank(line)){
                String[] ss = line.split(" +");
                if(parent.contains(ss[5])){
                    System.err.println("加载跳过:\t" + path);
                    return;
                }
                parent.add(ss[5]);
                dirsList.add(new FileSysContent(Long.parseLong(ss[1]) * 1024,Long.parseLong(ss[2]) * 1024, path, "磁盘" + (dirsList.size() + 1)));
            }
        }else if(System.getProperty("os.name").startsWith("Windows")){//window
            if(parent.contains(path.substring(0,path.indexOf(":") + 1))){
                System.err.println("加载跳过:\t" + path);
                return;
            }
            parent.add(path.substring(0,path.indexOf(":") + 1));
            Process p = Runtime.getRuntime().exec("fsutil volume diskfree " + path);
            BufferedReader in = new BufferedReader(new InputStreamReader( p.getInputStream(),"GBK"));
            String l1 = in.readLine();
            String l2 = in.readLine();
            in.close();

            if(StringUtils.isNotBlank(l1) && StringUtils.isNotBlank(l2)){
                dirsList.add(new FileSysContent(Long.parseLong(l1.split(":")[1].trim()),Long.parseLong(l2.split(":")[1].trim()), path, "磁盘" + (dirsList.size() + 1)));
            }
        }
    }

    /***
     * 初始加载
     */
    public static void init(){
        initMergeMd5();//合并md5
        initLoadMd5();//加载md5字典
    }

    /***
     * 文件备份
     */
    public static boolean back(){
        if(isSuspend()) return false;
        back = true;
        Md5OutContain.closeAll();
        dirsList.forEach(FileSysContent::back);
        back = false;
        return true;
    }

    /***
     * 文件还原
     */
    public static boolean restore(){
        if(isSuspend()) return false;
        loadMd5 = true;
        dirsList.forEach(FileSysContent::restore);
        loadMd5 = false;
        return true;
    }


/////////////--------------------------------------初始方法--------------------------------------------------////////////

    /***
     * 合并相同MD5字典
     */
    static void initMergeMd5(){
        merge = true;
        dirsList.forEach(FileSysContent::initMergeMd5);
        merge = false;
    }


    /***
     * 加载各自盘符的md5字典,并且进行游离文件md5补充
     */
    static void initLoadMd5() {
        loadMd5 = true;
        dirsList.forEach(FileSysContent::initLoadMd5);//加载
        try {Thread.sleep(100);} catch (Exception e) {}
        dirsList.forEach(FileSysContent::initCompletionMd5); //对于游离文件,进行md5字典补充
        while(FileSysContent.service.getActiveCount() > 0){//等待线程完成
            try {Thread.sleep(1000);} catch (Exception e) {}
        }
        loadMd5 = false;
    }


/////////////--------------------------------------实用方法--------------------------------------------------////////////
    /***
     * 进行查找后保存,若成功则返回true,并进行md5全保存
     * @param md5
     * @param mf 要保存的文件
     * @return
     */
    public static boolean saveLink(String md5, FileRoot.Md5File mf){
        for (FileSysContent content : dirsList) {
            if(content.saveLink(md5, mf)){
                return true;
            }
        }
        return false;
    }


    /***
     * 文件复制
     * @param systemId
     * @param type
     * @param fileName
     *@param toType  @return
     */
    public static FileResBean copyPath(String systemId, String type, String date, String fileName, String toType) {
        for (FileSysContent content : dirsList) {
            FileResBean bean = content.copyPath(systemId, FileRoot.Type.valueOf(type.toUpperCase()),date,fileName , FileRoot.Type.valueOf(toType.toUpperCase()));
            if(bean != null){
                return bean;
            }
        }
        throw new RuntimeException("文件系统无该文件信息.");
    }

    public static File findPath(String systemId, String type, String date, String fileName) {
        for (FileSysContent content : dirsList) {
            File file = content.findPath(systemId, FileRoot.Type.valueOf(type.toUpperCase()), date, fileName);
            if(file != null && file.exists()){
                return file;
            }
        }
        throw new RuntimeException("文件系统无该文件信息.");
    }

    /***
     * 磁盘信息获取
     * @return
     */
    public static FileResBean sysInfo() {
        Map map = new HashMap<>();
        for (FileSysContent content : dirsList) {
            map.put(content.panName,content.sysInfo());
        }
        map.put("message","查询成功");
        return FileResBean.to(() -> map);
    }

    /***
     * 文件系统树
     * @return
     */
    public static FileResBean sysTree() {
        Map m = new HashMap<>(10);
        SelectedVo vo = new SelectedVo();
        vo.setLabel("总盘符");
        for (FileSysContent content : dirsList) {
            vo.getChildren().add(content.sysTree());
        }
        m.put("message","查询成功");
        m.put("tree",vo);
        return FileResBean.to(() -> m);
    }


    /***
     * 获取文件树节点但文件信息
     * @param index
     * @param project
     * @param type
     * @param date
     * @return
     */
    public static FileResBean treeList(int index, String project, String type, String date) {
        File file = dirsList.get(index).root.rootFileDefine(FileRoot.Home.FILE,project,type, FileRoot.Content.DATA,date);
        List list = new ArrayList<>();
        if(file.isDirectory()){
            for(File f:file.listFiles()){
                SelectedVo v = new SelectedVo();
                v.setLabel(f.getName());
                v.setValue(type + "/" + date + "/" + f.getName() + "?systemId=" + project);
                list.add(v);
            }
        }
        return FileResBean.to(() -> list);
    }

    public synchronized static int deleteRecord(String systemId, Integer retainDay) {
        int size = 0;
        for (FileSysContent content : dirsList) {
            size += content.deleteRecord(systemId,retainDay);
        }
        return size;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void deleteFile(){
        //临时文件删除
        for (FileSysContent content : dirsList) {
            content.deleteFile(null, FileRoot.Type.TEMP,tempRetainDays);
        }

        //记录文件删除
        if(recoredRetainDays == null || recoredRetainDays == -1){
            return;
        }
        if(recoredRetainDays < 30){
            recoredRetainDays = 90;
        }
        deleteRecord(null, recoredRetainDays);
    }

    /***
     * 上传是否暂停,true暂停,false可用
     * @return
     */
    public static boolean isSuspend(){
        return loadMd5 || merge || back;
    }


    /***
     * 获取一个文件系统进行下载
     * @param size
     * @return
     */
    static FileSysContent getContent(long size) {
        if(dirsList.size() == 1){
            FileSysContent content = dirsList.get(0);
            if(content.hasUser(size)){
                content.useing.add(size);
                return content;
            }
        }else{
            List<FileSysContent> list = dirsList.stream().filter(c -> c.hasUser(size)).collect(Collectors.toList());
            if(list.size() > 0) {
                FileSysContent content = list.get(0);
                if(cycle){//文件系统随机存储
                    content = list.get(new Random(System.currentTimeMillis()).nextInt(list.size()));
                }
                content.useing.add(size);
                return content;
            }
        }
        throw new RuntimeException("File system disk space not enough,please expand and proceed");
    }

    public void registerUser(String account, String mm) {
        if(admin.equals(account)){
            throw new RuntimeException("管理帐号不能进行注册");
        }
        dirsList.get(0).registerUser(account,mm, false);
    }



    public boolean login(String user,String pwd){
        if(admin.equals(user)){
            if(password.equals(pwd)){
                return true;
            }else{
                throw new RuntimeException("管理员密码错误");
            }
        }
        return dirsList.get(0).login(user,pwd);
    }

    public boolean updatePassword(String account, String pwd, String pwd1) {
        if(admin.equals(account)){
            throw new RuntimeException("管理员帐号无法修改秘密");
        }
        if(StringUtils.isBlank(pwd) || StringUtils.isBlank(pwd1)){
            throw new RuntimeException("密码不能为空");
        }
        if(pwd.equals(pwd1)){
            throw new RuntimeException("两次密码不能相同");
        }
        pwd = MD5FileUtil.pwdMd5(pwd);
        pwd1 = MD5FileUtil.pwdMd5(pwd1);
        if(pwd.equals(dirsList.get(0).readPwd(account))){
            dirsList.get(0).registerUser(account,pwd1,true);
            return true;
        }
        return false;
    }
}
