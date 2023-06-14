package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * 检查是否完成登入
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //获取本次请求的uri
        String requestURI = request.getRequestURI();
        log.info("拦截道请求{}", requestURI);
        //这里我们定义一个需要放行的urls的路径(主要是登入页面和退出页面，以及所有的静态资源)
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };
        //这里就是匹配，匹配成功就放行
        boolean check = check(urls, requestURI);
        if(check){
//            log.info("本次{}请求不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        //然后其他请求就是判断是否已经登入(网页端)
        if(request.getSession().getAttribute("employee") != null){
//            log.info("用户已经登入， 登入id为{}", request.getSession().getAttribute("employee"));
            Long id = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(id);
            filterChain.doFilter(request, response);
            return;
        }
        //然后其他请求就是判断是否已经登入(移动端用户)
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已经登入， 登入id为{}", request.getSession().getAttribute("user"));
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request, response);
            return;
        }
        //如果还没登入，通过输出流的方式向客户端页面响应数据
        log.info("用户还未登入");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 将路径与与urls进行匹配，看是否需要进行拦截
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for(String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match)return true;
        }
        return false;
    }
}
