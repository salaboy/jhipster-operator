package tech.jhipster.operator.crds.microservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import tech.jhipster.operator.crds.app.CustomService;
import tech.jhipster.operator.crds.app.ServiceSpec;

import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroService extends CustomResource implements CustomService {
    private ServiceSpec spec;

    public ServiceSpec getSpec() {
        return spec;
    }

    public void setSpec(ServiceSpec spec) {
        this.spec = spec;
    }

    public String getKind(){
        return "MicroService";
    }

    @Override
    public String toString() {
        return "MicroService{" +
                "name='" + getMetadata().getName() + '\'' +
                ", spec=" + spec +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MicroService)) return false;
        MicroService that = (MicroService) o;
        return Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec);
    }
}
