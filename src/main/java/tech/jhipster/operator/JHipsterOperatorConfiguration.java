package tech.jhipster.operator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JHipsterOperatorConfiguration {
    @Value("k8s.service.checks.enabled")
    private boolean k8sServiceCheckEnabled = false;

    public boolean isK8sServiceCheckEnabled() {
        return k8sServiceCheckEnabled;
    }
}
