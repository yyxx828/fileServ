package com.xajiusuo.utils;

/**
 * Created by 杨勇 on 19-7-19.
 * 控制台颜色打印
 */
public class ColorUtil {

    public static String red(String s){
        return diy(C.red,null,s);
    }
    public static String green(String s){
        return diy(C.green,null,s);
    }
    public static String orange(String s){
        return diy(C.orange,null,s);
    }
    public static String blue(String s){
        return diy(C.blue,null,s);
    }
    public static String purple(String s){
        return diy(C.purple,null,s);
    }
    public static String lightblue(String s){
        return diy(C.lightblue,null,s);
    }
    public static String cyan(String s){
        return diy(C.cyan,null,s);
    }

    public static String diy(C c,B b,String s){
        String bc = "";
        if(b != null){
            bc = b.code + ";";
        }
        if(c == null) c = C.black;
        return "\u001B["+ bc + c.code + ";m" + s + "\u001B[0;" + c.code + "m";
    }

    enum C{
        red("31"),
        green("32"),
        orange("33"),
        blue("34"),
        purple("35"),
        lightblue("36"),
        cyan("37"),
        black("38");

        String code;
        C(String c){
            code = c;
        }
    }

    enum B{
        black("40"),
        red("41"),
        green("42"),
        orange("43"),
        blue("44"),
        purple("45"),
        lightblue("46"),
        cyan("47");
        String code;
        B(String c){
            code = c;
        }

    }
}
