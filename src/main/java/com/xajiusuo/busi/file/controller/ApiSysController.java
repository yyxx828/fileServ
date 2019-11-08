package com.xajiusuo.busi.file.controller;

import com.xajiusuo.busi.file.base.BaseController;
import com.xajiusuo.busi.file.contain.FileResBean;
import com.xajiusuo.busi.file.contain.FileSysAgent;
import com.xajiusuo.utils.MD5FileUtil;
import com.xajiusuo.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 杨勇 on 2018/5/23.
 */
@Api(description = "系统管理")
@RestController
@RequestMapping(value = "/api/sys")
public class ApiSysController extends BaseController{

    @Autowired
    private FileSysAgent agent;

    public static Map<String,HttpSession> sessionMap = Collections.synchronizedMap(new HashMap<>(27));

    /***
     * @return
     * Created by 杨勇 on 2018-5 文件备份整理 只能管理员执行
     * @author 杨勇 19-7-23
     */
    @ApiOperation(value = "文件备份整理", notes = "文件备份整理", httpMethod = "POST")
    @ApiImplicitParams({
    })
    @RequestMapping(value = "/back", method = RequestMethod.POST)
    public FileResBean back() {
        admin();
        if(FileSysAgent.back()){
            return FileResBean.to(() -> Collections.singletonMap("message","备份成功"));
        }
        return FileResBean.to("备份失败");
    }


    /***
     * @return
     * Created by 杨勇 on 2018-5 文件备份整理 只能管理员执行
     * @author 杨勇 19-7-23
     */
    @ApiOperation(value = "文件系统还原", notes = "文件系统还原", httpMethod = "POST")
    @ApiImplicitParams({
    })
    @RequestMapping(value = "/restore", method = RequestMethod.POST)
    public FileResBean restore() {
        admin();
        if(FileSysAgent.restore()){
            return FileResBean.to(() -> Collections.singletonMap("message","还原成功"));
        }
        return FileResBean.to("还原失败");
    }

    /***
     * @return
     * Created by 杨勇 on 2018-5 系统账户注册
     * @param systemId 要使用但系统ID,使用明文
     * @param pwd 登陆密码,需要明文
     * @author 杨勇 19-8-12
     */
    @ApiOperation(value = "系统账户注册", notes = "系统账户注册", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "systemId", required = true, value = "系统标识不区分大小写", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "pwd", required = true, value = "密码", paramType = "query", dataType = "string"),
    })
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public FileResBean register(String systemId,String pwd) {
        if(StringUtils.isBlank(systemId) || StringUtils.isBlank(pwd)){
            return FileResBean.to("系统标识和密码不能为空");
        }
        systemId = systemId.trim().toLowerCase();
        String account = MD5FileUtil.getMD5String(systemId);
        String mm = MD5FileUtil.pwdMd5(pwd);

        try{
            agent.registerUser(account,mm);
            return FileResBean.to(() -> Collections.singletonMap("message","注册成功"));
        }catch (Exception e){
            return FileResBean.to(e.getMessage());
        }
    }


    /***
     * Created by 杨勇 on 2018-5 用户密码修改,只能在登陆条件下
     * @param pwd 旧密码
     * @param pwd1 新密码
     * @author 杨勇 19-8-12
     */
    @ApiOperation(value = "修改密码", notes = "修改密码", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pwd", required = true, value = "原始密码", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "pwd1", required = true, value = "新密码", paramType = "query", dataType = "string"),
    })
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    public FileResBean updatePassword(String pwd,String pwd1){
        String account = getAccount (null);
        try{
            boolean res = agent.updatePassword(account,pwd,pwd1);
            if(res){
                return FileResBean.to(() -> Collections.singletonMap("message","密码修改成功"));
            }else{
                return FileResBean.to("原密码错误");
            }
        }catch (Exception e){
            return FileResBean.to(e.getMessage());
        }
    }

    /***
     * @return
     * Created by 杨勇 on 2018-5 业务系统用户登陆
     * @param systemId 要登陆但系统名,明文
     * @param pwd 密码 明文
     * 系统账户登陆
     * @author 杨勇 19-8-12
     */
    @ApiOperation(value = "系统账户登陆", notes = "系统账户登陆", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "systemId", required = true, value = "系统标识不区分大小写", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "pwd", required = true, value = "密码", paramType = "query", dataType = "string"),
    })
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public FileResBean login(String systemId,String pwd){
        if(StringUtils.isBlank(systemId) || StringUtils.isBlank(pwd)){
            return FileResBean.to("系统标识和密码不能为空");
        }
        systemId = systemId.trim().toLowerCase();
        String account = systemId;
        try{
            String mm = MD5FileUtil.pwdMd5(pwd);
            account = MD5FileUtil.getMD5String(account);
            boolean res = agent.login(account,mm);
            if(res){
                request.getSession().setAttribute("account",account);
                request.getSession().setAttribute("systemId",systemId);
                String sessionIdMd5 = MD5FileUtil.getMD5String(request.getSession().getId());
                sessionMap.put(sessionIdMd5,request.getSession());
                return FileResBean.to(() -> {
                    Map m = new HashMap();
                    m.put("message","登陆成功");
                    m.put("sessionId",sessionIdMd5);
                    return m;
                });
            }else{
                return FileResBean.to("密码错误,登陆失败");
            }
        }catch (Exception e){
            return FileResBean.to(e.getMessage());
        }
    }

    /***
     * @return
     * Created by 杨勇 on 2018-5 系统账户退出
     * @author 杨勇 19-8-12
     */
    @ApiOperation(value = "系统账户退出", notes = "系统账户登陆", httpMethod = "GET")
    @ApiImplicitParams({
    })
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public FileResBean logout(){
        if(getAccount(null) == null){
            return FileResBean.to(() -> Collections.singletonMap("message","无用户登陆信息"));
        }
        request.getSession().invalidate();
        return FileResBean.to(() -> Collections.singletonMap("message","用户已退出"));
    }

    /***
     * @return
     * Created by 杨勇 on 2018-5 用户登陆信息验证
     * @param sessionId 用户验证信息,业务使用r参数传入
     * @author 杨勇 19-8-12
     */
    @ApiOperation(value = "用户登陆信息验证", notes = "用户登陆信息验证", httpMethod = "GET")
    @ApiImplicitParams({
    })
    @RequestMapping(value = "/isLogin", method = RequestMethod.GET)
    public FileResBean isLogin(String sessionId){
        if(accountInfo(sessionMap.get(sessionId))){
            return FileResBean.to(() -> Collections.singletonMap("message","用户已登陆"));
        }else{
            sessionMap.remove(sessionId);
        }
        return FileResBean.to("用户未登陆");
    }

}