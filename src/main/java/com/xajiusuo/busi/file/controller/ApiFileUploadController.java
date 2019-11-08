package com.xajiusuo.busi.file.controller;

import com.xajiusuo.busi.file.base.BaseController;
import com.xajiusuo.busi.file.contain.*;
import com.xajiusuo.utils.MD5FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 杨勇 on 2018/5/23.
 */
@Api(description = "文件上传管理")
@RestController
@RequestMapping(value = "/api/upload")
public class ApiFileUploadController extends BaseController{


//////////////////////////////////////////////整体文件上传////////////////////////////////////////////////////////////////



    /***
     * Created by 杨勇 on 2018-5 文件上传
     * @param type 不同文件类型但上传[Static:永久文件,Record:记录,Temp:临时]
     * @param file 待上传的文件
     * @author 杨勇 19-7-25
     * @return 文件正常上传完完成后,返回文件信息,如果异常,返回异常信息
     */
    @ApiOperation(value = "文件上传", notes = "文件上传", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "Static:永久文件,Record:记录,Temp:临时", allowableValues = "Static,Record,Temp", defaultValue = "Static",paramType = "path", dataType = "String"),
    })
    @RequestMapping(value = "/upload{type}", method = RequestMethod.POST)
    public FileResBean upload(@PathVariable(name = "type") String type, @RequestPart(name = "file", required = true) MultipartFile file) {
        try {
            FileRoot.Type rt =  FileRoot.Type.valueOf(type.toUpperCase());
            return FileSaveUtil.uploadWholeFile(getSystemId(), rt,file);
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }
    }

/////////////////////////////////////////断点文件上传/////////////////////////////////////////////////////////////////////

    /***
     *
     * Created by 杨勇 on 2018-5 文件断点正常上传完完成后,返回文件信息,如果异常,返回异常信息,单次上传信息请大于1M
     * @param file 待上传的文件切片后文件
     * @param pos 本次上传文件的字节起始位置
     * @param type {@link FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 上传文件名,首次上传不能带文件名,上传后会将文件名返回,之后上传必须带返回的文件名,否则无法上传
     * @param md5 文件字典信息,上传同时,需要在本地js进行MD5计算,计算除MD5后在附加到当时上传文件时候但参数
     * @param totalSize 总文件大小,上传时候必须带入,否则上传结果无法正常保证
     * @author 杨勇 19-7-25
     * @return
     */
    @ApiOperation(value = "文件断点上传", notes = "常驻文件断点上传", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "Static:永久文件,Record:记录,Temp:临时", allowableValues = "Static,Record,Temp", defaultValue = "Static",paramType = "path", dataType = "String"),
    })
    @RequestMapping(value = "/partUpload{type}", method = RequestMethod.POST)
    public FileResBean partUpload(@PathVariable(name = "type") String type, @RequestPart(name = "file", required = true) MultipartFile file, long pos,String fileName,String md5,long totalSize) {
        try {
            FileRoot.Type rt =  FileRoot.Type.valueOf(type.toUpperCase());
            return FileSaveUtil.uploadPartFile(getSystemId(), rt,file,pos,fileName,md5,totalSize);
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }
    }

//////////////////////////////////////////////文件删除方法////////////////////////////////////////////////////////////////
    /***
     * Created by 杨勇 on 2018-5 记录文件删除
     * @return 文件删除描述
     * @param retainDay 对于 {@link FileRoot.Type} 的文件进行删除,天数必须大于30天,如果小于30天,则会使用默认90
     * @author 杨勇 19-8-9
     */
    @ApiOperation(value = "记录文件删除", notes = "记录文件删除", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "retainDay", required = false, value = "保留天数(大于30),默认90", allowableValues = "range[30, infinity]", defaultValue = "90",paramType = "query", dataType = "Integer"),
    })
    @RequestMapping(value = "/deleteRecord", method = RequestMethod.POST)
    public FileResBean deleteRecord(Integer retainDay) {

        if(retainDay == null || retainDay < 30){
            retainDay = 90;
        }

        try {
            int size = FileSysAgent.deleteRecord(getSystemId(), retainDay);
            return FileResBean.to(() -> {
                Map m = new HashMap();
                m.put("message","删除成功[" + size + "个]");
                return m;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }

    }
//////////////////////////////////////////////文件复制方法////////////////////////////////////////////////////////////////

    /***
     * Created by 杨勇 on 2018-5 文件远程复制
     * @param type {@link FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @param toType 要复制其它但类型 {@link FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @author 杨勇 19-7-31
     * @return 返回复制结果信息
     */
    @ApiOperation(value = "文件复制", notes = "文件复制", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "待复制文件类型", allowableValues = "static,record,temp", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "toType", required = true, value = "待复制文件类型", allowableValues = "static,record,temp", paramType = "query", dataType = "string"),
    })
    @RequestMapping(value = "/copyFile", method = RequestMethod.POST)
    public FileResBean copyFile(String type, String fileName, String toType) {
        return copyPath(type,null,fileName,toType);
    }

    /***
     * Created by 杨勇 on 2018-5 远程路径上传
     * @param type {@link FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param date 传入的二级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :
     * @param toType 要复制其它但类型 {@link FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @return 返回复制结果信息
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "路径复制", notes = "路径复制", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "待复制文件类型", allowableValues = "static,record,temp", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "toType", required = true, value = "待复制文件类型", allowableValues = "static,record,temp", paramType = "query", dataType = "string"),
    })
    @RequestMapping(value = "/copyPath", method = RequestMethod.POST)
    public FileResBean copyPath(String type, String date,String fileName, String toType) {
        try {
            return FileSysAgent.copyPath(getSystemId(),type,date, fileName, toType);
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }
    }

    /***
     * Created by 杨勇 on 2018-5 文件取消上传
     * @param fileName 要取消正在上传的断点文件信息,取消后该上传文件会进行删除
     * @author 杨勇 19-8-1
     * @return 返回操作结果
     */
    @ApiOperation(value = "取消上传", notes = "取消上传", httpMethod = "GET")
    @ApiImplicitParams({
    })
    @RequestMapping(value = "/cancelUpload", method = RequestMethod.GET)
    public FileResBean cancel(String fileName) {
        try {
            Md5OutContain.cancel(fileName);
            return FileResBean.to(() -> {
                Map<String,Object> m = new HashMap<String, Object>();
                m.put("statusCode",200);
                m.put("message","上传取消成功");
                return m;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }
    }



}