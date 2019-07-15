package tech.jhipster.operator.crds.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.Objects;
import java.util.Set;

@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationSpec implements KubernetesResource {

    private String version;
    private String selector;

    private Set<MicroServiceDescr> microservices;

    private String registry;
    private String gateway;

    private String status = "UNKNOWN";

    private String url = "NO URL YET.";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Set<MicroServiceDescr> getMicroservices() {
        return microservices;
    }

    public void setMicroservices(Set<MicroServiceDescr> microservices) {
        this.microservices = microservices;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }


    @Override
    public String toString() {
        return "ApplicationSpec{" +
                "version='" + version + '\'' +
                ", selector='" + selector + '\'' +
                ", microservices=" + microservices +
                ", registry='" + registry + '\'' +
                ", gateway='" + gateway + '\'' +
                ", status='" + status + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationSpec)) return false;
        ApplicationSpec that = (ApplicationSpec) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(selector, that.selector) &&
                Objects.equals(microservices, that.microservices) &&
                Objects.equals(registry, that.registry) &&
                Objects.equals(gateway, that.gateway) &&
                Objects.equals(status, that.status) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, selector, microservices, registry, gateway, status, url);
    }
}
