package tech.jhipster.operator.controllers;

import java.util.Objects;

public class NewJHipsterAppRequest {
    private String name;
    private String version;
    private String appJDLContent;


    public NewJHipsterAppRequest() {
    }

    public NewJHipsterAppRequest(String name, String version, String appJDLContent) {
        this.name = name;
        this.version = version;
        this.appJDLContent = appJDLContent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppJDLContent() {
        return appJDLContent;
    }

    public void setAppJDLContent(String appJDLContent) {
        this.appJDLContent = appJDLContent;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewJHipsterAppRequest)) return false;
        NewJHipsterAppRequest that = (NewJHipsterAppRequest) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(appJDLContent, that.appJDLContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, appJDLContent);
    }

    @Override
    public String toString() {
        return "NewJHipsterAppRequest{" +
                "name='" + name + '\'' +
                ", appJDLContent='" + appJDLContent + '\'' +
                '}';
    }
}
