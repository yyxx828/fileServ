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
     * @return
     * @desc 常驻文件上传
     * @author 杨勇 19-7-25
     */
    @ApiOperation(value = "文件url上传", notes = "文件url上传，未启用", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "Static:永久文件,Record:记录,Temp:临时", allowableValues = "Static,Record,Temp", defaultValue = "Static",paramType = "path", dataType = "String"),
    })
    @RequestMapping(value = "/uploadUrl{type}", method = RequestMethod.POST)
    public FileResBean uploadUrl(@PathVariable(name = "type") String type,String url) {
        try {
            URL url1 = new URL(url);
            url1.openConnection();
            InputStream in =  url1.openStream();

//            MD5FileUtil.copyf(in,);
            FileRoot.Type rt =  FileRoot.Type.valueOf(type.toUpperCase());
            return FileSaveUtil.uploadWholeFile(getSystemId(), rt,new File(""));
        } catch (Exception e) {
            e.printStackTrace();
            return FileResBean.to(e.getMessage());
        }
    }

    /***
     * @return
     * @desc 常驻文件上传
     * @author 杨勇 19-7-25
     */
    @ApiOperation(value = "文件上传", notes = "常驻文件上传", httpMethod = "POST")
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
     * @return
     * @desc 常驻文件断点上传
     * @author 杨勇 19-7-25
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
     * @return
     * @desc 记录文件删除
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
     * @return
     * @desc 文件复制
     * @author 杨勇 19-7-31
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
     * @return
     * @desc 文件复制
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
     * @return
     * @desc 取消上传
     * @author 杨勇 19-8-1
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