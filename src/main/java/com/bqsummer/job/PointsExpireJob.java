package com.bqsummer.job;

import com.bqsummer.framework.job.JobExecutor;
import com.bqsummer.framework.job.JobInfo;
import com.bqsummer.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@JobInfo(jobName = "pointsExpireJob", cron = "0 0 1 * * ?")
@RequiredArgsConstructor
public class PointsExpireJob extends JobExecutor {

    private final PointsService pointsService;

    @Override
    public void execute(JobExecutionContext context) {
        int total = 0;
        int batch;
        do {
            batch = pointsService.expireBucketsOnce(200);
            total += batch;
        } while (batch > 0);
        log.info("PointsExpireJob processed {} expired buckets", total);
    }
}

