package com.xajiusuo.busi.file.contain;

import com.xajiusuo.busi.file.base.SelectedVo;
import com.xajiusuo.utils.ColorUtil;
import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by 杨勇 on 19-7-15.
 * 文件系统管理
 */
public class FileSysContent {

    final static int max = 300000;//单个md5字典总容量
    final static int max_ = ((Double)(max / 0.7499)).intValue();//定义大小
    final static String back = "back.md5";//备份md5文件名
    final static SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");

    static final ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    FileRoot root;//该文件系统但路由相关

    List<Map<String,String>> md5Maps = Collections.synchronizedList(new ArrayList());//每个文件库的md5字典,一条md5文件大概50B,1K可以存放10条存满30万,大概需要36M内存

    String panName;//盘符名称
    long allSize;//总大小
    long avail;//可用大小

    List<Long> useing = Collections.synchronizedList(new ArrayList(50));

    public FileSysContent(long max, long min, String path, String panName){
        allSize = Math.max(max,min);
        avail = Math.min(max,min);
        useing.add(50l * 1024 * 1024);
        this.panName = panName;
        setRoot(() -> {return path;});
    }

    void initMergeMd5() {//初始合并相同时间md5字典
        List<FileRoot.Md5File> mdsFileList = root.paths(FileRoot.Home.FILE, null, FileRoot.Content.MD5);
        Map<String,File> writerMap = new HashMap((int) (500 / 0.7455));
        for(FileRoot.Md5File f1:mdsFileList){
            for(File f:f1.getFile().listFiles()){
                String date = f.getName().substring(0,10);
                if(writerMap.get(date) != null){
                    MD5FileUtil.append(f,writerMap.get(date));
                    f.delete();
                }else{
                    writerMap.put(date,f);
                }
            }
            writerMap.clear();
        }
    }

    List<String> files = new LinkedList();
    void initLoadMd5(){//读取文件MD5字典
        service.execute(() -> {
            loadMd5();
        });
    }

    void loadMd5(){//读取文件MD5字典
        synchronized (this){
            clear();
            System.out.println(ColorUtil.green(root.root()) +  ColorUtil.blue("\tloadMd5 begin"));
            List<FileRoot.Md5File> md5FileList = root.paths(FileRoot.Home.FILE, null, FileRoot.Content.MD5);
            for(FileRoot.Md5File mf:md5FileList){
                for(File f:mf.getFile().listFiles()){
                    BufferedReader in = null;
                    try {
                        readMd5(in = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8")), mf, f);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        MD5FileUtil.close(in);
                    }
                }
            }
            System.out.println(ColorUtil.green(root.root()) +  ColorUtil.blue("\tloadMd5 end"));
        }
    }

    void initCompletionMd5() {//对游离文件补全md5字典
        service.execute(() -> {
            synchronized (this) {
                System.out.println(ColorUtil.green(root.root()) +  ColorUtil.blue("\tinitCompletionMd5 begin"));
                List<FileRoot.Md5File> md5FileList = root.paths(FileRoot.Home.FILE, null, FileRoot.Content.DATA);//获取所有但文件路径
                for(FileRoot.Md5File mf:md5FileList){
                    for(File f:mf.getFile().listFiles()){
                        loop:for(File file:f.listFiles()){
                            String path = file.getAbsolutePath().substring(root.root().length());
                            if(files.contains(path))
                                continue;
                            for (Map<String, String> stringStringMap : md5Maps) {//U
                                if(stringStringMap.values().contains(path))
                                    continue loop;
                            }
                            logMd5(file,mf);//记录md5字典
                        }
                    }
                }
                files.clear();
                System.out.println(ColorUtil.green(root.root()) +  ColorUtil.blue("\tinitCompletionMd5 end"));
            }
        });
    }

    /***
     * 项目备份
     */
    void back() {
        synchronized (this){
            File dir = root.pathBackCreate();//获取备份路径
            File md5out = FileRoot.fileParentCreate(dir,back);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(md5out)));
                for (Map<String, String> map : md5Maps) {//U
                    for(String md5:map.keySet()){
                        String[] fs = map.get(md5).split(";");
                        File fr = new File(root.root(),fs[0]);
                        for (String f : fs) {
                            if((fr = new File(root.root(),f)).exists()) break;
                        }
                        if(fr.exists()){
                            String fn = fn(fr.getName());
                            FileCmd.createLink(fr, FileRoot.fileParentCreate(dir, FileRoot.Content.DATA,fn));
                            out.print(md5 + "\t" + fn + "\r\n");
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                MD5FileUtil.close(out);
            }
            List<FileRoot.Md5File> list = root.paths(FileRoot.Home.FILE, null, FileRoot.Content.MD5);
            for(FileRoot.Md5File mf:list){
                File bf = mf.getFile();//将md5字典按照原路径复制到back/md5下
                MD5FileUtil.copys(bf, FileRoot.fileParentCreate(dir, FileRoot.Content.MD5,bf.getParentFile().getParentFile().getName(),bf.getParentFile().getName(),bf.getName()));
            }
        }
    }

    /***
     * 项目还原
     */
    void restore(){
        synchronized (this){
            File dir = root.path(FileRoot.Home.BACK);//获取备份路径
            File md5File = FileRoot.fileParentCreate(dir, FileRoot.Content.MD5);
            if(md5File.isDirectory() && md5File.listFiles().length > 0){
                MD5FileUtil.copys(md5File, root.path(FileRoot.Home.FILE));//还原md5字典

                loadMd5();//加载字典

                //读取备份md5
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(FileRoot.fileParentCreate(dir,back))));
                    String line;
                    while((line = in.readLine()) != null){
                        String[] ss = line.split("\t");
                        for (Map<String, String> md5Map : md5Maps) {//U
                            String fs = md5Map.get(ss[0]);
                            File f0 = FileRoot.fileParentCreate(dir, FileRoot.Content.DATA,ss[1]);
                            if(f0.exists()){
                                String[] fss = fs.split(";");
                                List<File> list = new ArrayList(fss.length);
                                for(String s:fss){
                                    if(StringUtils.isNotBlank(s)){
                                        list.add(new File(root.root(),s));
                                    }
                                }
                                FileCmd.createLink(f0,list);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    MD5FileUtil.close(in);
                }
            }
            MD5FileUtil.delete(dir);
        }
    }

    public FileResBean saveFile(String proName, FileRoot.Type type, MultipartFile file) {
        File f = root.pathUpload(fn(file.getOriginalFilename()));
        try {
            //文件保存
            file.transferTo(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return saveFile(proName,type,f);
    }


    public FileResBean saveFile(String proName, FileRoot.Type type, File file) {
        //本文件系统查找
        String md5 = MD5FileUtil.getFileMD5String(file);
        FileRoot.Md5File mf = new FileRoot.Md5File(proName, FileRoot.Home.FILE,type,file);
        boolean res = FileSysAgent.saveLink(md5, mf);//文件系统容器进行硬链接创建
        useing.remove(file.length());
        if(!res){//无连接则进行文件内容保存
            File to = root.pathFile(fn(file.getName()),mf);
            to.getParentFile().mkdirs();
            file.renameTo(to);
            mf.f = to;
            logMd5(md5,to,mf);//md5全记录
            synchronized (this){
                avail -= to.length();
            }
        }

        return FileResBean.to(md5,mf);
    }



    public FileResBean copyPath(String systemId, FileRoot.Type type, String date, String fileName, FileRoot.Type toType) {
        if(StringUtils.isBlank(date)){
            date = ymd.format(new Date(Long.parseLong(fileName.substring(0,fileName.indexOf("_")))));
        }
        File f = findPath(systemId,type, date, fileName);
        if(f.exists()){
            String md5 = MD5FileUtil.getFileMD5String(f);
            FileRoot.Md5File mf = new FileRoot.Md5File(systemId, FileRoot.Home.FILE, toType,new File(f.getName()));
            if(saveLink(md5,mf)){
                return FileResBean.to(md5,mf);
            }
        }
        return null;
    }

    public File findPath(String systemId, FileRoot.Type type, String date, String fileName) {
        if(StringUtils.isBlank(date)){
            date = ymd.format(new Date(Long.parseLong(fileName.substring(0, fileName.indexOf("_")))));
        }
        return root.rootFileDefine(FileRoot.Home.FILE,systemId,type,FileRoot.Content.DATA, date,fileName);
    }

    /***
     * md5全记录
     * @param file
     * @param mf
     */
    public void logMd5(File file, FileRoot.Md5File mf){
        logMd5(MD5FileUtil.getFileMD5String(file),file,mf);
    }

    /***
     * md5全记录
     * @param md5 提供md5
     * @param file
     * @param mf
     */
    public void logMd5(String md5,File file, FileRoot.Md5File mf){
        //通过文件获取对应但日志输出流
        if(file == null) file = mf.f;
        root.logMd5(md5, file,mf);
        storeMd5(md5,file.getAbsolutePath());//内存md5字典
    }

    /***
     * 秒传 创建硬链接
     * @param md5 对应文件md5
     * @param mf 对应要上传的分类文件,其中mf.f为完整文件
     * @return
     */
    boolean saveLink(String md5, FileRoot.Md5File mf){
        for (Map<String, String> md5Map : md5Maps) {//U
            if(md5Map.containsKey(md5)){
                String[] fs = md5Map.get(md5).split(";");
                File f = new File(root.root(),fs[0]);
                for(String s:fs){//某一份文件存在则会进行恢复
                    if((f = new File(root.root(),s)).exists()) break;
                }
                File to = root.pathFile(fn(mf.getFile().getName()),mf);
                if(f.exists()){
                    FileCmd.createLink(f,to);
                    if(mf.f != null){
                        mf.f.delete();
                    }
                }else{
                    if(mf.f != null && mf.f.exists()){
                        mf.f.renameTo(to);
                        fileRecall((md5Map.get(md5) + ";" + to.getAbsolutePath().substring(root.root().length())).split(";"));
                    }else{
                        return false;
                    }
                }
                mf.f = to;
                logMd5(md5,to,mf);//md5全记录
                return true;
            }
        }
        return false;
    }


    boolean hasUser(long size){
        long psize = avail;
        for (Long l : useing) {
            psize -= l;
        }
        return psize > size;
    }

    /***
     * 加载md5进行md5字典登记
     * @param in 输入流
     * @param mf 文件类型
     * @param file 对应md5字典
     * @throws IOException
     */
    private void readMd5(BufferedReader in, FileRoot.Md5File mf, File file) throws IOException {//通过流读取md5字典信息
        String line;
        while((line = in.readLine()) != null){
            if(StringUtils.isNotBlank(line)){
                String[] ss = line.split("\t");
                storeMd5(ss[0], root.pathFile(ss[1],mf, file.getName().substring(0,10)).getAbsolutePath());//文件校验
            }
        }
    }

    /***
     * 加载md5进行md5字典登记
     * @param md5 md5字典
     * @param path 绝对路径
     */
    private void storeMd5(String md5,String path){
        //判断那个字典含有,如果有进行md5补全,文件补全
        path = path.substring(root.root().length());
        for (Map<String, String> m : md5Maps) {//U
            if(m.containsKey(md5)){
                String pf = m.get(md5);
                if(!pf.contains(";")){
                    files.add(pf);
                }
                files.add(path);
                m.put(md5,pf + ";" + path);
                fileRecall(m.get(md5).split(";"));
                return;
            }
        }
        md5Map().put(md5,path);
    }

    private void fileRecall(String[] fileNames){//对文件进行还原
        List<File> list = new ArrayList<>(fileNames.length);
        for(String f:fileNames){
            list.add(new File(root.root(),f));
        }
        FileCmd.createLink(list);
    }

    int index = 0;
    private Map<String,String> md5Map(){//获取加载md5Map
        Map<String,String> map;
        if(md5Maps.size() > index){//定义无需U
            map = md5Maps.get(index);
        }else{
            md5Maps.add(map = new HashMap(max_));
        }
        if(map.size() < max){
            return map;
        }
        index++;
        if(md5Maps.size() > index){
            map = md5Maps.get(index);
        }else{
            md5Maps.add(map = new HashMap(max_));
        }
        return map;
    }

    void clear(){//md5清理
        index = 0;
        md5Maps.forEach(Map::clear);
    }

    /***
     * 获取文件名称
     * @return
     */
    public static String fn(String filename){
        String ext = "";
        if(StringUtils.isNotBlank(filename)){
            ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";//将所有文件复制一份至back/data下
        }
        return System.currentTimeMillis() + "_" + StringUtils.random(3) + ext;
    }

    public void setRoot(FileRoot root) {
        this.root = root;
    }


    public Map sysInfo() {
        int fileNum = 0;
        long fileSize = 0;
        int fileOnlyNum = 0;
        int fileRepeatNum = 0;
        for (Map<String, String> md5Map : md5Maps) {
            for (String s : md5Map.keySet()) {
                fileOnlyNum++;
                String[] ss = md5Map.get(s).split(";");
                fileNum += ss.length;
                fileSize += new File(root.root(), ss[0]).length();
                fileRepeatNum += (ss.length - 1);
            }
        }

        long allSize = fileSize + avail;
        Map m = new HashMap<>();
        m.put("allSize",MD5FileUtil.l2s(allSize));//总容量
        m.put("avail",MD5FileUtil.l2s(avail));//可用容量
        m.put("availPer",(avail * 100 / allSize) + "%");//可用容量比
        m.put("fileSize",MD5FileUtil.l2s(MD5FileUtil.fileSize(new File(root.root()))));
        m.put("fileNum",fileNum);//文件数量
        m.put("fileOnlyNum",fileOnlyNum);//文件数量
        m.put("fileRepeatNum",fileRepeatNum);//文件数量
        m.put("useSize",MD5FileUtil.l2s(fileSize));//文件大小
        return m;
    }

    /***
     * 文件系统树结构
     * @return
     */
    public SelectedVo sysTree() {
        SelectedVo v = new SelectedVo();
        v.setLabel(panName + "[" + MD5FileUtil.fileNum(root.rootFileDefine(FileRoot.Home.FILE),"md5") + "]");
        List<FileRoot.Md5File> file = root.paths(FileRoot.Home.FILE, null, FileRoot.Content.DATA);
        for (FileRoot.Md5File md5File : file) {
            //查找项目
            String type = md5File.getFile().getParentFile().getName();
            String pro = md5File.getFile().getParentFile().getParentFile().getName();
            //项目
            SelectedVo v11 = null;
            for(SelectedVo v1 : v.getChildren()){
                if(v1.getLabel().startsWith(pro)){
                    v11 = v1;
                }
            }
            if(v11 == null){
                v11 = new SelectedVo();
                v11.setLabel(pro + "[" + MD5FileUtil.fileNum(md5File.getFile().getParentFile().getParentFile(),"md5") + "]");
                v11.setValue(panName.substring(2) + ":" + pro);
                v.getChildren().add(v11);
            }
            //类型
            SelectedVo v22 = null;
            for(SelectedVo v2:v11.getChildren()){
                if(v2.getLabel().startsWith(type)){
                    v22 = v2;
                }
            }
            if(v22 == null){
                v22 = new SelectedVo();
                v22.setLabel(type + "[" + MD5FileUtil.fileNum(md5File.getFile().getParentFile(),"md5") + "]");
                v22.setValue(panName.substring(2) + ":" + pro + "/" + type);
                v11.getChildren().add(v22);
            }
            //文件
            for(File f:md5File.getFile().listFiles()){
                SelectedVo v3 = new SelectedVo();
                v22.getChildren().add(v3);
                v3.setLabel(f.getName() + "[" + f.list().length + "]");
                v3.setValue(panName.substring(2) + ":" + pro + "/" + type + "/" + f.getName());
            }
        }

        return v;
    }


    private static List<String> deleteMd5List = Collections.synchronizedList(new ArrayList<>(10000));
    private static List<File> deleteFileList = Collections.synchronizedList(new ArrayList<>(100));

    /***
     * 删除记录文件
     * @param systemId
     * @param retainDay
     */
    public int deleteRecord(String systemId, Integer retainDay) {
        return deleteFile(systemId, FileRoot.Type.RECORD,retainDay);
    }


    /***
     * 删除记录文件
     * @param systemId
     * @param type
     * @param retainDay
     */
    public synchronized int deleteFile(String systemId, FileRoot.Type type, Integer retainDay) {
        int size = 0;
        deleteMd5List.clear();
        deleteFileList.clear();
        final List<File> md5List = new ArrayList<>(1);
        final List<File> dataList = new ArrayList<>(1);
        if(StringUtils.isNotBlank(systemId)){
            //读取md5字典
            File f = root.rootFileDefine(FileRoot.Home.FILE,systemId, type, FileRoot.Content.MD5);
            if(f.exists()){
                md5List.add(f);
            }
            //读取文件数据
            f = root.rootFileDefine(FileRoot.Home.FILE,systemId, type, FileRoot.Content.DATA);
            if(f.exists()){
                dataList.add(f);
            }
        }else{
            //读取md5字典
            List<FileRoot.Md5File> list1 = root.paths(FileRoot.Home.FILE, type, FileRoot.Content.MD5);
            list1.forEach(f -> md5List.add(f.getFile()));
            //读取文件数据
            list1 = root.paths(FileRoot.Home.FILE, type, FileRoot.Content.DATA);
            list1.forEach(f -> dataList.add(f.getFile()));
        }

        String date = ymd.format(new Date(System.currentTimeMillis() - (long) retainDay * 24 * 60 * 60 * 1000));
        md5List.forEach(dir -> {
            Arrays.asList(dir.listFiles()).forEach(f -> {
                if(!f.getName().startsWith(date) && f.getName().compareTo(date) < 0){
                    //读取md5字典信息,提供后面md5字典删除
                    BufferedReader in = null;
                    try {
                        in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                        String line = null;
                        while((line = in.readLine()) != null){
                            String[] ss = line.split("\t");
                            deleteMd5List.add(ss[0] + "\t" + f.getName().substring(0,10) + "/" + ss[1]);
                        }
                        deleteFileList.add(f);
                    } catch (Exception e) {
                    }finally {
                        MD5FileUtil.close(in);
                    }
                }
            });
        });

        dataList.forEach(dir -> {
            Arrays.asList(dir.listFiles()).forEach(f -> {
                if(!f.getName().equals(date) && f.getName().compareTo(date) < 0){
                    //对应待删除的文件数据
                    deleteFileList.add(f);
                }
            });
        });

        deleteMd5List.forEach(s -> {
            String[] ss = s.split("\t");
            for (Map<String, String> md5Map : md5Maps) {
                String v = md5Map.remove(ss[0]);
                if(v != null){
                    v = v.replaceFirst("(file[a-zA-Z0-9/]*" + ss[1] + ";)|(;file[a-zA-Z0-9/]*" + ss[1] + ")|(file[a-zA-Z0-9/]*" + ss[1] + ")","");
                    if(StringUtils.isNotBlank(v)){
                        md5Map.put(ss[0],v);
                    }
                }
            }
        });
        size = deleteMd5List.size();
        deleteFileList.forEach(MD5FileUtil::delete);
        deleteMd5List.clear();
        deleteFileList.clear();
        return size;
    }

    public void registerUser(String account, String pwd, boolean cover){
        File file = root.rootFileDefine(FileRoot.Home.USER,account);
        if(!cover && file.exists()){
            throw new RuntimeException("该用户已注册!");
        }
        try{
            file.getParentFile().mkdirs();
            file.createNewFile();
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
            out.print(pwd);
            MD5FileUtil.close(out);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public boolean login(String user,String pwd){
        return pwd.equals(readPwd(user));
    }


    public String readPwd(String account){
        File file = root.rootFileDefine(FileRoot.Home.USER,account);
        if(!file.exists()){
            throw new RuntimeException("该用户不存在!");
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            return in.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            MD5FileUtil.close(in);
        }
    }



}