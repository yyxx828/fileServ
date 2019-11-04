package com.other;

import com.xajiusuo.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hadoop on 19-10-17.
 */
public class TestReadPackage {


    List<File> list = new LinkedList<>();
    String rootName = "/home/hadoop/桌面/lsql/src/main/java/";

    @Before
    public void begin(){
        File root = new File(rootName);
        readFile(root);
    }

    private void readFile(File file) {
        if(file.isFile()){
            if(file.getAbsolutePath().endsWith(".java")){
                list.add(file);
            }
        }else if(file.isDirectory()){
            for (File file1 : file.listFiles()) {
                readFile(file1);
            }
        }
    }


    @Test
    public void readJavaTest() throws Exception {
        File file = new File("/home/hadoop/桌面/all.txt");
        List<String> buf = new LinkedList();
        PrintWriter out = new PrintWriter(file);
        int num = 0;
        for (File file1 : list) {
            BufferedReader in = new BufferedReader(new FileReader(file1));
            String line;
            while((line = in.readLine()) != null){
                if(StringUtils.isBlank(line) || line.startsWith("//") || line.startsWith("package ") || line.startsWith("import ") || line.startsWith("/**") || line.startsWith(" * ") || line.startsWith(" */")) continue;
                buf.add(line);
            }
            in.close();
            if(buf.size() > 2){//类有内容才打印
                print_(file1.getAbsolutePath(),out);//打印文件标识
                int row = 0;
                for (String s : buf) {
                    if(row > 0){
                        out.println(row + s);
                    }else{
                        out.println(s);
                    }
                    row++;
                    if(s.trim().equals("}") || s.trim().equals("{")){
                    }else{
                        num++;
                    }
                }
                num --;
                System.out.println(file1.getAbsolutePath() + " --- 读取完成");
            }else {
                System.out.println(file1.getAbsolutePath() + " xxx skip!");
            }
            buf.clear();
        }
        System.out.println("     ------------------------ over,有效代码 [" + num + "]条! ------------------------    ");
        out.println("     ------------------------ over,有效代码 [" + num + "]条! ------------------------    ");
        out.close();
    }

    public void print_(String fileName, PrintWriter out){
        String className = fileName.replace(rootName,"").replace("/",".");
        int _len = className.length() * 90 / 50;
        out.print("\r\n ----------");
        for(int i = 0;i < _len;i++){
            out.print("-");
        }
        out.print("--------------- \r\n");

        out.print("|       " + className + "      |\r\n");

        out.print(" ----------");
        for(int i = 0;i < _len;i++){
            out.print("-");
        }
        out.print("--------------- \r\n");
    }

}
