package com.ecommerce.sb_ecom.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// this is not a project class, it is made vby me for practice. i can delte it
import java.io.IOException;
//@Component
public class LogFilter implements Filter
{

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        HttpServletResponse httpServletResponse=(HttpServletResponse) response;

        System.out.println(httpServletRequest.getMethod());;
        System.out.println(httpServletRequest.getRequestURI());;

        chain.doFilter(request,response);

        System.out.println(httpServletResponse.getStatus());

    }
}
