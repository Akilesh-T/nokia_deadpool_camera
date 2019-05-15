package com.google.common.net;

import android.support.v4.internal.view.SupportMenu;
import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.Serializable;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@GwtCompatible
@Immutable
@Beta
public final class HostAndPort implements Serializable {
    private static final int NO_PORT = -1;
    private static final long serialVersionUID = 0;
    private final boolean hasBracketlessColons;
    private final String host;
    private final int port;

    private HostAndPort(String host, int port, boolean hasBracketlessColons) {
        this.host = host;
        this.port = port;
        this.hasBracketlessColons = hasBracketlessColons;
    }

    public String getHostText() {
        return this.host;
    }

    public boolean hasPort() {
        return this.port >= 0;
    }

    public int getPort() {
        Preconditions.checkState(hasPort());
        return this.port;
    }

    public int getPortOrDefault(int defaultPort) {
        return hasPort() ? this.port : defaultPort;
    }

    public static HostAndPort fromParts(String host, int port) {
        Preconditions.checkArgument(isValidPort(port), "Port out of range: %s", Integer.valueOf(port));
        HostAndPort parsedHost = fromString(host);
        Preconditions.checkArgument(parsedHost.hasPort() ^ 1, "Host has a port: %s", host);
        return new HostAndPort(parsedHost.host, port, parsedHost.hasBracketlessColons);
    }

    public static HostAndPort fromHost(String host) {
        HostAndPort parsedHost = fromString(host);
        Preconditions.checkArgument(parsedHost.hasPort() ^ 1, "Host has a port: %s", host);
        return parsedHost;
    }

    public static HostAndPort fromString(String hostPortString) {
        String host;
        int colonPos;
        Preconditions.checkNotNull(hostPortString);
        String portString = null;
        boolean hasBracketlessColons = false;
        if (hostPortString.startsWith("[")) {
            String[] hostAndPort = getHostAndPortFromBracketedHost(hostPortString);
            String host2 = hostAndPort[0];
            portString = hostAndPort[1];
            host = host2;
        } else {
            colonPos = hostPortString.indexOf(58);
            if (colonPos < 0 || hostPortString.indexOf(58, colonPos + 1) != -1) {
                host = hostPortString;
                hasBracketlessColons = colonPos >= 0;
            } else {
                host = hostPortString.substring(0, colonPos);
                portString = hostPortString.substring(colonPos + 1);
            }
        }
        colonPos = -1;
        if (!Strings.isNullOrEmpty(portString)) {
            Preconditions.checkArgument(portString.startsWith("+") ^ 1, "Unparseable port number: %s", hostPortString);
            try {
                colonPos = Integer.parseInt(portString);
                Preconditions.checkArgument(isValidPort(colonPos), "Port number out of range: %s", hostPortString);
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unparseable port number: ");
                stringBuilder.append(hostPortString);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return new HostAndPort(host, colonPos, hasBracketlessColons);
    }

    private static String[] getHostAndPortFromBracketedHost(String hostPortString) {
        Preconditions.checkArgument(hostPortString.charAt(0) == '[', "Bracketed host-port string must start with a bracket: %s", hostPortString);
        int colonIndex = hostPortString.indexOf(58);
        int closeBracketIndex = hostPortString.lastIndexOf(93);
        boolean z = colonIndex > -1 && closeBracketIndex > colonIndex;
        Preconditions.checkArgument(z, "Invalid bracketed host/port: %s", hostPortString);
        String host = hostPortString.substring(1, closeBracketIndex);
        if (closeBracketIndex + 1 == hostPortString.length()) {
            return new String[]{host, ""};
        }
        Preconditions.checkArgument(hostPortString.charAt(closeBracketIndex + 1) == ':', "Only a colon may follow a close bracket: %s", hostPortString);
        for (int i = closeBracketIndex + 2; i < hostPortString.length(); i++) {
            Preconditions.checkArgument(Character.isDigit(hostPortString.charAt(i)), "Port must be numeric: %s", hostPortString);
        }
        return new String[]{host, hostPortString.substring(closeBracketIndex + 2)};
    }

    public HostAndPort withDefaultPort(int defaultPort) {
        Preconditions.checkArgument(isValidPort(defaultPort));
        if (hasPort() || this.port == defaultPort) {
            return this;
        }
        return new HostAndPort(this.host, defaultPort, this.hasBracketlessColons);
    }

    public HostAndPort requireBracketsForIPv6() {
        Preconditions.checkArgument(this.hasBracketlessColons ^ 1, "Possible bracketless IPv6 literal: %s", this.host);
        return this;
    }

    public boolean equals(@Nullable Object other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (!(other instanceof HostAndPort)) {
            return false;
        }
        HostAndPort that = (HostAndPort) other;
        if (!(Objects.equal(this.host, that.host) && this.port == that.port && this.hasBracketlessColons == that.hasBracketlessColons)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hashCode(this.host, Integer.valueOf(this.port), Boolean.valueOf(this.hasBracketlessColons));
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(this.host.length() + 8);
        if (this.host.indexOf(58) >= 0) {
            builder.append('[');
            builder.append(this.host);
            builder.append(']');
        } else {
            builder.append(this.host);
        }
        if (hasPort()) {
            builder.append(':');
            builder.append(this.port);
        }
        return builder.toString();
    }

    private static boolean isValidPort(int port) {
        return port >= 0 && port <= SupportMenu.USER_MASK;
    }
}
