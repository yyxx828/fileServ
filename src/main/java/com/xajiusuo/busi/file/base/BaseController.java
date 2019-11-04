package com.xajiusuo.busi.file.base;

import com.xajiusuo.busi.file.contain.FileResBean;
import com.xajiusuo.busi.file.controller.ApiSysController;
import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.List;

/**
 * Created by 杨勇 on 19-8-1.
 */
@RestController
public class BaseController {

    @Autowired
    protected HttpServletRequest request;
    @Autowired
    protected HttpServletResponse response;

    @Value("${file.admin.accout}")
    String admin;

    protected void fileDownOrShow(String fileName, String contentType, WriterDown downFun) throws Exception {
        fileDownOrShow(fileName,contentType, null, OutputStreamDown.to((out) -> downFun.write(new BufferedWriter(new OutputStreamWriter(out, "utf-8")))));
    }

    protected void fileDownOrShow(String fileName, String contentType, List<?> list) throws Exception {
        fileDownOrShow(fileName, contentType, WriterDown.to((out) -> {
            for(Object o : list) {
                if(o != null) {
                    out.write(o.toString() + "\r\n");
                }
            }
        }));
    }

    protected void fileDownOrShow(String fileName, String contentType, InputStream in, Long contentLength) throws Exception {
        fileDownOrShow(fileName, contentType, contentLength, OutputStreamDown.to ((out) -> {
            byte[] buffer = new byte[8192];
            int len1;
            while((len1 = in.read(buffer)) != -1) {
                out.write(buffer, 0, len1);
            }
            in.close();
        }));
    }

    protected void fileDownOrShow(String fileName, String contentType, Long contentLength, OutputStreamDown downFun) throws Exception {
        if(StringUtils.isBlank(contentType)) {
            contentType = "text/html";
        }

        this.response.setContentType(contentType);
        if(contentType.startsWith("text")) {
            this.response.addHeader("Content-Type", "text/html; charset=utf-8");
        } else {
            this.response.addHeader("Content-Type", contentType);
        }
        if(contentLength != null){
            response.setHeader("content-length",contentLength + "");
        }

        if(StringUtils.isNotBlank(fileName)) {
            this.response.addHeader("Content-Disposition", "attachment;filename=" + this.converName(fileName));
        }

        ServletOutputStream out = this.response.getOutputStream();
        downFun.write(out);
        out.flush();
        out.close();
    }

    /***
     * 播放多媒体
     * @param contentType
     * @param file
     */
    protected void multiMediaPlay(String contentType, File file){
        try {
            RandomAccessFile randomFile = new RandomAccessFile(file,"r");

            String range = request.getHeader("Range");

            long contentLength = file.length();
            int start = 0,end = 0;
            if(range != null && range.startsWith("bytes=")){
                String[] values = range.split("=")[1].split("-");
                start = Integer.parseInt(values[0]);
                if(values.length > 1){
                    end = Integer.parseInt(values[1]);
                }
            }
            int requestSize = 0;
            if(end != 0 && end > start){
                requestSize = end - start + 1;
            }else{
                requestSize = Integer.MAX_VALUE;
            }

            byte[] buffer = new byte[8192];
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges","bytes");
            response.setHeader("ETag",file.getName());
            response.setDateHeader("Last-Modified", file.lastModified());

            if(range == null){
                response.setHeader("content-length",contentLength + "");
            }else{
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                long requestStart = 0,requestEnd = 0;
                String[] ranges = range.split("=");
                if(ranges.length > 1){
                    String[] rangeDatas = ranges[1].split("-");
                    requestStart = Integer.parseInt(rangeDatas[0]);
                    if(rangeDatas.length > 1){
                        requestEnd = Integer.parseInt(rangeDatas[1]);
                    }
                }
                long length = 0;
                if(requestEnd > 0){
                    length = requestEnd - requestStart + 1;
                    response.setHeader("Content-length",length + "");
                    response.setHeader("Content-Range","bytes " + requestStart + "-" + requestEnd + "/" + contentLength );
                }else{
                    length = contentLength - requestStart;
                    response.setHeader("Content-length",length + "");
                    response.setHeader("Content-Range","bytes " + requestStart + "-" + (contentLength - 1) + "/" + contentLength );
                }
            }

            ServletOutputStream out = response.getOutputStream();
            int needSize = requestSize;
            randomFile.seek(start);
            while(needSize > 0){
                int len = randomFile.read(buffer);
                if(needSize < buffer.length){
                    out.write(buffer,0,needSize);
                }else{
                    out.write(buffer,0,len);
                    if(len < buffer.length){
                        break;
                    }
                }
                needSize -= buffer.length;
            }
            MD5FileUtil.close(randomFile,out);
        }catch (Exception e){
        }
    }

    /***
     * 文件名转换
     * @param name
     * @return
     */
    protected String converName(String name) {
        try {
            return new String(name.getBytes("utf-8"), "ISO8859-1");
        } catch (Exception var3) {
            return name;
        }
    }

    @ExceptionHandler({Exception.class})
    public FileResBean handleException(Exception e) {
        String message;
        if(e instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException)e).getRootCause().getLocalizedMessage();
        } else {
            message = e.getLocalizedMessage();
        }
        return FileResBean.to(message);
    }

    public String getSystemId(){
        return getSystemId(null);
    }

    public String getSystemId(String sysId){
        HttpSession session = ApiSysController.sessionMap.get(request.getParameter("r"));
        Object o =  getAccount(session);
        if(o == null){
            throw new RuntimeException("用户未登陆");
        }
        if (o.equals(admin)){
            if(StringUtils.isNotBlank(sysId)){
                return sysId;
            }else{
                throw new RuntimeException("管理员帐号不能进行该上传操作");
            }
        }else{
            return getSystemid(session);
        }
    }

    public void admin(){
        String account = getAccount(null);
        if(account == null){
            throw new RuntimeException("用户未登陆");
        }
        if(!admin.equals(account)){
            throw new RuntimeException("非管理员账户无查看权限");
        }
    }

    public boolean accountInfo(HttpSession session){
        return getAccount(session) != null;
    }


    public String getAccount(HttpSession session){
        if(session == null){
            session = request.getSession();
        }
        return (String) session.getAttribute("account");
    }

    public String getSystemid(HttpSession session){
        if(session == null){
            session = request.getSession();
        }
        return (String) session.getAttribute("systemId");
    }

}
