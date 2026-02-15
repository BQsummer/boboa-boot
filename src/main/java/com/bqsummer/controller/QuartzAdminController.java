package com.bqsummer.controller;

import com.bqsummer.common.vo.Response;
import com.bqsummer.common.vo.req.quartz.UpdateQuartzCronReq;
import com.bqsummer.framework.exception.SnorlaxClientException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/quartz")
@RequiredArgsConstructor
public class QuartzAdminController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final Scheduler scheduler;

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<QuartzJobView>> listJobs() throws SchedulerException {
        List<QuartzJobView> result = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers == null || triggers.isEmpty()) {
                    result.add(buildView(jobDetail, null, null));
                    continue;
                }

                for (Trigger trigger : triggers) {
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    result.add(buildView(jobDetail, trigger, triggerState));
                }
            }
        }

        result.sort(Comparator
                .comparing(QuartzJobView::getJobGroup, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(QuartzJobView::getJobName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(QuartzJobView::getTriggerGroup, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(QuartzJobView::getTriggerName, Comparator.nullsLast(String::compareToIgnoreCase)));

        return Response.success(result);
    }

    @PostMapping("/jobs/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Void> pauseJob(@RequestParam String jobName,
                                   @RequestParam(defaultValue = "DEFAULT") String jobGroup) throws SchedulerException {
        JobKey jobKey = buildAndCheckJobKey(jobName, jobGroup);
        scheduler.pauseJob(jobKey);
        log.info("Paused quartz job: {}.{}", jobGroup, jobName);
        return Response.success();
    }

    @PostMapping("/jobs/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Void> resumeJob(@RequestParam String jobName,
                                    @RequestParam(defaultValue = "DEFAULT") String jobGroup) throws SchedulerException {
        JobKey jobKey = buildAndCheckJobKey(jobName, jobGroup);
        scheduler.resumeJob(jobKey);
        log.info("Resumed quartz job: {}.{}", jobGroup, jobName);
        return Response.success();
    }

    @PostMapping("/jobs/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Void> triggerNow(@RequestParam String jobName,
                                     @RequestParam(defaultValue = "DEFAULT") String jobGroup) throws SchedulerException {
        JobKey jobKey = buildAndCheckJobKey(jobName, jobGroup);
        scheduler.triggerJob(jobKey);
        log.info("Triggered quartz job now: {}.{}", jobGroup, jobName);
        return Response.success();
    }

    @PutMapping("/jobs/cron")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Void> updateCron(@RequestParam String jobName,
                                     @RequestParam(defaultValue = "DEFAULT") String jobGroup,
                                     @Valid @RequestBody UpdateQuartzCronReq req) throws SchedulerException {
        String cronExpression = req.getCronExpression().trim();
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new SnorlaxClientException("Invalid cron expression: " + cronExpression);
        }

        JobKey jobKey = buildAndCheckJobKey(jobName, jobGroup);
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        CronTrigger oldTrigger = triggers.stream()
                .filter(trigger -> trigger instanceof CronTrigger)
                .map(trigger -> (CronTrigger) trigger)
                .findFirst()
                .orElseThrow(() -> new SnorlaxClientException("No cron trigger found for job: " + jobGroup + "." + jobName));

        TriggerKey triggerKey = oldTrigger.getKey();
        CronTrigger newTrigger = oldTrigger.getTriggerBuilder()
                .withSchedule(cronSchedule(cronExpression))
                .build();
        scheduler.rescheduleJob(triggerKey, newTrigger);
        log.info("Updated cron for quartz job {}.{}, trigger {}.{}, cron={}",
                jobGroup, jobName, triggerKey.getGroup(), triggerKey.getName(), cronExpression);
        return Response.success();
    }

    private JobKey buildAndCheckJobKey(String jobName, String jobGroup) throws SchedulerException {
        String cleanJobName = jobName == null ? "" : jobName.trim();
        String cleanJobGroup = jobGroup == null ? "" : jobGroup.trim();
        if (cleanJobName.isEmpty()) {
            throw new SnorlaxClientException("jobName cannot be blank");
        }
        if (cleanJobGroup.isEmpty()) {
            throw new SnorlaxClientException("jobGroup cannot be blank");
        }

        JobKey jobKey = JobKey.jobKey(cleanJobName, cleanJobGroup);
        if (!scheduler.checkExists(jobKey)) {
            throw new SnorlaxClientException("Job not found: " + cleanJobGroup + "." + cleanJobName);
        }
        return jobKey;
    }

    private QuartzJobView buildView(JobDetail jobDetail, Trigger trigger, Trigger.TriggerState triggerState) {
        QuartzJobView view = new QuartzJobView();
        view.setJobName(jobDetail.getKey().getName());
        view.setJobGroup(jobDetail.getKey().getGroup());
        view.setJobClass(jobDetail.getJobClass().getName());
        view.setDescription(jobDetail.getDescription());
        view.setDurable(jobDetail.isDurable());

        if (trigger != null) {
            view.setTriggerName(trigger.getKey().getName());
            view.setTriggerGroup(trigger.getKey().getGroup());
            view.setTriggerState(triggerState == null ? null : triggerState.name());
            view.setNextFireTime(formatDate(trigger.getNextFireTime()));
            view.setPreviousFireTime(formatDate(trigger.getPreviousFireTime()));
            view.setStartTime(formatDate(trigger.getStartTime()));
            view.setEndTime(formatDate(trigger.getEndTime()));
            view.setPriority(trigger.getPriority());
            view.setMisfireInstruction(trigger.getMisfireInstruction());
            if (trigger instanceof CronTrigger cronTrigger) {
                view.setCronExpression(cronTrigger.getCronExpression());
                view.setTimeZoneId(cronTrigger.getTimeZone() == null ? null : cronTrigger.getTimeZone().getID());
            }
        }

        return view;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(date.toInstant().atZone(ZONE_ID).toLocalDateTime());
    }

    @lombok.Data
    public static class QuartzJobView {
        private String jobName;
        private String jobGroup;
        private String jobClass;
        private String description;
        private boolean durable;

        private String triggerName;
        private String triggerGroup;
        private String triggerState;
        private String cronExpression;
        private String nextFireTime;
        private String previousFireTime;
        private String startTime;
        private String endTime;
        private Integer priority;
        private Integer misfireInstruction;
        private String timeZoneId;
    }
}
