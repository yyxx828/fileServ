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
     * Created by 杨勇 on 2018-5 磁盘文件信息 只能管理员进行查看
     * @author 杨勇 19-8-7
     * @return
     */
    @ApiOperation(value = "磁盘文件信息", notes = "磁盘文件信息", httpMethod = "GET")
    @RequestMapping(value = "/panInfo", method = RequestMethod.GET)
    public FileResBean panInfo() {
        admin();
        return FileSysAgent.sysInfo();
    }

    /***
     * Created by 杨勇 on 2018-5 系统文件树 只能管理员进行查看
     * @author 杨勇 19-8-7
     * @return
     */
    @ApiOperation(value = "系统文件树", notes = "系统文件树", httpMethod = "GET")
    @RequestMapping(value = "/panTree", method = RequestMethod.GET)
    public FileResBean panTree() {
        admin();
        return FileSysAgent.sysTree();
    }

    /***
     * @return
     * Created by 杨勇 on 2018-5 树枝对应文件列表 只能管理员进行查看
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @author 杨勇 19-8-7
     */
    @ApiOperation(value = "树枝对应文件列表", notes = "树枝对应文件列表", httpMethod = "GET")
    @RequestMapping(value = "/panTreeList/{index}:{project}/{type}/{path}", method = RequestMethod.GET)
    public FileResBean panTreeList(@PathVariable(value = "index") Integer index,@PathVariable(value = "project") String project,@PathVariable(value = "type") String type,@PathVariable(value = "date") String path) {
        admin();
        return FileSysAgent.treeList(index - 1,project,type,path);
    }

//////////////////////--------------------------文件预览--------------------------///////////////////////////////////////



    /***
     * Created by 杨勇 on 2018-5 图片预览
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 上传文件名,首次上传不能带文件名,上传后会将文件名返回,之后上传必须带返回的文件名,否则无法上传
     * @author 杨勇 19-7-31
     * @return
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
     * Created by 杨勇 on 2018-5 图片预览
     * @author 杨勇 19-7-31
     */
    /***
     *
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @return
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
     * Created by 杨勇 on 2018-5 文档预览
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
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
     * Created by 杨勇 on 2018-5 文档预览
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文档预览", notes = "文档预览", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/viewPath/{type}/{date}/{fileName}", method = RequestMethod.GET)
    public String viewPath(String systemId,@PathVariable(value = "type") String type,@PathVariable(value = "path") String path,@PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,path, fileName);
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
     * Created by 杨勇 on 2018-5 音视频文件拖动播放
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
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
     * Created by 杨勇 on 2018-5 音视频路径拖动播放
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "音视频路径拖动播放", notes = "音视频路径拖动播放", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", required = true, value = "文件类型", allowableValues = "static,record,temp", paramType = "path", dataType = "string"),
    })
    @RequestMapping(value = "/playPath/{type}/{path}/{fileName}", method = RequestMethod.GET)
    public String playPath(String systemId, @PathVariable(value = "type") String type, @PathVariable(value = "path") String path, @PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,path, fileName);
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
     * Created by 杨勇 on 2018-5 文件下载
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文件下载", notes = "文件下载", httpMethod = "GET")
    @RequestMapping(value = "/downFile/{type}/{fileName}", method = RequestMethod.GET)
    public String downFile(String systemId, @PathVariable(value = "type") String type, @PathVariable(value = "fileName") String fileName) {
        return downPath(getSystemId(systemId),type,null,fileName);
    }


    /***
     * Created by 杨勇 on 2018-5 文件下载
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-7-31
     */
    @ApiOperation(value = "文件下载", notes = "文件下载", httpMethod = "GET")
    @RequestMapping(value = "/downPath/{type}/{path}/{fileName}", method = RequestMethod.GET)
    public String downPath(String systemId, @PathVariable(value = "type") String type, @PathVariable(value = "date") String path, @PathVariable(value = "fileName") String fileName) {
        try {
            //查看文件是否存在
            File file = FileSysAgent.findPath(getSystemId(systemId),type,path , fileName);
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
     * Created by 杨勇 on 2018-5 文件是否存在
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-9-17
     */
    @ApiOperation(value = "文件是否存在", notes = "文件是否存在", httpMethod = "GET")
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
     * Created by 杨勇 on 2018-5 文件是否存在
     * @param systemId 系统ID,需要在业务系统进行提供,
     * @param type {@link com.xajiusuo.busi.file.contain.FileRoot.Type} 文件上传类型 [Static:永久文件,Record:记录,Temp:临时]
     * @param path 文件的上级目录 eg:20190101
     * @param fileName 要复制的文件名称 无二级根目录 eg :xxxxxxx.txt
     * @author 杨勇 19-9-17
     */
    @ApiOperation(value = "文件是否存在", notes = "文件是否存在", httpMethod = "GET")
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