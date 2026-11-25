package controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.ConflictChecker;
import service.CourseManager;
import service.InstructorManager;
import service.ReportManager;
import service.RoomManager;
import service.ScheduleManager;
import util.SampleData;

/**
 * Spring configuration class that registers pure Java domain services as Spring beans.
 * Keeps business and service logic completely decoupled from Spring annotations.
 */
@Configuration
public class AppConfig {

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }

    @Bean
    public CourseManager courseManager() {
        return new CourseManager();
    }

    @Bean
    public InstructorManager instructorManager() {
        return new InstructorManager();
    }

    @Bean
    public ConflictChecker conflictChecker() {
        return new ConflictChecker();
    }

    @Bean
    public ScheduleManager scheduleManager(ConflictChecker conflictChecker,
                                           RoomManager roomManager,
                                           CourseManager courseManager,
                                           InstructorManager instructorManager,
                                           @Value("${clsms.load-sample-data:true}") boolean loadSampleData) {
        ScheduleManager scheduleManager = new ScheduleManager(conflictChecker, roomManager, courseManager, instructorManager);
        // The app starts empty. Sample data (PRD Appendix A) is loaded only when clsms.load-sample-data=true
        if (loadSampleData) {
            SampleData.loadSampleData(roomManager, courseManager, instructorManager);
        }
        return scheduleManager;
    }

    @Bean
    public ReportManager reportManager(ScheduleManager scheduleManager,
                                       RoomManager roomManager,
                                       CourseManager courseManager,
                                       InstructorManager instructorManager) {
        return new ReportManager(scheduleManager, roomManager, courseManager, instructorManager);
    }
}
