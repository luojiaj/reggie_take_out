package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request,@RequestBody Employee employee){

        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }

        //4、密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        //6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
//        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工，员工信息:{}", employee.toString());
        //设置初始密码123456并用md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //获取当前登录的用户id
        Long empid = (Long)request.getSession().getAttribute("employee");

        //采取公共字段注入的方式:1.在属性上加@TableField注解 2.common下编写类MyMetaObjectHandler实现接口实现
        //employee.setCreateUser(empid);
        //employee.setUpdateUser(empid);
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * @param page 前段传递的默认页(默认为查第一页)
     * @param pageSize 前段传递的每一页几条数据(默认为10条)
     * @param name 前段的根据名字查询(可能有)
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器
        Page pageInfo = new Page(page, pageSize);
        //构造条件构造器(name),name不为空才like
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }
    //只需要返回一个code就可以

    /**
     *根据id来修改员工信息, 参数中的employee的status已经发生变化
     * 所以直接调用UpdateId即可，变化的变。不变的不变(MBP自动)
     * 只需要加一个putMapping注解就行，因为更新的url还是/employee下
     * @param employee 传过来的是一个json格式
     * @return
     */
    //问题：前端的id是long类型，传递过来的id会不一样, 因为是雪花算法生成所以太长了
    //最后几位可能会丢失(变成0)
    //雪花算法生成的id是19位，前段js处理的时候丢失精度，只能处理前16位正确！
    //导致与数据库不一样
    //解决：前段把long类型id变成string字符串，在common中引入json转化器
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
        employeeService.updateById(employee);
        return R.success("员工修改数据成功");
    }

    /**
     * PathVariable代表拿到前端url中的id
     * 点修改后会自动调用update方法
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("前端修改员工信息页面---后台根据id修改员工信息(前台传)");
        Employee employee = employeeService.getById(id);
        if(employee == null){
            return R.error("没有查询到对应信息");
        }
        return R.success(employee);
    }
}
