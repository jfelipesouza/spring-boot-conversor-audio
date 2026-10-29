package com.jfelipesouza.conversor.download;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.jfelipesouza.conversor.converter.ConversionFailedException;

@Component
public class YouTubeUrlValidator {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be");

    public void validate(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidYouTubeUrlException();
        }

        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !ALLOWED_HOSTS.contains(host.toLowerCase(java.util.Locale.ROOT))
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getRawUserInfo() != null
                || uri.getFragment() != null) {
            throw new InvalidYouTubeUrlException();
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isNonPublic(address)) {
                    throw new InvalidYouTubeUrlException();
                }
            }
        } catch (UnknownHostException exception) {
            throw new ConversionFailedException();
        }
    }

    private boolean isNonPublic(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return (bytes[0] & 0xfe) == 0xfc;
        }
        if (!(address instanceof Inet4Address)) {
            return true;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        int third = bytes[2] & 0xff;
        return first == 0
                || first == 10
                || first == 127
                || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && (second == 168 || (second == 0 && third == 0)))
                || (first == 198 && (second == 18 || second == 19 || (second == 51 && third == 100)))
                || (first == 203 && second == 0 && third == 113);
    }
}
