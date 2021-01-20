/**
 *
 * Copyright (c) 2021 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

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

    /**
     * Instantiates new exporter configuration object
     * @param exporterName exporter name
     * @param addressIn the address of the endpoint where metrics are exposed
     * @param argsIn the string with command line arguments
     */
    public ExporterConfig(String exporterName, String addressIn, String argsIn) {
        this.name = exporterName;
        this.address = addressIn;
        this.args = argsIn;
    }

    public String getName() {
        return name;
    }

    public void setName(String exporterName) {
        this.name = exporterName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String addressIn) {
        this.address = addressIn;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String argsIn) {
        this.args = argsIn;
    }

    /**
     * Get port number at which metrics are exposed
     * @return port number
     */
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
