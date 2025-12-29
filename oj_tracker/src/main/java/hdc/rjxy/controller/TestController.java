package hdc.rjxy.controller;

import hdc.rjxy.common.R;
import hdc.rjxy.mapper.UserMapper;
import hdc.rjxy.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 验证 1: JSON 接口测试
     * 检查点: Spring MVC 是否工作？Jackson 是否能序列化对象？Java 8 时间是否正常？
     * 访问地址: /test/json
     */
    @GetMapping("/json")
    @ResponseBody
    public Map<String, Object> testJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Spring MVC 环境配置成功！");
        map.put("time", LocalDateTime.now()); // 测试 JavaTimeModule 是否生效
        return map;
    }

    /**
     * 验证 2: 页面渲染测试
     * 检查点: Thymeleaf 是否能找到模板？变量是否能渲染？
     * 访问地址: /test/page
     */
    @GetMapping("/page")
    public String testPage(Model model) {
        // 向页面传递数据
        model.addAttribute("title", "环境验证页");
        model.addAttribute("content", "Thymeleaf 模板引擎工作正常！");
        model.addAttribute("currentTime", LocalDateTime.now());

        // 返回视图名称，对应 /WEB-INF/templates/test_page.html
        return "test_page";
    }

    @GetMapping("/error")
    public void testError() {
        throw new IllegalArgumentException("测试参数异常拦截");
    }

    @GetMapping("/db/mp")
    @ResponseBody
    public R<User> testMp() {
        // 使用 MP 自带的 selectById，不需要在 XML 里写 SQL
        // 假设数据库里有个 id=1 的用户
        User user = userMapper.selectById(1L);

        if (user == null) {
            return R.fail("未找到用户，但数据库连接正常");
        }
        return R.ok(user);
    }
}