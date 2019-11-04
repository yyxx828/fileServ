package com.xajiusuo.busi.file.base;

import java.io.OutputStream;

/**
 * Created by hadoop on 19-8-1.
 */

@FunctionalInterface
public interface OutputStreamDown {


    void write(OutputStream var1) throws Exception;

    static OutputStreamDown to(OutputStreamDown down){
        return down;
    }

}
