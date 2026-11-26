package org.fiware.tmforum.common.test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Optional;

/**
 * Resolves the host an NGSI-LD broker reaches the test server at.
 * <p>
 * The ITs run the broker inside the k3s container (see the manifests in {@code k3s/}), so a notification sent to
 * {@code localhost} never arrives: inside that container, localhost is the broker itself. The container does reach
 * the test server on the gateway of its docker bridge, which is the address of the host's {@code docker0} interface.
 * <p>
 * Set {@code -D} {@value #HOST_PROPERTY} {@code =<host>} to pin another address, e.g. for a different container
 * runtime, or {@code localhost} where the test JVM shares the broker's network namespace.
 */
public final class BrokerCallbackHost {

	/** System property that pins the callback host instead of looking it up. */
	public static final String HOST_PROPERTY = "test.broker-callback-host";

	/** Host interface whose address the containers on the default docker bridge use as their gateway. */
	private static final String DOCKER_BRIDGE_INTERFACE = "docker0";

	/** Used when there is no docker bridge, which is also correct if the broker shares the test JVM's network. */
	private static final String FALLBACK_HOST = "localhost";

	private BrokerCallbackHost() {
	}

	/**
	 * The host part of a callback URI the broker can connect to: the pinned {@value #HOST_PROPERTY}, else the
	 * docker bridge address, else {@value #FALLBACK_HOST}.
	 */
	public static String resolve() {
		String pinned = System.getProperty(HOST_PROPERTY);
		if (pinned != null && !pinned.isBlank()) {
			return pinned.trim();
		}
		return dockerBridgeAddress().orElse(FALLBACK_HOST);
	}

	private static Optional<String> dockerBridgeAddress() {
		try {
			NetworkInterface dockerBridge = NetworkInterface.getByName(DOCKER_BRIDGE_INTERFACE);
			if (dockerBridge == null) {
				return Optional.empty();
			}
			return Collections.list(dockerBridge.getInetAddresses()).stream()
					.filter(Inet4Address.class::isInstance)
					.map(InetAddress::getHostAddress)
					.findFirst();
		} catch (SocketException e) {
			return Optional.empty();
		}
	}
}
