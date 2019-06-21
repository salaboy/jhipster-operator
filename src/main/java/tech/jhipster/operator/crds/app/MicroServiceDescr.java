package tech.jhipster.operator.crds.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroServiceDescr {
    private String name;
    private String kind;
    private String serviceName;

    public MicroServiceDescr() {
    }

    public MicroServiceDescr(String name, String kind, String serviceName) {
        this.name = name;
        this.kind = kind;
        this.serviceName = serviceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "MicroServiceDescr{" +
                "name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MicroServiceDescr that = (MicroServiceDescr) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(kind, that.kind) &&
                Objects.equals(serviceName, that.serviceName);
    }
}
