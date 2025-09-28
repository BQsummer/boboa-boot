package com.bqsummer.plugin.metric;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;

@WebEndpoint(id = "business")
@Component
public class BusinessMetricsEndpoint extends AbstractMetricsEndpoint implements InitializingBean {

    @Autowired
    private BusinessMetrics businessMetrics;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.metrics = businessMetrics;
    }
}
