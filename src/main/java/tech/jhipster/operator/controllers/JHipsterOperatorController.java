package tech.jhipster.operator.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.operator.AppsOperator;
import tech.jhipster.operator.app.AppService;

import java.util.Collection;

@RestController
public class JHipsterOperatorController {

    private Logger logger = LoggerFactory.getLogger(JHipsterOperatorController.class);

    @Autowired
    private AppService applicationsService;

    @Autowired
    private AppsOperator appsOperator;

    @GetMapping("/apps/")
    public Collection<String> appList() {
        return applicationsService.getApps();
    }


    @DeleteMapping("/apps/{appName}")
    public void deleteJHipsterApp(@PathVariable String appName) {
        logger.info("> Deleting Application: " + appName);
        appsOperator.deleteApp(appName);
    }


    @GetMapping("/status")
    public String serviceStatus() {
        return String.valueOf(appsOperator.isOn());
    }

    @DeleteMapping("/status")
    public void turnOnOff() {
        appsOperator.setOn(!appsOperator.isOn());
        if (appsOperator.isOn()) {
            appsOperator.bootstrap();
        }
        logger.info("JHipster K8s Operator is now: " + ((appsOperator.isOn()) ? "ON" : "OFF"));
    }

}
