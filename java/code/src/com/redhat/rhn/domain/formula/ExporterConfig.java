package com.redhat.rhn.domain.formula;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExporterConfig {

    private static final String PORT_ARG_REGEX = "--(?:telemetry\\.address|web\\.listen-address)=[\"']?:([0-9]*)[\"']?";
    private static final String PORT_ADDRESS_REGEX = ":([0-9]*)$";

    private String name;
    private String address;
    private String args;

    public ExporterConfig(String name, String address, String args) {
        this.name = name;
        this.address = address;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public Integer getPort() {
        Optional<Integer> port = getPatternMatchGroupAsInteger(PORT_ARG_REGEX, args);
        if (port.isEmpty()) {
            port = getPatternMatchGroupAsInteger(PORT_ADDRESS_REGEX, address);
        }
        return port.orElse(null);
    }

    private Optional<Integer> getPatternMatchGroupAsInteger(String regex, String input) {
        Optional<Integer> intGroup = Optional.empty();
        Optional<String> optInput = Optional.ofNullable(input);
        if (optInput.isPresent()) {
            Pattern intPattern = Pattern.compile(regex);
            Matcher intMatcher = intPattern.matcher(optInput.get());
            if (intMatcher.find()) {
                intGroup = Optional.of(Integer.valueOf(intMatcher.group(1)));
            }
        }
        return intGroup;
    }
}
