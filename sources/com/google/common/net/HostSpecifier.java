package com.google.common.net;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.text.ParseException;
import javax.annotation.Nullable;

@Beta
public final class HostSpecifier {
    private final String canonicalForm;

    private HostSpecifier(String canonicalForm) {
        this.canonicalForm = canonicalForm;
    }

    public static HostSpecifier fromValid(String specifier) {
        HostAndPort parsedHost = HostAndPort.fromString(specifier);
        Preconditions.checkArgument(parsedHost.hasPort() ^ 1);
        String host = parsedHost.getHostText();
        InetAddress addr = null;
        try {
            addr = InetAddresses.forString(host);
        } catch (IllegalArgumentException e) {
        }
        if (addr != null) {
            return new HostSpecifier(InetAddresses.toUriString(addr));
        }
        InternetDomainName domain = InternetDomainName.from(host);
        if (domain.hasPublicSuffix()) {
            return new HostSpecifier(domain.toString());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Domain name does not have a recognized public suffix: ");
        stringBuilder.append(host);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static HostSpecifier from(String specifier) throws ParseException {
        try {
            return fromValid(specifier);
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid host specifier: ");
            stringBuilder.append(specifier);
            ParseException parseException = new ParseException(stringBuilder.toString(), 0);
            parseException.initCause(e);
            throw parseException;
        }
    }

    public static boolean isValid(String specifier) {
        try {
            fromValid(specifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HostSpecifier)) {
            return false;
        }
        return this.canonicalForm.equals(((HostSpecifier) other).canonicalForm);
    }

    public int hashCode() {
        return this.canonicalForm.hashCode();
    }

    public String toString() {
        return this.canonicalForm;
    }
}
