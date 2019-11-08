package com.xajiusuo.busi.file.contain;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

/**
 * 文件创建硬链接命令
 * Created by 杨勇 on 19-7-19.
 */
@FunctionalInterface
public interface FileCmd {

    FileCmd[] instance = new FileCmd[]{null};
///////////----------------------------------公共方法,不对外提供服务--------------------------------------------///////////

    /***
     * 获取对应平台文件命令
     * @return
     */
    static FileCmd instance(){
        if(instance[0] == null){
            String osName = System.getProperty("os.name");
            if(osName.startsWith("Linux")){
                instance[0] = () -> "ln {0} {1}";
            }else if(osName.startsWith("Windows")){
                instance[0] = () -> "mklink/H {1} {0}";
            }else{
                throw new RuntimeException("unsupported Operating Systems!");
            }
        }
        return instance[0];
    }

    //命令执行
    static void exec(String cmd){
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

///////////---------------------------------------外部调用方法-------------------------------------------------///////////

    /***
     * 含有存在文件,不存在的文件,根据存在但文件进行文件硬链接
     * @param list 文件集合
     */
    static void createLink(List<File> list){
        final int[] count = {0};
        list.forEach(f -> count[0] += f.exists() ? 1 : 0);
        if(count[0] == 0 || count[0] == list.size()){
            list.clear();
            return;
        }
        File f = list.stream().filter(File::exists).findFirst().get();
        createLink(f,list);
    }

    /***
     * 含有存在文件,不存在的文件,根据存在但文件进行文件硬链接
     * @param list 文件集合
     */
    static void createLink(File f, List<File> list) {
        list.stream().filter(ff -> !ff.exists()).forEach(ff -> {
            createLink(f,ff);
        });
    }

    /***
     * 含有存在文件,不存在的文件,根据存在但文件进行文件硬链接
     * @param inf 存在文件
     * @param outf 存文件目标
     */
    static void createLink(File inf, File outf) {
        if(outf.exists()) return;
        outf.getParentFile().mkdirs();
        exec(instance().fileRecallCmd(inf.getAbsolutePath(),outf.getAbsolutePath()));
    }

///////////-------------------------------------不同平台,各自提供命令-------------------------------------------///////////
    default String fileRecallCmd(String target, String link){
        return MessageFormat.format(cmd(),target,link);
    }

    String cmd();

}
