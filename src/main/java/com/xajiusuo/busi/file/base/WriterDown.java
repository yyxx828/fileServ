package com.xajiusuo.busi.file.base;

import java.io.BufferedWriter;

/**
 * Created by hadoop on 19-8-1.
 */
@FunctionalInterface
public interface WriterDown {

    void write(BufferedWriter var1) throws Exception;

    static WriterDown to(WriterDown down){
        return down;
    }
}
