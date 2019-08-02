/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.disy.commons.web;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import okio.Buffer;

//import javax.annotation.Nullable;

/** Junk drawer of utility methods. */
final class WebUrlUtilities {

  /**
   * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
   * of Android's private InetAddress#isNumeric API.
   *
   * <p>This matches IPv6 addresses as a hex string containing at least one colon, and possibly
   * including dots after the first colon. It matches IPv4 addresses as strings containing only
   * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
   * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
   * verification).
   */
  private static final Pattern VERIFY_AS_IP_ADDRESS =
      Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

  private WebUrlUtilities() {}

  /**
   * Increments {@code pos} until {@code input[pos]} is not ASCII whitespace. Stops at {@code
   * limit}.
   */
  static int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
    for (int i = pos; i < limit; i++) {
      switch (input.charAt(i)) {
        case '\t':
        case '\n':
        case '\f':
        case '\r':
        case ' ':
          continue;
        default:
          return i;
      }
    }
    return limit;
  }

  /**
   * Decrements {@code limit} until {@code input[limit - 1]} is not ASCII whitespace. Stops at
   * {@code pos}.
   */
  static int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
    for (int i = limit - 1; i >= pos; i--) {
      switch (input.charAt(i)) {
        case '\t':
        case '\n':
        case '\f':
        case '\r':
        case ' ':
          continue;
        default:
          return i + 1;
      }
    }
    return pos;
  }

  /**
   * Returns the index of the first character in {@code input} that contains a character in {@code
   * delimiters}. Returns limit if there is no such character.
   */
  static int delimiterOffset(String input, int pos, int limit, String delimiters) {
    for (int i = pos; i < limit; i++) {
      if (delimiters.indexOf(input.charAt(i)) != -1) return i;
    }
    return limit;
  }

  /**
   * Returns the index of the first character in {@code input} that is {@code delimiter}. Returns
   * limit if there is no such character.
   */
  static int delimiterOffset(String input, int pos, int limit, char delimiter) {
    for (int i = pos; i < limit; i++) {
      if (input.charAt(i) == delimiter) return i;
    }
    return limit;
  }

  /**
   * If {@code host} is an IP address, this returns the IP address in canonical form.
   *
   * <p>Otherwise this performs IDN ToASCII encoding and canonicalize the result to lowercase. For
   * example this converts {@code ☃.net} to {@code xn--n3h.net}, and {@code WwW.GoOgLe.cOm} to
   * {@code www.google.com}. {@code null} will be returned if the host cannot be ToASCII encoded or
   * if the result contains unsupported ASCII characters.
   */
  static String canonicalizeHost(String host) {
    // If the input contains a :, it’s an IPv6 address.
    if (host.contains(":")) {
      // If the input is encased in square braces "[...]", drop 'em.
      InetAddress inetAddress = host.startsWith("[") && host.endsWith("]")
          ? decodeIpv6(host, 1, host.length() - 1)
          : decodeIpv6(host, 0, host.length());
      if (inetAddress == null) return null;
      byte[] address = inetAddress.getAddress();
      if (address.length == 16) return inet6AddressToAscii(address);
      if (address.length == 4) return inetAddress.getHostAddress(); // An IPv4-mapped IPv6 address.
      throw new AssertionError("Invalid IPv6 address: '" + host + "'");
    }

    try {
      String result = IDN.toASCII(host).toLowerCase(Locale.US);
      if (result.isEmpty()) return null;

      // Confirm that the IDN ToASCII result doesn't contain any illegal characters.
      if (containsInvalidHostnameAsciiCodes(result)) {
        return null;
      }
      // TODO: implement all label limits.
      return result;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
    for (int i = 0; i < hostnameAscii.length(); i++) {
      char c = hostnameAscii.charAt(i);
      // The WHATWG Host parsing rules accepts some character codes which are invalid by
      // definition for OkHttp's host header checks (and the WHATWG Host syntax definition). Here
      // we rule out characters that would cause problems in host headers.
      if (c <= '\u001f' || c >= '\u007f') {
        return true;
      }
      // Check for the characters mentioned in the WHATWG Host parsing spec:
      // U+0000, U+0009, U+000A, U+000D, U+0020, "#", "%", "/", ":", "?", "@", "[", "\", and "]"
      // (excluding the characters covered above).
      if (" #%/:?@[\\]".indexOf(c) != -1) {
        return true;
      }
    }
    return false;
  }

  /** Returns true if {@code host} is not a host name and might be an IP address. */
  static boolean verifyAsIpAddress(String host) {
    return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
  }

  static int decodeHexDigit(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
  }

  /** Decodes an IPv6 address like 1111:2222:3333:4444:5555:6666:7777:8888 or ::1. */
  private static /*@Nullable*/ InetAddress decodeIpv6(String input, int pos, int limit) {
    byte[] address = new byte[16];
    int b = 0;
    int compress = -1;
    int groupOffset = -1;

    for (int i = pos; i < limit; ) {
      if (b == address.length) return null; // Too many groups.

      // Read a delimiter.
      if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
        // Compression "::" delimiter, which is anywhere in the input, including its prefix.
        if (compress != -1) return null; // Multiple "::" delimiters.
        i += 2;
        b += 2;
        compress = b;
        if (i == limit) break;
      } else if (b != 0) {
        // Group separator ":" delimiter.
        if (input.regionMatches(i, ":", 0, 1)) {
          i++;
        } else if (input.regionMatches(i, ".", 0, 1)) {
          // If we see a '.', rewind to the beginning of the previous group and parse as IPv4.
          if (!decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) return null;
          b += 2; // We rewound two bytes and then added four.
          break;
        } else {
          return null; // Wrong delimiter.
        }
      }

      // Read a group, one to four hex digits.
      int value = 0;
      groupOffset = i;
      for (; i < limit; i++) {
        char c = input.charAt(i);
        int hexDigit = decodeHexDigit(c);
        if (hexDigit == -1) break;
        value = (value << 4) + hexDigit;
      }
      int groupLength = i - groupOffset;
      if (groupLength == 0 || groupLength > 4) return null; // Group is the wrong size.

      // We've successfully read a group. Assign its value to our byte array.
      address[b++] = (byte) ((value >>> 8) & 0xff);
      address[b++] = (byte) (value & 0xff);
    }

    // All done. If compression happened, we need to move bytes to the right place in the
    // address. Here's a sample:
    //
    //      input: "1111:2222:3333::7777:8888"
    //     before: { 11, 11, 22, 22, 33, 33, 00, 00, 77, 77, 88, 88, 00, 00, 00, 00  }
    //   compress: 6
    //          b: 10
    //      after: { 11, 11, 22, 22, 33, 33, 00, 00, 00, 00, 00, 00, 77, 77, 88, 88 }
    //
    if (b != address.length) {
      if (compress == -1) return null; // Address didn't have compression or enough groups.
      System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
      Arrays.fill(address, compress, compress + (address.length - b), (byte) 0);
    }

    try {
      return InetAddress.getByAddress(address);
    } catch (UnknownHostException e) {
      throw new AssertionError();
    }
  }

  /** Decodes an IPv4 address suffix of an IPv6 address, like 1111::5555:6666:192.168.0.1. */
  private static boolean decodeIpv4Suffix(
      String input, int pos, int limit, byte[] address, int addressOffset) {
    int b = addressOffset;

    for (int i = pos; i < limit; ) {
      if (b == address.length) return false; // Too many groups.

      // Read a delimiter.
      if (b != addressOffset) {
        if (input.charAt(i) != '.') return false; // Wrong delimiter.
        i++;
      }

      // Read 1 or more decimal digits for a value in 0..255.
      int value = 0;
      int groupOffset = i;
      for (; i < limit; i++) {
        char c = input.charAt(i);
        if (c < '0' || c > '9') break;
        if (value == 0 && groupOffset != i) return false; // Reject unnecessary leading '0's.
        value = (value * 10) + c - '0';
        if (value > 255) return false; // Value out of range.
      }
      int groupLength = i - groupOffset;
      if (groupLength == 0) return false; // No digits.

      // We've successfully read a byte.
      address[b++] = (byte) value;
    }

    if (b != addressOffset + 4) return false; // Too few groups. We wanted exactly four.
    return true; // Success.
  }

  /** Encodes an IPv6 address in canonical form according to RFC 5952. */
  private static String inet6AddressToAscii(byte[] address) {
    // Go through the address looking for the longest run of 0s. Each group is 2-bytes.
    // A run must be longer than one group (section 4.2.2).
    // If there are multiple equal runs, the first one must be used (section 4.2.3).
    int longestRunOffset = -1;
    int longestRunLength = 0;
    for (int i = 0; i < address.length; i += 2) {
      int currentRunOffset = i;
      while (i < 16 && address[i] == 0 && address[i + 1] == 0) {
        i += 2;
      }
      int currentRunLength = i - currentRunOffset;
      if (currentRunLength > longestRunLength && currentRunLength >= 4) {
        longestRunOffset = currentRunOffset;
        longestRunLength = currentRunLength;
      }
    }

    // Emit each 2-byte group in hex, separated by ':'. The longest run of zeroes is "::".
    Buffer result = new Buffer();
    for (int i = 0; i < address.length; ) {
      if (i == longestRunOffset) {
        result.writeByte(':');
        i += longestRunLength;
        if (i == 16) result.writeByte(':');
      } else {
        if (i > 0) result.writeByte(':');
        int group = (address[i] & 0xff) << 8 | address[i + 1] & 0xff;
        result.writeHexadecimalUnsignedLong(group);
        i += 2;
      }
    }
    return result.readUtf8();
  }
}
