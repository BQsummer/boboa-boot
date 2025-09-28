package com.bqsummer.framework.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

@Service
@Slf4j
public class JobExecutorRegister implements ApplicationContextAware, InitializingBean {

    @Autowired
    public SchedulerFactoryBean factory;

    private ApplicationContext applicationContext;

    static {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void registerAllJobs(SchedulerFactoryBean factory) throws SchedulerException {
        //  deleteExistJob(factory);
        factory.getScheduler().clear();
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(JobInfo.class);
        registerJobs(factory, beansWithAnnotation);
    }

    public void registerJobs(SchedulerFactoryBean factory, Map<String, Object> beansWithAnnotation) throws SchedulerException {
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            JobInfo annotation = AnnotationUtils.findAnnotation(entry.getValue().getClass(), JobInfo.class);
            Object jobExecutorObj = entry.getValue();
            if (annotation != null && jobExecutorObj instanceof JobExecutor) {
                if (StringUtils.isBlank(annotation.jobName()) || StringUtils.isBlank(annotation.cron())) {
                    log.error("job name or cron is blank. name = {}, cron = {}", annotation.jobName(), annotation.cron());
                    continue;
                }
                JobExecutor bean = (JobExecutor) jobExecutorObj;
                String jobName = entry.getKey();
                addJob(factory.getScheduler(), bean, annotation);
                log.info("register job : {}", jobName);
            }
        }
    }

    public void deleteExistJob(SchedulerFactoryBean factory) throws SchedulerException {
        Map<String, Pair<JobDetail, List<Trigger>>> existJob = listExistJob(factory.getScheduler());
        deleteJob(factory.getScheduler(), existJob);
    }

    public void deleteJob(Scheduler scheduler, Map<String, Pair<JobDetail, List<Trigger>>> existJob) throws SchedulerException {
        for (Map.Entry<String, Pair<JobDetail, List<Trigger>>> entry : existJob.entrySet()) {
            if (!CollectionUtils.isEmpty(entry.getValue().getValue())) {
                for (Trigger trigger : entry.getValue().getValue()) {
                    scheduler.unscheduleJob(trigger.getKey());
                    log.info("unschedule trigger : {}", trigger.getKey().getName());
                }
            }
            scheduler.deleteJob(entry.getValue().getKey().getKey());
            log.info("delete job : {}", entry.getKey());
        }
    }

    public void addJob(Scheduler scheduler, JobExecutor bean, JobInfo annotation) throws SchedulerException {
        String jobName = getJobName(bean);
        JobDetail job = newJob(bean.getClass())
                .withIdentity(jobName, JobConstants.DEFAULT_JOB_GROUP_NAME)
                .build();
        scheduler.scheduleJob(job, buildCronTrigger(job, jobName, annotation));
    }

//    public void updateJob(Scheduler scheduler, JobExecutor bean, Pair<JobDetail, List<Trigger>> existMap, JobInfo annotation) throws SchedulerException {
//        if (CollectionUtils.isEmpty(existMap.getValue())) {
//            addJob(scheduler, bean, annotation);
//            return;
//        }
//        boolean hasOneTrigger = false;
//        for (Trigger trigger : existMap.getValue()) {
//            if (hasOneTrigger) {
//                scheduler.unscheduleJob(trigger.getKey());
//            }
//            if (trigger instanceof CronTrigger) {
//                String jobName = getJobName(bean);
//                CronTrigger newCronTrigger = buildCronTrigger(jobName, annotation);
//                if (!triggerMatch((CronTrigger) trigger, newCronTrigger)) {
//                    scheduler.unscheduleJob(trigger.getKey());
//                }
//                hasOneTrigger = true;
//            } else {
//                scheduler.unscheduleJob(trigger.getKey());
//            }
//        }
//    }

    public CronTrigger buildCronTrigger(JobDetail job, String jobName, JobInfo annotation) {
        return newTrigger()
                .forJob(job)
                .withIdentity(buildTriggerName(jobName), JobConstants.DEFAULT_TRIGGER_GROUP_NAME)
                .withSchedule(cronSchedule(annotation.cron()))
                .startNow()
                .build();
    }

    public boolean triggerMatch(CronTrigger exist, CronTrigger current) {
        return StringUtils.equals(exist.getCronExpression(), current.getCronExpression());
    }

    public String buildTriggerName(String jobName) {
        return jobName + JobConstants.TRIGGER_SUFFIX;
    }

    public String getJobName(JobExecutor job) {
        Class<? extends Object> jobExecutorClass = job.getClass();
        JobInfo annotation = jobExecutorClass.getAnnotation(JobInfo.class);
        String jobName = annotation.jobName();
        if (StringUtils.isBlank(jobName)) {
            jobName = jobExecutorClass.getSimpleName();
            jobName = jobName.substring(0, 1).toLowerCase() + jobName.substring(1);
        }
        return jobName;
    }

    /**
     * org.quartz.JobPersistenceException: Couldn't retrieve job because a required class was not found: com.bqsummer.job.TestJob
     *
     * @param scheduler
     * @return
     * @throws SchedulerException
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public Map<String, Pair<JobDetail, List<Trigger>>> listExistJob(Scheduler scheduler) throws SchedulerException {
        Map<String, Pair<JobDetail, List<Trigger>>> map = new HashMap<>();
        for (String group : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(groupEquals(group))) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                List<Trigger> jobTriggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                map.put(jobKey.getName(), Pair.of(jobDetail, jobTriggers));
            }
        }
        return map;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerAllJobs(factory);
    }
}
