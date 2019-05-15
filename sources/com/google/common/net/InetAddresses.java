package com.google.common.net;

import android.support.v4.internal.view.SupportMenu;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Nullable;

@Beta
public final class InetAddresses {
    private static final Inet4Address ANY4 = ((Inet4Address) forString("0.0.0.0"));
    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV6_PART_COUNT = 8;
    private static final Inet4Address LOOPBACK4 = ((Inet4Address) forString("127.0.0.1"));

    @Beta
    public static final class TeredoInfo {
        private final Inet4Address client;
        private final int flags;
        private final int port;
        private final Inet4Address server;

        public TeredoInfo(@Nullable Inet4Address server, @Nullable Inet4Address client, int port, int flags) {
            boolean z = port >= 0 && port <= SupportMenu.USER_MASK;
            Preconditions.checkArgument(z, "port '%s' is out of range (0 <= port <= 0xffff)", Integer.valueOf(port));
            boolean z2 = flags >= 0 && flags <= SupportMenu.USER_MASK;
            Preconditions.checkArgument(z2, "flags '%s' is out of range (0 <= flags <= 0xffff)", Integer.valueOf(flags));
            this.server = (Inet4Address) MoreObjects.firstNonNull(server, InetAddresses.ANY4);
            this.client = (Inet4Address) MoreObjects.firstNonNull(client, InetAddresses.ANY4);
            this.port = port;
            this.flags = flags;
        }

        public Inet4Address getServer() {
            return this.server;
        }

        public Inet4Address getClient() {
            return this.client;
        }

        public int getPort() {
            return this.port;
        }

        public int getFlags() {
            return this.flags;
        }
    }

    private InetAddresses() {
    }

    private static Inet4Address getInet4Address(byte[] bytes) {
        Preconditions.checkArgument(bytes.length == 4, "Byte array has invalid length for an IPv4 address: %s != 4.", Integer.valueOf(bytes.length));
        return (Inet4Address) bytesToInetAddress(bytes);
    }

    public static InetAddress forString(String ipString) {
        byte[] addr = ipStringToBytes(ipString);
        if (addr != null) {
            return bytesToInetAddress(addr);
        }
        throw new IllegalArgumentException(String.format("'%s' is not an IP string literal.", new Object[]{ipString}));
    }

    public static boolean isInetAddress(String ipString) {
        return ipStringToBytes(ipString) != null;
    }

    private static byte[] ipStringToBytes(String ipString) {
        boolean hasColon = false;
        boolean hasDot = false;
        for (int i = 0; i < ipString.length(); i++) {
            char c = ipString.charAt(i);
            if (c == '.') {
                hasDot = true;
            } else if (c == ':') {
                if (hasDot) {
                    return null;
                }
                hasColon = true;
            } else if (Character.digit(c, 16) == -1) {
                return null;
            }
        }
        if (hasColon) {
            if (hasDot) {
                ipString = convertDottedQuadToHex(ipString);
                if (ipString == null) {
                    return null;
                }
            }
            return textToNumericFormatV6(ipString);
        } else if (hasDot) {
            return textToNumericFormatV4(ipString);
        } else {
            return null;
        }
    }

    private static byte[] textToNumericFormatV4(String ipString) {
        String[] address = ipString.split("\\.", 5);
        if (address.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        int i = 0;
        while (i < bytes.length) {
            try {
                bytes[i] = parseOctet(address[i]);
                i++;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return bytes;
    }

    private static byte[] textToNumericFormatV6(String ipString) {
        String[] parts = ipString.split(":", 10);
        if (parts.length < 3 || parts.length > 9) {
            return null;
        }
        int partsHi;
        int partsLo;
        int skipIndex = -1;
        for (int i = 1; i < parts.length - 1; i++) {
            if (parts[i].length() == 0) {
                if (skipIndex >= 0) {
                    return null;
                }
                skipIndex = i;
            }
        }
        if (skipIndex >= 0) {
            partsHi = skipIndex;
            partsLo = (parts.length - skipIndex) - 1;
            if (parts[0].length() == 0) {
                partsHi--;
                if (partsHi != 0) {
                    return null;
                }
            }
            if (parts[parts.length - 1].length() == 0) {
                partsLo--;
                if (partsLo != 0) {
                    return null;
                }
            }
        }
        partsHi = parts.length;
        partsLo = 0;
        int partsSkipped = 8 - (partsHi + partsLo);
        if (!skipIndex < 0 ? partsSkipped >= 1 : partsSkipped == 0) {
            return null;
        }
        ByteBuffer rawBytes = ByteBuffer.allocate(16);
        int i2 = 0;
        while (i2 < partsHi) {
            try {
                rawBytes.putShort(parseHextet(parts[i2]));
                i2++;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        for (i2 = 0; i2 < partsSkipped; i2++) {
            rawBytes.putShort((short) 0);
        }
        for (NumberFormatException ex = partsLo; ex > null; ex--) {
            rawBytes.putShort(parseHextet(parts[parts.length - ex]));
        }
        return rawBytes.array();
    }

    private static String convertDottedQuadToHex(String ipString) {
        int lastColon = ipString.lastIndexOf(58);
        String initialPart = ipString.substring(0, lastColon + 1);
        byte[] quad = textToNumericFormatV4(ipString.substring(lastColon + 1));
        if (quad == null) {
            return null;
        }
        String penultimate = Integer.toHexString(((quad[0] & 255) << 8) | (quad[1] & 255));
        String ultimate = Integer.toHexString(((quad[2] & 255) << 8) | (quad[3] & 255));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(initialPart);
        stringBuilder.append(penultimate);
        stringBuilder.append(":");
        stringBuilder.append(ultimate);
        return stringBuilder.toString();
    }

    private static byte parseOctet(String ipPart) {
        int octet = Integer.parseInt(ipPart);
        if (octet <= 255 && (!ipPart.startsWith("0") || ipPart.length() <= 1)) {
            return (byte) octet;
        }
        throw new NumberFormatException();
    }

    private static short parseHextet(String ipPart) {
        int hextet = Integer.parseInt(ipPart, 16);
        if (hextet <= SupportMenu.USER_MASK) {
            return (short) hextet;
        }
        throw new NumberFormatException();
    }

    private static InetAddress bytesToInetAddress(byte[] addr) {
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    public static String toAddrString(InetAddress ip) {
        Preconditions.checkNotNull(ip);
        if (ip instanceof Inet4Address) {
            return ip.getHostAddress();
        }
        Preconditions.checkArgument(ip instanceof Inet6Address);
        byte[] bytes = ip.getAddress();
        int[] hextets = new int[8];
        for (int i = 0; i < hextets.length; i++) {
            hextets[i] = Ints.fromBytes((byte) 0, (byte) 0, bytes[2 * i], bytes[(2 * i) + 1]);
        }
        compressLongestRunOfZeroes(hextets);
        return hextetsToIPv6String(hextets);
    }

    private static void compressLongestRunOfZeroes(int[] hextets) {
        int bestRunStart = -1;
        int bestRunLength = -1;
        int runStart = -1;
        int i = 0;
        while (i < hextets.length + 1) {
            if (i >= hextets.length || hextets[i] != 0) {
                if (runStart >= 0) {
                    int runLength = i - runStart;
                    if (runLength > bestRunLength) {
                        bestRunStart = runStart;
                        bestRunLength = runLength;
                    }
                    runStart = -1;
                }
            } else if (runStart < 0) {
                runStart = i;
            }
            i++;
        }
        if (bestRunLength >= 2) {
            Arrays.fill(hextets, bestRunStart, bestRunStart + bestRunLength, -1);
        }
    }

    private static String hextetsToIPv6String(int[] hextets) {
        StringBuilder buf = new StringBuilder(39);
        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean thisIsNumber = hextets[i] >= 0;
            if (thisIsNumber) {
                if (lastWasNumber) {
                    buf.append(':');
                }
                buf.append(Integer.toHexString(hextets[i]));
            } else if (i == 0 || lastWasNumber) {
                buf.append("::");
            }
            lastWasNumber = thisIsNumber;
        }
        return buf.toString();
    }

    public static String toUriString(InetAddress ip) {
        if (!(ip instanceof Inet6Address)) {
            return toAddrString(ip);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(toAddrString(ip));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static InetAddress forUriString(String hostAddr) {
        String ipString;
        Preconditions.checkNotNull(hostAddr);
        int expectBytes;
        if (hostAddr.startsWith("[") && hostAddr.endsWith("]")) {
            ipString = hostAddr.substring(1, hostAddr.length() - 1);
            expectBytes = 16;
        } else {
            ipString = hostAddr;
            expectBytes = 4;
        }
        byte[] addr = ipStringToBytes(ipString);
        if (addr != null && addr.length == expectBytes) {
            return bytesToInetAddress(addr);
        }
        throw new IllegalArgumentException(String.format("Not a valid URI IP literal: '%s'", new Object[]{hostAddr}));
    }

    public static boolean isUriInetAddress(String ipString) {
        try {
            forUriString(ipString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isCompatIPv4Address(Inet6Address ip) {
        if (!ip.isIPv4CompatibleAddress()) {
            return false;
        }
        byte[] bytes = ip.getAddress();
        if (bytes[12] == (byte) 0 && bytes[13] == (byte) 0 && bytes[14] == (byte) 0 && (bytes[15] == (byte) 0 || bytes[15] == (byte) 1)) {
            return false;
        }
        return true;
    }

    public static Inet4Address getCompatIPv4Address(Inet6Address ip) {
        Preconditions.checkArgument(isCompatIPv4Address(ip), "Address '%s' is not IPv4-compatible.", toAddrString(ip));
        return getInet4Address(Arrays.copyOfRange(ip.getAddress(), 12, 16));
    }

    public static boolean is6to4Address(Inet6Address ip) {
        byte[] bytes = ip.getAddress();
        return bytes[0] == (byte) 32 && bytes[1] == (byte) 2;
    }

    public static Inet4Address get6to4IPv4Address(Inet6Address ip) {
        Preconditions.checkArgument(is6to4Address(ip), "Address '%s' is not a 6to4 address.", toAddrString(ip));
        return getInet4Address(Arrays.copyOfRange(ip.getAddress(), 2, 6));
    }

    public static boolean isTeredoAddress(Inet6Address ip) {
        byte[] bytes = ip.getAddress();
        return bytes[0] == (byte) 32 && bytes[1] == (byte) 1 && bytes[2] == (byte) 0 && bytes[3] == (byte) 0;
    }

    public static TeredoInfo getTeredoInfo(Inet6Address ip) {
        Object[] objArr = new Object[1];
        int i = 0;
        objArr[0] = toAddrString(ip);
        Preconditions.checkArgument(isTeredoAddress(ip), "Address '%s' is not a Teredo address.", objArr);
        byte[] bytes = ip.getAddress();
        Inet4Address server = getInet4Address(Arrays.copyOfRange(bytes, 4, 8));
        int flags = ByteStreams.newDataInput(bytes, 8).readShort() & SupportMenu.USER_MASK;
        int port = SupportMenu.USER_MASK & (~ByteStreams.newDataInput(bytes, 10).readShort());
        byte[] clientBytes = Arrays.copyOfRange(bytes, 12, 16);
        while (i < clientBytes.length) {
            clientBytes[i] = (byte) (~clientBytes[i]);
            i++;
        }
        return new TeredoInfo(server, getInet4Address(clientBytes), port, flags);
    }

    public static boolean isIsatapAddress(Inet6Address ip) {
        boolean z = false;
        if (isTeredoAddress(ip)) {
            return false;
        }
        byte[] bytes = ip.getAddress();
        if ((bytes[8] | 3) != 3) {
            return false;
        }
        if (bytes[9] == (byte) 0 && bytes[10] == (byte) 94 && bytes[11] == (byte) -2) {
            z = true;
        }
        return z;
    }

    public static Inet4Address getIsatapIPv4Address(Inet6Address ip) {
        Preconditions.checkArgument(isIsatapAddress(ip), "Address '%s' is not an ISATAP address.", toAddrString(ip));
        return getInet4Address(Arrays.copyOfRange(ip.getAddress(), 12, 16));
    }

    public static boolean hasEmbeddedIPv4ClientAddress(Inet6Address ip) {
        return isCompatIPv4Address(ip) || is6to4Address(ip) || isTeredoAddress(ip);
    }

    public static Inet4Address getEmbeddedIPv4ClientAddress(Inet6Address ip) {
        if (isCompatIPv4Address(ip)) {
            return getCompatIPv4Address(ip);
        }
        if (is6to4Address(ip)) {
            return get6to4IPv4Address(ip);
        }
        if (isTeredoAddress(ip)) {
            return getTeredoInfo(ip).getClient();
        }
        throw new IllegalArgumentException(String.format("'%s' has no embedded IPv4 address.", new Object[]{toAddrString(ip)}));
    }

    public static boolean isMappedIPv4Address(String ipString) {
        byte[] bytes = ipStringToBytes(ipString);
        if (bytes == null || bytes.length != 16) {
            return false;
        }
        int i = 0;
        while (true) {
            int i2 = 10;
            if (i >= 10) {
                while (true) {
                    i = i2;
                    if (i >= 12) {
                        return true;
                    }
                    if (bytes[i] != (byte) -1) {
                        return false;
                    }
                    i2 = i + 1;
                }
            } else if (bytes[i] != (byte) 0) {
                return false;
            } else {
                i++;
            }
        }
    }

    public static Inet4Address getCoercedIPv4Address(InetAddress ip) {
        if (ip instanceof Inet4Address) {
            return (Inet4Address) ip;
        }
        byte[] bytes = ip.getAddress();
        boolean leadingBytesOfZero = true;
        for (int i = 0; i < 15; i++) {
            if (bytes[i] != (byte) 0) {
                leadingBytesOfZero = false;
                break;
            }
        }
        if (leadingBytesOfZero && bytes[15] == (byte) 1) {
            return LOOPBACK4;
        }
        if (leadingBytesOfZero && bytes[15] == (byte) 0) {
            return ANY4;
        }
        long addressAsLong;
        Inet6Address ip6 = (Inet6Address) ip;
        if (hasEmbeddedIPv4ClientAddress(ip6)) {
            addressAsLong = (long) getEmbeddedIPv4ClientAddress(ip6).hashCode();
        } else {
            addressAsLong = ByteBuffer.wrap(ip6.getAddress(), 0, 8).getLong();
        }
        int coercedHash = Hashing.murmur3_32().hashLong(addressAsLong).asInt() | -536870912;
        if (coercedHash == -1) {
            coercedHash = -2;
        }
        return getInet4Address(Ints.toByteArray(coercedHash));
    }

    public static int coerceToInteger(InetAddress ip) {
        return ByteStreams.newDataInput(getCoercedIPv4Address(ip).getAddress()).readInt();
    }

    public static Inet4Address fromInteger(int address) {
        return getInet4Address(Ints.toByteArray(address));
    }

    public static InetAddress fromLittleEndianByteArray(byte[] addr) throws UnknownHostException {
        byte[] reversed = new byte[addr.length];
        for (int i = 0; i < addr.length; i++) {
            reversed[i] = addr[(addr.length - i) - 1];
        }
        return InetAddress.getByAddress(reversed);
    }

    public static InetAddress decrement(InetAddress address) {
        byte[] addr = address.getAddress();
        int i = addr.length - 1;
        while (i >= 0 && addr[i] == (byte) 0) {
            addr[i] = (byte) -1;
            i--;
        }
        Preconditions.checkArgument(i >= 0, "Decrementing %s would wrap.", address);
        addr[i] = (byte) (addr[i] - 1);
        return bytesToInetAddress(addr);
    }

    public static InetAddress increment(InetAddress address) {
        byte[] addr = address.getAddress();
        int i = addr.length - 1;
        while (i >= 0 && addr[i] == (byte) -1) {
            addr[i] = (byte) 0;
            i--;
        }
        Preconditions.checkArgument(i >= 0, "Incrementing %s would wrap.", address);
        addr[i] = (byte) (addr[i] + 1);
        return bytesToInetAddress(addr);
    }

    public static boolean isMaximum(InetAddress address) {
        byte[] addr = address.getAddress();
        for (byte b : addr) {
            if (b != (byte) -1) {
                return false;
            }
        }
        return true;
    }
}
