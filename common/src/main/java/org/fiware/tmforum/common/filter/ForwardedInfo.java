package org.fiware.tmforum.common.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@ToString
@Data
public class ForwardedInfo {

	private String forwardedFor;
	private String forwardedProto;
	private String forwardedHost;
	private int forwardedPort;
	private String forwardedBy;
	private String forwardedPrefix;

	public void setForwardedPortStr(String portStr) {
		if (portStr == null || portStr.isEmpty()) {
			return;
		}
		this.forwardedPort = Integer.parseInt(portStr);
	}

	public String getForwardedPrefix() {
		return forwardedPrefix != null ? forwardedPrefix : "";
	}
}
