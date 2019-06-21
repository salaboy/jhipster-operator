package tech.jhipster.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
public class MyApplication {
    private Logger logger = LoggerFactory.getLogger(MyApplication.class);

    @Autowired
    private AppsOperator appsOperator;


    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class,
                args);
    }


    @Scheduled(fixedDelay = 10000)
    public void reconcileLoop() {
        if (appsOperator.isOn()) {
            if (appsOperator.isInitDone()) {
                logger.info("+ --------------------- RECONCILE LOOP -------------------- + ");
                appsOperator.reconcile();
                logger.info("+ --------------------- END RECONCILE  -------------------- +\n\n\n ");
            } else {
                // Bootstrap
                logger.info("> JHipster Operator Bootstrapping ... ");
                appsOperator.bootstrap();
            }
        }
    }

}
