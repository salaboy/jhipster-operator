package tech.jhipster.operator.crds.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Application extends CustomResource {

    private ApplicationSpec spec;


    public ApplicationSpec getSpec() {
        return spec;
    }

    public void setSpec(ApplicationSpec spec) {
        this.spec = spec;
    }


    @Override
    public String toString() {
        return "Application{" +
                super.toString() +
                "spec=" + spec +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Application)) return false;
        Application that = (Application) o;
        return spec.equals(that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec);
    }
}
