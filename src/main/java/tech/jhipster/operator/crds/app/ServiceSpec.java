package tech.jhipster.operator.crds.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceSpec implements KubernetesResource {

    private String serviceName;
    private String serviceVersion;
    private String servicePort;


    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServicePort() {
        return servicePort;
    }

    public void setServicePort(String servicePort) {
        this.servicePort = servicePort;
    }

    @Override
    public String toString() {
        return "ServiceSpec{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceVersion='" + serviceVersion + '\'' +
                ", servicePort='" + servicePort + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceSpec)) return false;
        ServiceSpec that = (ServiceSpec) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(serviceVersion, that.serviceVersion) &&
                Objects.equals(servicePort, that.servicePort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceVersion, servicePort);
    }
}
