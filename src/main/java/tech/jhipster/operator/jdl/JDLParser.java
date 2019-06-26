package tech.jhipster.operator.jdl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This is not a serious parser, this is just to demonstrate what can be done if we have a JDL Java Class Model available
 * in other words.. this sucks.. please don't use it :)
 */
public class JDLParser {

    private static final String CFG_REGEX = "application(\\s|\\t)*\\{(\\s|\\t)*config(\\s|\\t)*\\{(?<cfg>.*?)\\}";
    private static final String BN_REGEX = "(\\s|\\t)*baseName(\\s|\\t)*(?<bn>[A-Za-z0-9]+)";
    private static final String AT_REGEX = "(\\s|\\t)*applicationType(\\s|\\t)*(?<at>[A-Za-z0-9]+)";
    private static final String SP_REGEX = "(\\s|\\t)*serverPort(\\s|\\t)*(?<sp>[A-Za-z0-9]+)";

    private static final Pattern CFG_PATTERN = Pattern.compile(CFG_REGEX);
    private static final Pattern BN_PATTERN = Pattern.compile(BN_REGEX);
    private static final Pattern AT_PATTERN = Pattern.compile(AT_REGEX);
    private static final Pattern SP_PATTERN = Pattern.compile(SP_REGEX);

    public static JHipsterApplicationDefinition parse(String name, String version, String jdl) {
        JHipsterApplicationDefinition jHipsterApplicationDefinition = new JHipsterApplicationDefinition(name, version);
        jHipsterApplicationDefinition.setJDLContent(jdl);
        jdl = jdl.replaceAll("\\n", " ");

        Matcher cfgMatcher = CFG_PATTERN.matcher(jdl);

        while (cfgMatcher.find()) {
            JHipsterModuleDefinition jHipsterModuleDefinition = new JHipsterModuleDefinition();
            String cfg = cfgMatcher.group("cfg");

            jHipsterModuleDefinition.setName(getGroup(cfg, BN_PATTERN, "bn"));
            jHipsterModuleDefinition.setType(getGroup(cfg, AT_PATTERN, "at"));
            jHipsterModuleDefinition.setPort(getGroup(cfg, SP_PATTERN, "sp"));

            jHipsterApplicationDefinition.addModule(jHipsterModuleDefinition);
        }

        return jHipsterApplicationDefinition;
    }

    public static String fromJDLServiceToKind(String type) {
        switch (type) {
            case "gateway":
                return "Gateway";
            case "microservice":
                return "MicroService";
            case "registry":
                return "Registry";
            default:
                return "N/A";
        }
    }

    private static String getGroup(String src, Pattern pattern, String groupName) {
        Matcher matcher = pattern.matcher(src);
        return matcher.find() ? matcher.group(groupName) : null;
    }
}
