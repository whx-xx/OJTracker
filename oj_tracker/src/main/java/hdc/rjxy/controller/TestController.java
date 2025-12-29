package hdc.rjxy.controller;

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
}