package com.hmall.common.aop;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
public class userAspect {

    // 拦截所有 @RestController 中的方法
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable{
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();
            String userInfo = request.getHeader("userInfo");
            if (StrUtil.isNotBlank(userInfo)) {
                UserContext.setUser(Long.valueOf(userInfo));
            }
        }
        try {
            return pjp.proceed();          // 执行真正的 Controller 方法
        } finally {
            UserContext.removeUser();      // 必须用 finally 清理，否则线程池复用时数据串号
        }
    }
}
