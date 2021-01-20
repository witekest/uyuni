/*
  Copyright (c) 2021 SUSE LLC

  This software is licensed to you under the GNU General Public License,
  version 2 (GPLv2). There is NO WARRANTY for this software, express or
  implied, including the implied warranties of MERCHANTABILITY or FITNESS
  FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  along with this software; if not, see
  http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.

  Red Hat trademarks are not licensed under GPLv2. No permission is
  granted to use or replicate Red Hat trademarks that are incorporated
  in this software or its documentation.
 */

package com.redhat.rhn.domain.dto;

/**
 * Class for representing the endpoint information of applications or Prometheus exporters of a minion system
 */
public class EndpointInfo {

    private Long systemID;
    private String endpointName;
    private Integer port;
    private String path;
    private String module;
    private String exporterName;

    /**
     * Instantiates a new endpoint information
     * @param systemIDIn server ID
     * @param endpointNameIn endpoint name
     */
    public EndpointInfo(Long systemIDIn, String endpointNameIn) {
        this.systemID = systemIDIn;
        this.endpointName = endpointNameIn;
    }

    public Long getSystemID() {
        return systemID;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer portIn) {
        this.port = portIn;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String pathIn) {
        this.path = pathIn;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String moduleIn) {
        this.module = moduleIn;
    }

    public String getExporterName() {
        return exporterName;
    }

    public void setExporterName(String exporterNameIn) {
        this.exporterName = exporterNameIn;
    }

    @Override
    public String toString() {
        return "EndpointInfo{" +
                "systemID=" + systemID +
                ", endpointName='" + endpointName + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", module='" + module + '\'' +
                ", exporterName='" + exporterName + '\'' +
                '}';
    }
}
