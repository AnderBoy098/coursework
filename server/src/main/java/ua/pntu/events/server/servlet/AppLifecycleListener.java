package ua.pntu.events.server.servlet;

import ua.pntu.events.server.util.ServerLogging;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

/** Ініціалізація логування під час запуску та зупинки веб-застосунку в Tomcat. */
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppLifecycleListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServerLogging.init();
        LOG.info("Серверний застосунок запущено: " + sce.getServletContext().getContextPath());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Серверний застосунок зупинено");
    }
}
