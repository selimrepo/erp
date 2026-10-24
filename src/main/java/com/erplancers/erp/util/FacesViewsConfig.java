package com.erplancers.erp.util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class FacesViewsConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        context.setInitParameter(
                "org.omnifaces.FACES_VIEWS_SCAN_PATHS",
                "/*.xhtml"
        );
    }
}