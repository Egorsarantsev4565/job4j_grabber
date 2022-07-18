package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    public static void main(String[] args) {
        ClassLoader loader = AlertRabbit.class.getClassLoader();
        try (InputStream io = loader.getResourceAsStream("rabbit.properties")) {
            Properties properties = new Properties();
            properties.load(io);
            Class.forName(properties.getProperty("driver"));
            String url = properties.getProperty("url");
            String login = properties.getProperty("login");
            String password = properties.getProperty("password");
            try (Connection connection = DriverManager.getConnection(url, login, password)) {
                int interval = Integer.parseInt(properties.getProperty("rabbit.interval"));
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap data = new JobDataMap();
                data.put("store", connection);
                JobDetail job = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(interval)
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
            }
        } catch (IOException | ClassNotFoundException
                | SQLException | SchedulerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

        public static class Rabbit implements Job {
            public Rabbit() {
                System.out.println(hashCode());
            }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here...");
            Connection connection = (Connection) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("store");
            try (PreparedStatement ps = connection.
                    prepareStatement("insert into rabbit(created_date) values (?)")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.execute();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}