package tech.jhipster.operator.crds.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import tech.jhipster.operator.crds.app.CustomService;
import tech.jhipster.operator.crds.app.ServiceSpec;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)

public class Registry extends CustomResource implements CustomService {
    private ServiceSpec spec;

    public ServiceSpec getSpec() {
        return spec;
    }

    public void setSpec(ServiceSpec spec) {
        this.spec = spec;
    }

    public String getKind() {
        return "Registry";
    }

    @Override
    public String toString() {
        return "Registry{" +
                "name='" + getMetadata().getName() + '\'' +
                ", spec=" + spec +
                '}';
    }
}
