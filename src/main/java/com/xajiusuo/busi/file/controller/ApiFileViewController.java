package com.xajiusuo.busi.file.controller;

import com.xajiusuo.busi.file.base.BaseController;
import com.xajiusuo.busi.file.contain.FileResBean;
import com.xajiusuo.busi.file.contain.FileSysAgent;
import com.xajiusuo.utils.MD5FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by 杨勇 on 2018/5/23.
 */
@Api(description = "文件展示管理")
@RestController
@RequestMapping(value = "/api/see")
public class ApiFileViewController extends BaseController {

//////////////////////--------------------------文件列表--------------------------///////////////////////////////////////

    /***
     * @return
     * @desc 磁盘文件信息
     * @author 杨勇 19-8-7
     */
    @ApiOperation(value = "磁盘文件信息", notes = "磁盘文件信息", httpMethod = "GET")
    @RequestMapping(value = "/panInfo", method = RequestMethod.GET)
    public FileResBean panInfo() {
        admin();
        return FileSysAgent.sysInfo();
    }

    /***
     * @return
     * @desc 系统文件树
     * @author 杨勇 19-8-7
     */
    @ApiOperation(value = "系统文件树", notes = "系统文件树", httpMethod = "GET")
    @RequestMapping(value = "/panTree", method = RequestMethod.GET)
    public FileResBean panTree() {
        admin();
        return FileSysAgent.sysTree();
    }

    /***
     * @return
     * @desc 树枝对应文件列表
     * @author 杨勇 19-8-7
     */
    @ApiOperation(value = "树枝对应文件列表", notes = "树枝对应文件列表", httpMethod = "GET")
    @RequestMapping(value = "/panTreeList/{index}:{project}/{type}/{date}", method = RequestMethod.GET)
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    public FileResBean panTreeList(@PathVariable(value = "index") Integer index,@PathVariable(value = "project") String project,@PathVariable(value = "type") String type,@PathVariable(value = "date") String date) {
        admin();
        return FileSysAgent.treeList(index - 1,project,type,date);
    }

//////////////////////--------------------------文件预览--------------------------///////////////////////////////////////



    /***
     * @return
     * @desc 图片预览
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "图片预览", notes = "图片预览", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/showFilePic/{type}/{fileName}", method = RequestMethod.GET)
    public String showFilePic(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type, null, fileName);
            String content = MD5FileUtil.getContentType(file);
            if(content != null && content.startsWith("image")){
                fileDownOrShow("", content,new FileInputStream(file), file.length());
            }else {
                throw new RuntimeException("该文件不是图片文件");
            }
        } catch (Exception e) {
            if(e instanceof FileNotFoundException){
                throw new RuntimeException("文件系统无该文件信息.");
            }else{
                throw new RuntimeException(e);
            }
        }
        return "NONE";
    }

    /***
     * @return
     * @desc 图片预览
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "图片预览", notes = "图片预览", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/showPathPic/{type}/{path}/{fileName}", method = RequestMethod.GET)
    public String showPathPic(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "path") String path,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,path,fileName);
            String content = MD5FileUtil.getContentType(file);
            if(content.startsWith("image")){
                fileDownOrShow("", content,new FileInputStream(file), file.length());
            }else {
                throw new RuntimeException("该文件不是图片文件");
            }
        } catch (Exception e) {
            if(e instanceof FileNotFoundException){
                throw new RuntimeException("文件系统无该文件信息.");
            }else{
                throw new RuntimeException(e);
            }
        }
        return "NONE";
    }


    /***
     * @return
     * @desc 文档预览
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文档预览", notes = "文档预览", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/viewFile/{type}/{fileName}", method = RequestMethod.GET)
    public String viewFile(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "fileName") String fileName) {
        return viewPath(systemId,type,null,fileName);
    }

    /***
     * @return
     * @desc 文档预览
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文档预览", notes = "文档预览", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/viewPath/{type}/{date}/{fileName}", method = RequestMethod.GET)
    public String viewPath(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "date") String date,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,date, fileName);
            fileDownOrShow("", MD5FileUtil.getContentType(file),new FileInputStream(file), null);
        } catch (Exception e) {
            if(e instanceof FileNotFoundException){
                throw new RuntimeException("文件系统无该文件信息.");
            }else{
                throw new RuntimeException(e);
            }
        }
        return "NONE";
    }


    /***
     * @return
     * @desc 音视频文件拖动播放
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "音视频文件拖动播放", notes = "音视频文件拖动播放", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/playFile/{type}/{fileName}", method = RequestMethod.GET)
    public String playFile(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "fileName") String fileName) {
        return playPath(systemId,type,null,fileName);
    }


    /***
     * @return
     * @desc 音视频路径拖动播放
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "音视频路径拖动播放", notes = "音视频路径拖动播放", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/playPath/{type}/{date}/{fileName}", method = RequestMethod.GET)
    public String playPath(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "date") String date,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,date, fileName);
            String content = MD5FileUtil.getContentType(file);
            if(content != null && (content.startsWith("video") || content.startsWith("audio"))){
                multiMediaPlay(content,file);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "NONE";
    }

//////////////////////--------------------------文件下载--------------------------///////////////////////////////////////

    //文件下载

    /***
     * @return
     * @desc 文件下载
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文件下载", notes = "文件下载", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/downFile/{type}/{fileName}", method = RequestMethod.GET)
    public String downFile(String systemId, @PathVariable(value = "type") String type, @PathVariable(value = "fileName") String fileName) {
        return downPath(getSystemId(systemId),type,null,fileName);
    }


    /***
     * @return
     * @desc 文件下载
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文件下载", notes = "文件下载", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/downPath/{type}/{date}/{fileName}", method = RequestMethod.GET)
    public String downPath(String systemId, @PathVariable(value = "type") String type, @PathVariable(value = "date") String date, @PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,date , fileName);
            String content = MD5FileUtil.getContentType(file);
            fileDownOrShow(file.getName(), content,new FileInputStream(file), file.length());
        } catch (Exception e) {
            if(e instanceof FileNotFoundException){
                throw new RuntimeException("文件系统无该文件信息.");
            }else{
                throw new RuntimeException(e);
            }
        }
        return "NONE";
    }

    /***
     * @return
     * @desc 文件是否存在
     * @author 杨勇 19-9-17
     */
    @ApiOperation(value = "文件是否存在", notes = "文件是否存在", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/existFile/{type}/{fileName}", method = RequestMethod.GET)
    @ResponseBody
    public String existFile(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "fileName") String fileName) {
        try {
            File file = FileSysAgent.findPath(getSystemId(systemId),type, null, fileName);
            return file.exists() + "";
        } catch (Exception e) {
            return false + "";
        }
    }

    /***
     * @return
     * @desc 文件是否存在
     * @author 杨勇 19-9-17
     */
    @ApiOperation(value = "文件是否存在", notes = "文件是否存在", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/existPath/{type}/{path}/{fileName}", method = RequestMethod.GET)
    @ResponseBody
    public String existPath(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "path") String path,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,path,fileName);
            return file.exists() + "";
        } catch (Exception e) {
            return false + "";
        }
    }


}