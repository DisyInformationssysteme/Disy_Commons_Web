/*
 * Copyright (C) 2015 Square, Inc.
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
package net.disy.oss.weburl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.disy.oss.weburl.UrlComponentEncodingTester.Component;
import net.disy.oss.weburl.UrlComponentEncodingTester.Encoding;

public final class WebUrlTest {

  enum WebUrlFactoryMethod {
    GET, PARSE
  }

  @Retention(RetentionPolicy.RUNTIME)
  @ParameterizedTest(name = "Create WebUrl with {arguments}")
  @EnumSource(value = WebUrlFactoryMethod.class)
  public @interface ParameterizedTestWithWebUrlFactory { }

  private static WebUrl create(String url, WebUrlFactoryMethod method) {
    return method == WebUrlFactoryMethod.GET
        ? WebUrl.get(url)
        : WebUrl.parse(url).orElse(null);
  }

  private static void assertInvalid(String string, String exceptionMessage, WebUrlFactoryMethod method) {
    if (method == WebUrlFactoryMethod.GET) {
      try {
        create(string, method);
        fail("Expected get of \"" + string + "\" to throw with: " + exceptionMessage);
      } catch (IllegalArgumentException e) {
        assertThat(e.getMessage()).isEqualTo(exceptionMessage);
      }
    } else {
      assertThat(create(string, method)).overridingErrorMessage(string).isNull();
    }
  }


  @ParameterizedTestWithWebUrlFactory
  public void parseTrimsAsciiWhitespace(WebUrlFactoryMethod method) {
    WebUrl expected = create("http://host/", method);
    // Leading.
    assertThat(create("http://host/\f\n\t \r", method)).isEqualTo(expected);
    // Trailing.
    assertThat(create("\r\n\f \thttp://host/", method)).isEqualTo(expected);
    // Both.
    assertThat(create(" http://host/ ", method)).isEqualTo(expected);
    // Both.
    assertThat(create("    http://host/    ", method)).isEqualTo(expected);
    assertThat(create("http://host/", method).resolve("   ")).isEqualTo(expected);
    assertThat(create("http://host/", method).resolve("  .  ")).isEqualTo(expected);
  }

  @ParameterizedTestWithWebUrlFactory
  public void parseHostAsciiNonPrintable(WebUrlFactoryMethod method) {
    String host = "host\u0001";
    assertInvalid("http://" + host + "/", "Invalid URL host: \"host\u0001\"", method);
    // TODO make exception message escape non-printable characters
  }

  @ParameterizedTestWithWebUrlFactory
  public void parseDoesNotTrimOtherWhitespaceCharacters(WebUrlFactoryMethod method) {
    // Whitespace characters list from Google's Guava team: http://goo.gl/IcR9RD
    // line tabulation
    assertThat(create("http://h/\u000b", method).encodedPath()).isEqualTo("/%0B");
    // information separator 4
    assertThat(create("http://h/\u001c", method).encodedPath()).isEqualTo("/%1C");
    // information separator 3
    assertThat(create("http://h/\u001d", method).encodedPath()).isEqualTo("/%1D");
    // information separator 2
    assertThat(create("http://h/\u001e", method).encodedPath()).isEqualTo("/%1E");
    // information separator 1
    assertThat(create("http://h/\u001f", method).encodedPath()).isEqualTo("/%1F");
    // next line
    assertThat(create("http://h/\u0085", method).encodedPath()).isEqualTo("/%C2%85");
    // non-breaking space
    assertThat(create("http://h/\u00a0", method).encodedPath()).isEqualTo("/%C2%A0");
    // ogham space mark
    assertThat(create("http://h/\u1680", method).encodedPath()).isEqualTo("/%E1%9A%80");
    // mongolian vowel separator
    assertThat(create("http://h/\u180e", method).encodedPath()).isEqualTo("/%E1%A0%8E");
    // en quad
    assertThat(create("http://h/\u2000", method).encodedPath()).isEqualTo("/%E2%80%80");
    // em quad
    assertThat(create("http://h/\u2001", method).encodedPath()).isEqualTo("/%E2%80%81");
    // en space
    assertThat(create("http://h/\u2002", method).encodedPath()).isEqualTo("/%E2%80%82");
    // em space
    assertThat(create("http://h/\u2003", method).encodedPath()).isEqualTo("/%E2%80%83");
    // three-per-em space
    assertThat(create("http://h/\u2004", method).encodedPath()).isEqualTo("/%E2%80%84");
    // four-per-em space
    assertThat(create("http://h/\u2005", method).encodedPath()).isEqualTo("/%E2%80%85");
    // six-per-em space
    assertThat(create("http://h/\u2006", method).encodedPath()).isEqualTo("/%E2%80%86");
    // figure space
    assertThat(create("http://h/\u2007", method).encodedPath()).isEqualTo("/%E2%80%87");
    // punctuation space
    assertThat(create("http://h/\u2008", method).encodedPath()).isEqualTo("/%E2%80%88");
    // thin space
    assertThat(create("http://h/\u2009", method).encodedPath()).isEqualTo("/%E2%80%89");
    // hair space
    assertThat(create("http://h/\u200a", method).encodedPath()).isEqualTo("/%E2%80%8A");
    // zero-width space
    assertThat(create("http://h/\u200b", method).encodedPath()).isEqualTo("/%E2%80%8B");
    // zero-width non-joiner
    assertThat(create("http://h/\u200c", method).encodedPath()).isEqualTo("/%E2%80%8C");
    // zero-width joiner
    assertThat(create("http://h/\u200d", method).encodedPath()).isEqualTo("/%E2%80%8D");
    // left-to-right mark
    assertThat(create("http://h/\u200e", method).encodedPath()).isEqualTo("/%E2%80%8E");
    // right-to-left mark
    assertThat(create("http://h/\u200f", method).encodedPath()).isEqualTo("/%E2%80%8F");
    // line separator
    assertThat(create("http://h/\u2028", method).encodedPath()).isEqualTo("/%E2%80%A8");
    // paragraph separator
    assertThat(create("http://h/\u2029", method).encodedPath()).isEqualTo("/%E2%80%A9");
    // narrow non-breaking space
    assertThat(create("http://h/\u202f", method).encodedPath()).isEqualTo("/%E2%80%AF");
    // medium mathematical space
    assertThat(create("http://h/\u205f", method).encodedPath()).isEqualTo("/%E2%81%9F");
    // ideographic space
    assertThat(create("http://h/\u3000", method).encodedPath()).isEqualTo("/%E3%80%80");
  }

  @ParameterizedTestWithWebUrlFactory
  public void scheme(WebUrlFactoryMethod method) {
    assertThat(create("http://host/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("Http://host/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("http://host/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("HTTP://host/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("https://host/", method)).isEqualTo(create("https://host/", method));
    assertThat(create("HTTPS://host/", method)).isEqualTo(create("https://host/", method));

    assertInvalid("image640://480.png", "Expected URL scheme 'http' or 'https' but was 'image640'", method);
    assertInvalid("httpp://host/", "Expected URL scheme 'http' or 'https' but was 'httpp'", method);
    assertInvalid("0ttp://host/", "Expected URL scheme 'http' or 'https' but no colon was found", method);
    assertInvalid("ht+tp://host/", "Expected URL scheme 'http' or 'https' but was 'ht+tp'", method);
    assertInvalid("ht.tp://host/", "Expected URL scheme 'http' or 'https' but was 'ht.tp'", method);
    assertInvalid("ht-tp://host/", "Expected URL scheme 'http' or 'https' but was 'ht-tp'", method);
    assertInvalid("ht1tp://host/", "Expected URL scheme 'http' or 'https' but was 'ht1tp'", method);
    assertInvalid("httpss://host/", "Expected URL scheme 'http' or 'https' but was 'httpss'", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void parseNoScheme(WebUrlFactoryMethod method) {
    assertInvalid("//host", "Expected URL scheme 'http' or 'https' but no colon was found", method);
    assertInvalid("/path", "Expected URL scheme 'http' or 'https' but no colon was found", method);
    assertInvalid("path", "Expected URL scheme 'http' or 'https' but no colon was found", method);
    assertInvalid("?query", "Expected URL scheme 'http' or 'https' but no colon was found", method);
    assertInvalid("#fragment", "Expected URL scheme 'http' or 'https' but no colon was found", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void newBuilderResolve(WebUrlFactoryMethod method) {
    // Non-exhaustive tests because implementation is the same as resolve.
    WebUrl base = create("http://host/a/b", method);
    assertThat(base.newBuilder("https://host2").build()).isEqualTo(create("https://host2/", method));
    assertThat(base.newBuilder("//host2").build()).isEqualTo(create("http://host2/", method));
    assertThat(base.newBuilder("/path").build()).isEqualTo(create("http://host/path", method));
    assertThat(base.newBuilder("path").build()).isEqualTo(create("http://host/a/path", method));
    assertThat(base.newBuilder("?query").build()).isEqualTo(create("http://host/a/b?query", method));
    assertThat(base.newBuilder("#fragment").build()).isEqualTo(create("http://host/a/b#fragment", method));
    assertThat(base.newBuilder("").build()).isEqualTo(create("http://host/a/b", method));
    assertThat(base.newBuilder("ftp://b")).isNull();
    assertThat(base.newBuilder("ht+tp://b")).isNull();
    assertThat(base.newBuilder("ht-tp://b")).isNull();
    assertThat(base.newBuilder("ht.tp://b")).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void redactedUrl(WebUrlFactoryMethod method) {
    WebUrl baseWithPasswordAndUsername = create("http://username:password@host/a/b#fragment", method);
    WebUrl baseWithUsernameOnly = create("http://username@host/a/b#fragment", method);
    WebUrl baseWithPasswordOnly = create("http://password@host/a/b#fragment", method);
    assertThat(baseWithPasswordAndUsername.redact()).isEqualTo("http://host/...");
    assertThat(baseWithUsernameOnly.redact()).isEqualTo("http://host/...");
    assertThat(baseWithPasswordOnly.redact()).isEqualTo("http://host/...");
  }

  @ParameterizedTestWithWebUrlFactory
  public void resolveNoScheme(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b", method);
    assertThat(base.resolve("//host2")).isEqualTo(create("http://host2/", method));
    assertThat(base.resolve("/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("path")).isEqualTo(create("http://host/a/path", method));
    assertThat(base.resolve("?query")).isEqualTo(create("http://host/a/b?query", method));
    assertThat(base.resolve("#fragment")).isEqualTo(create("http://host/a/b#fragment", method));
    assertThat(base.resolve("")).isEqualTo(create("http://host/a/b", method));
    assertThat(base.resolve("\\path")).isEqualTo(create("http://host/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void resolveUnsupportedScheme(WebUrlFactoryMethod method) {
    WebUrl base = create("http://a/", method);
    assertThat(base.resolve("ftp://b")).isNull();
    assertThat(base.resolve("ht+tp://b")).isNull();
    assertThat(base.resolve("ht-tp://b")).isNull();
    assertThat(base.resolve("ht.tp://b")).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void resolveSchemeLikePath(WebUrlFactoryMethod method) {
    WebUrl base = create("http://a/", method);
    assertThat(base.resolve("http//b/")).isEqualTo(create("http://a/http//b/", method));
    assertThat(base.resolve("ht+tp//b/")).isEqualTo(create("http://a/ht+tp//b/", method));
    assertThat(base.resolve("ht-tp//b/")).isEqualTo(create("http://a/ht-tp//b/", method));
    assertThat(base.resolve("ht.tp//b/")).isEqualTo(create("http://a/ht.tp//b/", method));
  }

  /** https://tools.ietf.org/html/rfc3986#section-5.4.1 */
  @ParameterizedTestWithWebUrlFactory
  public void rfc3886NormalExamples(WebUrlFactoryMethod method) {
    WebUrl url = create("http://a/b/c/d;p?q", method);
    // No 'g:' scheme in WebUrl.
    assertThat(url.resolve("g:h")).isNull();
    assertThat(url.resolve("g")).isEqualTo(create("http://a/b/c/g", method));
    assertThat(url.resolve("./g")).isEqualTo(create("http://a/b/c/g", method));
    assertThat(url.resolve("g/")).isEqualTo(create("http://a/b/c/g/", method));
    assertThat(url.resolve("/g")).isEqualTo(create("http://a/g", method));
    assertThat(url.resolve("//g")).isEqualTo(create("http://g", method));
    assertThat(url.resolve("?y")).isEqualTo(create("http://a/b/c/d;p?y", method));
    assertThat(url.resolve("g?y")).isEqualTo(create("http://a/b/c/g?y", method));
    assertThat(url.resolve("#s")).isEqualTo(create("http://a/b/c/d;p?q#s", method));
    assertThat(url.resolve("g#s")).isEqualTo(create("http://a/b/c/g#s", method));
    assertThat(url.resolve("g?y#s")).isEqualTo(create("http://a/b/c/g?y#s", method));
    assertThat(url.resolve(";x")).isEqualTo(create("http://a/b/c/;x", method));
    assertThat(url.resolve("g;x")).isEqualTo(create("http://a/b/c/g;x", method));
    assertThat(url.resolve("g;x?y#s")).isEqualTo(create("http://a/b/c/g;x?y#s", method));
    assertThat(url.resolve("")).isEqualTo(create("http://a/b/c/d;p?q", method));
    assertThat(url.resolve(".")).isEqualTo(create("http://a/b/c/", method));
    assertThat(url.resolve("./")).isEqualTo(create("http://a/b/c/", method));
    assertThat(url.resolve("..")).isEqualTo(create("http://a/b/", method));
    assertThat(url.resolve("../")).isEqualTo(create("http://a/b/", method));
    assertThat(url.resolve("../g")).isEqualTo(create("http://a/b/g", method));
    assertThat(url.resolve("../..")).isEqualTo(create("http://a/", method));
    assertThat(url.resolve("../../")).isEqualTo(create("http://a/", method));
    assertThat(url.resolve("../../g")).isEqualTo(create("http://a/g", method));
  }

  /** https://tools.ietf.org/html/rfc3986#section-5.4.2 */
  @ParameterizedTestWithWebUrlFactory
  public void rfc3886AbnormalExamples(WebUrlFactoryMethod method) {
    WebUrl url = create("http://a/b/c/d;p?q", method);
    assertThat(url.resolve("../../../g")).isEqualTo(create("http://a/g", method));
    assertThat(url.resolve("../../../../g")).isEqualTo(create("http://a/g", method));
    assertThat(url.resolve("/./g")).isEqualTo(create("http://a/g", method));
    assertThat(url.resolve("/../g")).isEqualTo(create("http://a/g", method));
    assertThat(url.resolve("g.")).isEqualTo(create("http://a/b/c/g.", method));
    assertThat(url.resolve(".g")).isEqualTo(create("http://a/b/c/.g", method));
    assertThat(url.resolve("g..")).isEqualTo(create("http://a/b/c/g..", method));
    assertThat(url.resolve("..g")).isEqualTo(create("http://a/b/c/..g", method));
    assertThat(url.resolve("./../g")).isEqualTo(create("http://a/b/g", method));
    assertThat(url.resolve("./g/.")).isEqualTo(create("http://a/b/c/g/", method));
    assertThat(url.resolve("g/./h")).isEqualTo(create("http://a/b/c/g/h", method));
    assertThat(url.resolve("g/../h")).isEqualTo(create("http://a/b/c/h", method));
    assertThat(url.resolve("g;x=1/./y")).isEqualTo(create("http://a/b/c/g;x=1/y", method));
    assertThat(url.resolve("g;x=1/../y")).isEqualTo(create("http://a/b/c/y", method));
    assertThat(url.resolve("g?y/./x")).isEqualTo(create("http://a/b/c/g?y/./x", method));
    assertThat(url.resolve("g?y/../x")).isEqualTo(create("http://a/b/c/g?y/../x", method));
    assertThat(url.resolve("g#s/./x")).isEqualTo(create("http://a/b/c/g#s/./x", method));
    assertThat(url.resolve("g#s/../x")).isEqualTo(create("http://a/b/c/g#s/../x", method));
    // "http:g" also okay.
    assertThat(url.resolve("http:g")).isEqualTo(create("http://a/b/c/g", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void parseAuthoritySlashCountDoesntMatter(WebUrlFactoryMethod method) {
    assertThat(create("http:host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:/host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http://host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\/host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:/\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:///host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\//host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:/\\/host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http://\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\\\/host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:/\\\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:\\\\\\host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http:////host/path", method)).isEqualTo(create("http://host/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void resolveAuthoritySlashCountDoesntMatterWithDifferentScheme(WebUrlFactoryMethod method) {
    WebUrl base = create("https://a/b/c", method);
    assertThat(base.resolve("http:host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http://host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:///host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\//host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http://\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:////host/path")).isEqualTo(create("http://host/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void resolveAuthoritySlashCountMattersWithSameScheme(WebUrlFactoryMethod method) {
    WebUrl base = create("http://a/b/c", method);
    assertThat(base.resolve("http:host/path")).isEqualTo(create("http://a/b/host/path", method));
    assertThat(base.resolve("http:/host/path")).isEqualTo(create("http://a/host/path", method));
    assertThat(base.resolve("http:\\host/path")).isEqualTo(create("http://a/host/path", method));
    assertThat(base.resolve("http://host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:///host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\//host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http://\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\/host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:/\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:\\\\\\host/path")).isEqualTo(create("http://host/path", method));
    assertThat(base.resolve("http:////host/path")).isEqualTo(create("http://host/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void username(WebUrlFactoryMethod method) {
    assertThat(create("http://@host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http://user@host/path", method)).isEqualTo(create("http://user@host/path", method));
  }

  /** Given multiple '@' characters, the last one is the delimiter. */
  @ParameterizedTestWithWebUrlFactory
  public void authorityWithMultipleAtSigns(WebUrlFactoryMethod method) {
    WebUrl httpUrl = create("http://foo@bar@baz/path", method);
    assertThat(httpUrl.username()).isEqualTo("foo@bar");
    assertThat(httpUrl.password()).isEqualTo("");
    assertThat(httpUrl).isEqualTo(create("http://foo%40bar@baz/path", method));
  }

  /** Given multiple ':' characters, the first one is the delimiter. */
  @ParameterizedTestWithWebUrlFactory
  public void authorityWithMultipleColons(WebUrlFactoryMethod method) {
    WebUrl httpUrl = create("http://foo:pass1@bar:pass2@baz/path", method);
    assertThat(httpUrl.username()).isEqualTo("foo");
    assertThat(httpUrl.password()).isEqualTo("pass1@bar:pass2");
    assertThat(httpUrl).isEqualTo(create("http://foo:pass1%40bar%3Apass2@baz/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void usernameAndPassword(WebUrlFactoryMethod method) {
    assertThat(create("http://username:password@host/path", method)).isEqualTo(
        create("http://username:password@host/path", method));
    assertThat(create("http://username:@host/path", method)).isEqualTo(
        create("http://username@host/path", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void passwordWithEmptyUsername(WebUrlFactoryMethod method) {
    // Chrome doesn't mind, but Firefox rejects URLs with empty usernames and non-empty passwords.
    assertThat(create("http://:@host/path", method)).isEqualTo(create("http://host/path", method));
    assertThat(create("http://:password@@host/path", method).encodedPassword()).isEqualTo(
        "password%40");
  }

  @ParameterizedTestWithWebUrlFactory
  public void unprintableCharactersArePercentEncoded(WebUrlFactoryMethod method) {
    assertThat(create("http://host/\u0000", method).encodedPath()).isEqualTo("/%00");
    assertThat(create("http://host/\u0008", method).encodedPath()).isEqualTo("/%08");
    assertThat(create("http://host/\ufffd", method).encodedPath()).isEqualTo("/%EF%BF%BD");
  }

  @Test public void usernameCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.PERCENT, '[', ']', '{', '}', '|', '^', '\'', ';', '=', '@')
        .override(Encoding.SKIP, ':', '/', '\\', '?', '#')
        .skipForUri('%')
        .test(Component.USER);
  }

  @Test public void passwordCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.PERCENT, '[', ']', '{', '}', '|', '^', '\'', ':', ';', '=', '@')
        .override(Encoding.SKIP, '/', '\\', '?', '#')
        .skipForUri('%')
        .test(Component.PASSWORD);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostContainsIllegalCharacter(WebUrlFactoryMethod method) {
    assertInvalid("http://\n/", "Invalid URL host: \"\n\"", method);
    assertInvalid("http:// /", "Invalid URL host: \" \"", method);
    assertInvalid("http://%20/", "Invalid URL host: \"%20\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameLowercaseCharactersMappedDirectly(WebUrlFactoryMethod method) {
    assertThat(create("http://abcd", method).host()).isEqualTo("abcd");
    assertThat(create("http://σ", method).host()).isEqualTo("xn--4xa");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameUppercaseCharactersConvertedToLowercase(WebUrlFactoryMethod method) {
    assertThat(create("http://ABCD", method).host()).isEqualTo("abcd");
    assertThat(create("http://Σ", method).host()).isEqualTo("xn--4xa");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameIgnoredCharacters(WebUrlFactoryMethod method) {
    // The soft hyphen (­) should be ignored.
    assertThat(create("http://AB\u00adCD", method).host()).isEqualTo("abcd");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameMultipleCharacterMapping(WebUrlFactoryMethod method) {
    // Map the single character telephone symbol (℡) to the string "tel".
    assertThat(create("http://\u2121", method).host()).isEqualTo("tel");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameMappingLastMappedCodePoint(WebUrlFactoryMethod method) {
    assertThat(create("http://\uD87E\uDE1D", method).host()).isEqualTo("xn--pu5l");
  }

  @Disabled("The java.net.IDN implementation doesn't ignore characters that it should.")
  @ParameterizedTestWithWebUrlFactory
  public void hostnameMappingLastIgnoredCodePoint(WebUrlFactoryMethod method) {
    assertThat(create("http://ab\uDB40\uDDEFcd", method).host()).isEqualTo("abcd");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostnameMappingLastDisallowedCodePoint(WebUrlFactoryMethod method) {
    assertInvalid("http://\uDBFF\uDFFF", "Invalid URL host: \"\uDBFF\uDFFF\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6(WebUrlFactoryMethod method) {
    // Square braces are absent from host()...
    assertThat(create("http://[::1]/", method).host()).isEqualTo("::1");

    // ... but they're included in toString().
    assertThat(create("http://[::1]/", method).toString()).isEqualTo("http://[::1]/");

    // IPv6 colons don't interfere with port numbers or passwords.
    assertThat(create("http://[::1]:8080/", method).port()).isEqualTo(8080);
    assertThat(create("http://user:password@[::1]/", method).password()).isEqualTo("password");
    assertThat(create("http://user:password@[::1]:8080/", method).host()).isEqualTo("::1");

    // Permit the contents of IPv6 addresses to be percent-encoded...
    assertThat(create("http://[%3A%3A%31]/", method).host()).isEqualTo("::1");

    // Including the Square braces themselves! (This is what Chrome does.)
    assertThat(create("http://%5B%3A%3A1%5D/", method).host()).isEqualTo("::1");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressDifferentFormats(WebUrlFactoryMethod method) {
    // Multiple representations of the same address; see http://tools.ietf.org/html/rfc5952.
    String a3 = "2001:db8::1:0:0:1";
    assertThat(create("http://[2001:db8:0:0:1:0:0:1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:0db8:0:0:1:0:0:1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:db8::1:0:0:1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:db8::0:1:0:0:1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:0db8::1:0:0:1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:db8:0:0:1::1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:db8:0000:0:1::1]", method).host()).isEqualTo(a3);
    assertThat(create("http://[2001:DB8:0:0:1::1]", method).host()).isEqualTo(a3);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressLeadingCompression(WebUrlFactoryMethod method) {
    assertThat(create("http://[::0001]", method).host()).isEqualTo("::1");
    assertThat(create("http://[0000::0001]", method).host()).isEqualTo("::1");
    assertThat(create("http://[0000:0000:0000:0000:0000:0000:0000:0001]", method).host()).isEqualTo("::1");
    assertThat(create("http://[0000:0000:0000:0000:0000:0000::0001]", method).host()).isEqualTo("::1");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressTrailingCompression(WebUrlFactoryMethod method) {
    assertThat(create("http://[0001:0000::]", method).host()).isEqualTo("1::");
    assertThat(create("http://[0001::0000]", method).host()).isEqualTo("1::");
    assertThat(create("http://[0001::]", method).host()).isEqualTo("1::");
    assertThat(create("http://[1::]", method).host()).isEqualTo("1::");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressTooManyDigitsInGroup(WebUrlFactoryMethod method) {
    assertInvalid("http://[00000:0000:0000:0000:0000:0000:0000:0001]",
        "Invalid URL host: \"[00000:0000:0000:0000:0000:0000:0000:0001]\"", method);
    assertInvalid("http://[::00001]", "Invalid URL host: \"[::00001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressMisplacedColons(WebUrlFactoryMethod method) {
    assertInvalid("http://[:0000:0000:0000:0000:0000:0000:0000:0001]",
        "Invalid URL host: \"[:0000:0000:0000:0000:0000:0000:0000:0001]\"", method);
    assertInvalid("http://[:::0000:0000:0000:0000:0000:0000:0000:0001]",
        "Invalid URL host: \"[:::0000:0000:0000:0000:0000:0000:0000:0001]\"", method);
    assertInvalid("http://[:1]", "Invalid URL host: \"[:1]\"", method);
    assertInvalid("http://[:::1]", "Invalid URL host: \"[:::1]\"", method);
    assertInvalid("http://[0000:0000:0000:0000:0000:0000:0001:]",
        "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0001:]\"", method);
    assertInvalid("http://[0000:0000:0000:0000:0000:0000:0000:0001:]",
        "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001:]\"", method);
    assertInvalid("http://[0000:0000:0000:0000:0000:0000:0000:0001::]",
        "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001::]\"", method);
    assertInvalid("http://[0000:0000:0000:0000:0000:0000:0000:0001:::]",
        "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0001:::]\"", method);
    assertInvalid("http://[1:]", "Invalid URL host: \"[1:]\"", method);
    assertInvalid("http://[1:::]", "Invalid URL host: \"[1:::]\"", method);
    assertInvalid("http://[1:::1]", "Invalid URL host: \"[1:::1]\"", method);
    assertInvalid("http://[0000:0000:0000:0000::0000:0000:0000:0001]",
        "Invalid URL host: \"[0000:0000:0000:0000::0000:0000:0000:0001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressTooManyGroups(WebUrlFactoryMethod method) {
    assertInvalid("http://[0000:0000:0000:0000:0000:0000:0000:0000:0001]",
        "Invalid URL host: \"[0000:0000:0000:0000:0000:0000:0000:0000:0001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressTooMuchCompression(WebUrlFactoryMethod method) {
    assertInvalid("http://[0000::0000:0000:0000:0000::0001]",
        "Invalid URL host: \"[0000::0000:0000:0000:0000::0001]\"", method);
    assertInvalid("http://[::0000:0000:0000:0000::0001]",
        "Invalid URL host: \"[::0000:0000:0000:0000::0001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6ScopedAddress(WebUrlFactoryMethod method) {
    // java.net.InetAddress parses scoped addresses. These aren't valid in URLs.
    assertInvalid("http://[::1%2544]", "Invalid URL host: \"[::1%2544]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6AddressTooManyLeadingZeros(WebUrlFactoryMethod method) {
    // Guava's been buggy on this case. https://github.com/google/guava/issues/3116
    assertInvalid("http://[2001:db8:0:0:1:0:0:00001]",
        "Invalid URL host: \"[2001:db8:0:0:1:0:0:00001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6WithIpv4Suffix(WebUrlFactoryMethod method) {
    assertThat(create("http://[::1:255.255.255.255]/", method).host()).isEqualTo("::1:ffff:ffff");
    assertThat(create("http://[0:0:0:0:0:1:0.0.0.0]/", method).host()).isEqualTo("::1:0:0");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6WithIpv4SuffixWithOctalPrefix(WebUrlFactoryMethod method) {
    // Chrome interprets a leading '0' as octal; Firefox rejects them. (We reject them.)
    assertInvalid("http://[0:0:0:0:0:1:0.0.0.000000]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.0.0.000000]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:0.010.0.010]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.010.0.010]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:0.0.0.000001]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.0.0.000001]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6WithIpv4SuffixWithHexadecimalPrefix(WebUrlFactoryMethod method) {
    // Chrome interprets a leading '0x' as hexadecimal; Firefox rejects them. (We reject them.)
    assertInvalid("http://[0:0:0:0:0:1:0.0x10.0.0x10]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.0x10.0.0x10]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6WithMalformedIpv4Suffix(WebUrlFactoryMethod method) {
    assertInvalid("http://[0:0:0:0:0:1:0.0:0.0]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.0:0.0]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:0.0-0.0]/",
        "Invalid URL host: \"[0:0:0:0:0:1:0.0-0.0]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:.255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:.255.255.255]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:255..255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:255..255.255]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:255.255..255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:255.255..255]\"", method);
    assertInvalid("http://[0:0:0:0:0:0:1:255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:0:1:255.255]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:256.255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:256.255.255.255]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:ff.255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:ff.255.255.255]\"", method);
    assertInvalid("http://[0:0:0:0:0:0:1:255.255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:0:1:255.255.255.255]\"", method);
    assertInvalid("http://[0:0:0:0:1:255.255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:1:255.255.255.255]\"", method);
    assertInvalid("http://[0:0:0:0:1:0.0.0.0:1]/",
        "Invalid URL host: \"[0:0:0:0:1:0.0.0.0:1]\"", method);
    assertInvalid("http://[0:0.0.0.0:1:0:0:0:0:1]/",
        "Invalid URL host: \"[0:0.0.0.0:1:0:0:0:0:1]\"", method);
    assertInvalid("http://[0.0.0.0:0:0:0:0:0:1]/",
        "Invalid URL host: \"[0.0.0.0:0:0:0:0:0:1]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6WithIncompleteIpv4Suffix(WebUrlFactoryMethod method) {
    // To Chrome & Safari these are well-formed; Firefox disagrees. (We're consistent with Firefox).
    assertInvalid("http://[0:0:0:0:0:1:255.255.255.]/",
        "Invalid URL host: \"[0:0:0:0:0:1:255.255.255.]\"", method);
    assertInvalid("http://[0:0:0:0:0:1:255.255.255]/",
        "Invalid URL host: \"[0:0:0:0:0:1:255.255.255]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6Malformed(WebUrlFactoryMethod method) {
    assertInvalid("http://[::g]/", "Invalid URL host: \"[::g]\"", method);
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6CanonicalForm(WebUrlFactoryMethod method) {
    assertThat(create("http://[abcd:ef01:2345:6789:abcd:ef01:2345:6789]/", method).host()).isEqualTo(
        "abcd:ef01:2345:6789:abcd:ef01:2345:6789");
    assertThat(create("http://[a:0:0:0:b:0:0:0]/", method).host()).isEqualTo("a::b:0:0:0");
    assertThat(create("http://[a:b:0:0:c:0:0:0]/", method).host()).isEqualTo("a:b:0:0:c::");
    assertThat(create("http://[a:b:0:0:0:c:0:0]/", method).host()).isEqualTo("a:b::c:0:0");
    assertThat(create("http://[a:0:0:0:b:0:0:0]/", method).host()).isEqualTo("a::b:0:0:0");
    assertThat(create("http://[0:0:0:a:b:0:0:0]/", method).host()).isEqualTo("::a:b:0:0:0");
    assertThat(create("http://[0:0:0:a:0:0:0:b]/", method).host()).isEqualTo("::a:0:0:0:b");
    assertThat(create("http://[0:a:b:c:d:e:f:1]/", method).host()).isEqualTo("0:a:b:c:d:e:f:1");
    assertThat(create("http://[a:b:c:d:e:f:1:0]/", method).host()).isEqualTo("a:b:c:d:e:f:1:0");
    assertThat(create("http://[FF01:0:0:0:0:0:0:101]/", method).host()).isEqualTo("ff01::101");
    assertThat(create("http://[2001:db8::1]/", method).host()).isEqualTo("2001:db8::1");
    assertThat(create("http://[2001:db8:0:0:0:0:2:1]/", method).host()).isEqualTo("2001:db8::2:1");
    assertThat(create("http://[2001:db8:0:1:1:1:1:1]/", method).host()).isEqualTo(
        "2001:db8:0:1:1:1:1:1");
    assertThat(create("http://[2001:db8:0:0:1:0:0:1]/", method).host()).isEqualTo(
        "2001:db8::1:0:0:1");
    assertThat(create("http://[2001:0:0:1:0:0:0:1]/", method).host()).isEqualTo("2001:0:0:1::1");
    assertThat(create("http://[1:0:0:0:0:0:0:0]/", method).host()).isEqualTo("1::");
    assertThat(create("http://[0:0:0:0:0:0:0:1]/", method).host()).isEqualTo("::1");
    assertThat(create("http://[0:0:0:0:0:0:0:0]/", method).host()).isEqualTo("::");
    assertThat(create("http://[::ffff:c0a8:1fe]/", method).host()).isEqualTo("192.168.1.254");
  }

  /** The builder permits square braces but does not require them. */
  @ParameterizedTestWithWebUrlFactory
  public void hostIpv6Builder(WebUrlFactoryMethod method) {
    WebUrl base = create("http://example.com/", method);
    assertThat(base.newBuilder().host("[::1]").build().toString()).isEqualTo(
        "http://[::1]/");
    assertThat(base.newBuilder().host("[::0001]").build().toString()).isEqualTo(
        "http://[::1]/");
    assertThat(base.newBuilder().host("::1").build().toString()).isEqualTo("http://[::1]/");
    assertThat(base.newBuilder().host("::0001").build().toString()).isEqualTo(
        "http://[::1]/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostIpv4CanonicalForm(WebUrlFactoryMethod method) {
    assertThat(create("http://255.255.255.255/", method).host()).isEqualTo("255.255.255.255");
    assertThat(create("http://1.2.3.4/", method).host()).isEqualTo("1.2.3.4");
    assertThat(create("http://0.0.0.0/", method).host()).isEqualTo("0.0.0.0");
  }

  @ParameterizedTestWithWebUrlFactory
  public void hostWithTrailingDot(WebUrlFactoryMethod method) {
    assertThat(create("http://host./", method).host()).isEqualTo("host.");
  }

  @ParameterizedTestWithWebUrlFactory
  public void port(WebUrlFactoryMethod method) {
    assertThat(create("http://host:80/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("http://host:99/", method)).isEqualTo(create("http://host:99/", method));
    assertThat(create("http://host:/", method)).isEqualTo(create("http://host/", method));
    assertThat(create("http://host:65535/", method).port()).isEqualTo(65535);
    assertInvalid("http://host:0/", "Invalid URL port: \"0\"", method);
    assertInvalid("http://host:65536/", "Invalid URL port: \"65536\"", method);
    assertInvalid("http://host:-1/", "Invalid URL port: \"-1\"", method);
    assertInvalid("http://host:a/", "Invalid URL port: \"a\"", method);
    assertInvalid("http://host:%39%39/", "Invalid URL port: \"%39%39\"", method);
  }

  @Test public void pathCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.PERCENT, '^', '{', '}', '|')
        .override(Encoding.SKIP, '\\', '?', '#')
        .skipForUri('%', '[', ']')
        .test(Component.PATH);
  }

  @Test public void queryCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.IDENTITY, '?', '`')
        .override(Encoding.PERCENT, '\'')
        .override(Encoding.SKIP, '#', '+')
        .skipForUri('%', '\\', '^', '`', '{', '|', '}')
        .test(Component.QUERY);
  }

  @Test public void queryValueCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.IDENTITY, '?', '`')
        .override(Encoding.PERCENT, '\'')
        .override(Encoding.SKIP, '#', '+')
        .skipForUri('%', '\\', '^', '`', '{', '|', '}')
        .test(Component.QUERY_VALUE);
  }

  @Test public void fragmentCharacters() {
    new UrlComponentEncodingTester()
        .override(Encoding.IDENTITY, ' ', '"', '#', '<', '>', '?', '`')
        .skipForUri('%', ' ', '"', '#', '<', '>', '\\', '^', '`', '{', '|', '}')
        .identityForNonAscii()
        .test(Component.FRAGMENT);
  }

  @ParameterizedTestWithWebUrlFactory
  public void fragmentNonAscii(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#Σ", method);
    assertThat(url.toString()).isEqualTo("http://host/#Σ");
    assertThat(url.fragment()).isEqualTo("Σ");
    assertThat(url.encodedFragment()).isEqualTo("Σ");
    assertThat(url.uri().toString()).isEqualTo("http://host/#Σ");
  }

  @ParameterizedTestWithWebUrlFactory
  public void fragmentNonAsciiThatOffendsJavaNetUri(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#\u0080", method);
    assertThat(url.toString()).isEqualTo("http://host/#\u0080");
    assertThat(url.fragment()).isEqualTo("\u0080");
    assertThat(url.encodedFragment()).isEqualTo("\u0080");
    // Control characters may be stripped!
    assertThat(url.uri()).isEqualTo(URI.create("http://host/#"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void fragmentPercentEncodedNonAscii(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#%C2%80", method);
    assertThat(url.toString()).isEqualTo("http://host/#%C2%80");
    assertThat(url.fragment()).isEqualTo("\u0080");
    assertThat(url.encodedFragment()).isEqualTo("%C2%80");
    assertThat(url.uri().toString()).isEqualTo("http://host/#%C2%80");
  }

  @ParameterizedTestWithWebUrlFactory
  public void fragmentPercentEncodedPartialCodePoint(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#%80", method);
    assertThat(url.toString()).isEqualTo("http://host/#%80");
    // Unicode replacement character.
    assertThat(url.fragment()).isEqualTo("\ufffd");
    assertThat(url.encodedFragment()).isEqualTo("%80");
    assertThat(url.uri().toString()).isEqualTo("http://host/#%80");
  }

  @ParameterizedTestWithWebUrlFactory
  public void relativePath(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.resolve("d/e/f")).isEqualTo(create("http://host/a/b/d/e/f", method));
    assertThat(base.resolve("../../d/e/f")).isEqualTo(create("http://host/d/e/f", method));
    assertThat(base.resolve("..")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("../..")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../..")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve(".")).isEqualTo(create("http://host/a/b/", method));
    assertThat(base.resolve("././..")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("c/d/../e/../")).isEqualTo(create("http://host/a/b/c/", method));
    assertThat(base.resolve("..e/")).isEqualTo(create("http://host/a/b/..e/", method));
    assertThat(base.resolve("e/f../")).isEqualTo(create("http://host/a/b/e/f../", method));
    assertThat(base.resolve("%2E.")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve(".%2E")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("%2E%2E")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("%2e.")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve(".%2e")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("%2e%2e")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("%2E")).isEqualTo(create("http://host/a/b/", method));
    assertThat(base.resolve("%2e")).isEqualTo(create("http://host/a/b/", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void relativePathWithTrailingSlash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c/", method);
    assertThat(base.resolve("..")).isEqualTo(create("http://host/a/b/", method));
    assertThat(base.resolve("../")).isEqualTo(create("http://host/a/b/", method));
    assertThat(base.resolve("../..")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("../../")).isEqualTo(create("http://host/a/", method));
    assertThat(base.resolve("../../..")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../../")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../../..")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../../../")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../../../a")).isEqualTo(create("http://host/a", method));
    assertThat(base.resolve("../../../../a/..")).isEqualTo(create("http://host/", method));
    assertThat(base.resolve("../../../../a/b/..")).isEqualTo(create("http://host/a/", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void pathWithBackslash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.resolve("d\\e\\f")).isEqualTo(create("http://host/a/b/d/e/f", method));
    assertThat(base.resolve("../..\\d\\e\\f")).isEqualTo(create("http://host/d/e/f", method));
    assertThat(base.resolve("..\\..")).isEqualTo(create("http://host/", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void relativePathWithSameScheme(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.resolve("http:d/e/f")).isEqualTo(create("http://host/a/b/d/e/f", method));
    assertThat(base.resolve("http:../../d/e/f")).isEqualTo(create("http://host/d/e/f", method));
  }

  @ParameterizedTestWithWebUrlFactory
  public void decodeUsername(WebUrlFactoryMethod method) {
    assertThat(create("http://user@host/", method).username()).isEqualTo("user");
    assertThat(create("http://%F0%9F%8D%A9@host/", method).username()).isEqualTo("\uD83C\uDF69");
  }

  @ParameterizedTestWithWebUrlFactory
  public void decodePassword(WebUrlFactoryMethod method) {
    assertThat(create("http://user:password@host/", method).password()).isEqualTo("password");
    assertThat(create("http://user:@host/", method).password()).isEqualTo("");
    assertThat(create("http://user:%F0%9F%8D%A9@host/", method).password()).isEqualTo(
        "\uD83C\uDF69");
  }

  @ParameterizedTestWithWebUrlFactory
  public void decodeSlashCharacterInDecodedPathSegment(WebUrlFactoryMethod method) {
    assertThat(create("http://host/a%2Fb%2Fc", method).pathSegments()).isEqualTo(List.of("a/b/c"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void decodeEmptyPathSegments(WebUrlFactoryMethod method) {
    assertThat(create("http://host/", method).pathSegments()).isEqualTo(List.of(""));
  }

  @ParameterizedTestWithWebUrlFactory
  public void percentDecode(WebUrlFactoryMethod method) {
    assertThat(create("http://host/%00", method).pathSegments()).isEqualTo(List.of("\u0000"));
    assertThat(create("http://host/a/%E2%98%83/c", method).pathSegments()).isEqualTo(
        List.of("a", "\u2603", "c"));
    assertThat(create("http://host/a/%F0%9F%8D%A9/c", method).pathSegments()).isEqualTo(
        List.of("a", "\uD83C\uDF69", "c"));
    assertThat(create("http://host/a/%62/c", method).pathSegments()).isEqualTo(
        List.of("a", "b", "c"));
    assertThat(create("http://host/a/%7A/c", method).pathSegments()).isEqualTo(
        List.of("a", "z", "c"));
    assertThat(create("http://host/a/%7a/c", method).pathSegments()).isEqualTo(
        List.of("a", "z", "c"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void malformedPercentEncoding(WebUrlFactoryMethod method) {
    assertThat(create("http://host/a%f/b", method).pathSegments()).isEqualTo(List.of("a%f", "b"));
    assertThat(create("http://host/%/b", method).pathSegments()).isEqualTo(List.of("%", "b"));
    assertThat(create("http://host/%", method).pathSegments()).isEqualTo(List.of("%"));
    assertThat(create("http://github.com/%%30%30", method).pathSegments()).isEqualTo(List.of("%00"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void malformedUtf8Encoding(WebUrlFactoryMethod method) {
    // Replace a partial UTF-8 sequence with the Unicode replacement character.
    assertThat(create("http://host/a/%E2%98x/c", method).pathSegments()).isEqualTo(List.of("a", "\ufffdx", "c"));
  }

  @Test public void incompleteUrlComposition() {
    try {
      WebUrl.builder().scheme("http").build();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).isEqualTo("host == null");
    }
    try {
      WebUrl.builder().host("host").build();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).isEqualTo("scheme == null");
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void builderToString(WebUrlFactoryMethod method) {
    assertThat(create("https://host.com/path", method).newBuilder().toString()).isEqualTo(
        "https://host.com/path");
  }

  @Test public void incompleteBuilderToString() {
    assertThat(WebUrl.builder().scheme("https").encodedPath("/path").toString()).isEqualTo(
        "https:///path");
    assertThat(WebUrl.builder().host("host.com").encodedPath("/path").toString()).isEqualTo(
        "//host.com/path");
    assertThat(
        (Object) WebUrl.builder().host("host.com").encodedPath("/path").port(8080).toString()).isEqualTo(
        "//host.com:8080/path");
  }

  @Test public void minimalUrlComposition() {
    WebUrl url = WebUrl.builder().scheme("http").host("host").build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.scheme()).isEqualTo("http");
    assertThat(url.username()).isEqualTo("");
    assertThat(url.password()).isEqualTo("");
    assertThat(url.host()).isEqualTo("host");
    assertThat(url.port()).isEqualTo(80);
    assertThat(url.encodedPath()).isEqualTo("/");
    assertThat(url.query()).isNull();
    assertThat(url.fragment()).isNull();
  }

  @Test public void fullUrlComposition() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .username("username")
        .password("password")
        .host("host")
        .port(8080)
        .addPathSegment("path")
        .query("query")
        .fragment("fragment")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://username:password@host:8080/path?query#fragment");
    assertThat(url.scheme()).isEqualTo("http");
    assertThat(url.username()).isEqualTo("username");
    assertThat(url.password()).isEqualTo("password");
    assertThat(url.host()).isEqualTo("host");
    assertThat(url.port()).isEqualTo(8080);
    assertThat(url.encodedPath()).isEqualTo("/path");
    assertThat(url.query()).isEqualTo("query");
    assertThat(url.fragment()).isEqualTo("fragment");
  }

  @ParameterizedTestWithWebUrlFactory
  public void changingSchemeChangesDefaultPort(WebUrlFactoryMethod method) {
    assertThat(create("http://example.com", method)
        .newBuilder()
        .scheme("https")
        .build().port()).isEqualTo(443);

    assertThat(create("https://example.com", method)
        .newBuilder()
        .scheme("http")
        .build().port()).isEqualTo(80);

    assertThat(create("https://example.com:1234", method)
        .newBuilder()
        .scheme("http")
        .build().port()).isEqualTo(1234);
  }

  @Test public void composeEncodesWhitespace() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .username("a\r\n\f\t b")
        .password("c\r\n\f\t d")
        .host("host")
        .addPathSegment("e\r\n\f\t f")
        .query("g\r\n\f\t h")
        .fragment("i\r\n\f\t j")
        .build();
    assertThat(url.toString()).isEqualTo(("http://a%0D%0A%0C%09%20b:c%0D%0A%0C%09%20d@host"
        + "/e%0D%0A%0C%09%20f?g%0D%0A%0C%09%20h#i%0D%0A%0C%09 j"));
    assertThat(url.username()).isEqualTo("a\r\n\f\t b");
    assertThat(url.password()).isEqualTo("c\r\n\f\t d");
    assertThat(url.pathSegments().get(0)).isEqualTo("e\r\n\f\t f");
    assertThat(url.query()).isEqualTo("g\r\n\f\t h");
    assertThat(url.fragment()).isEqualTo("i\r\n\f\t j");
  }

  @Test public void composeFromUnencodedComponents() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .username("a:\u0001@/\\?#%b")
        .password("c:\u0001@/\\?#%d")
        .host("ef")
        .port(8080)
        .addPathSegment("g:\u0001@/\\?#%h")
        .query("i:\u0001@/\\?#%j")
        .fragment("k:\u0001@/\\?#%l")
        .build();
    assertThat(url.toString()).isEqualTo(
        ("http://a%3A%01%40%2F%5C%3F%23%25b:c%3A%01%40%2F%5C%3F%23%25d@ef:8080/"
        + "g:%01@%2F%5C%3F%23%25h?i:%01@/\\?%23%25j#k:%01@/\\?#%25l"));
    assertThat(url.scheme()).isEqualTo("http");
    assertThat(url.username()).isEqualTo("a:\u0001@/\\?#%b");
    assertThat(url.password()).isEqualTo("c:\u0001@/\\?#%d");
    assertThat(url.pathSegments()).isEqualTo(List.of("g:\u0001@/\\?#%h"));
    assertThat(url.query()).isEqualTo("i:\u0001@/\\?#%j");
    assertThat(url.fragment()).isEqualTo("k:\u0001@/\\?#%l");
    assertThat(url.encodedUsername()).isEqualTo("a%3A%01%40%2F%5C%3F%23%25b");
    assertThat(url.encodedPassword()).isEqualTo("c%3A%01%40%2F%5C%3F%23%25d");
    assertThat(url.encodedPath()).isEqualTo("/g:%01@%2F%5C%3F%23%25h");
    assertThat(url.encodedQuery()).isEqualTo("i:%01@/\\?%23%25j");
    assertThat(url.encodedFragment()).isEqualTo("k:%01@/\\?#%25l");
  }

  @Test public void composeFromEncodedComponents() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .encodedUsername("a:\u0001@/\\?#%25b")
        .encodedPassword("c:\u0001@/\\?#%25d")
        .host("ef")
        .port(8080)
        .addEncodedPathSegment("g:\u0001@/\\?#%25h")
        .encodedQuery("i:\u0001@/\\?#%25j")
        .encodedFragment("k:\u0001@/\\?#%25l")
        .build();
    assertThat(url.toString()).isEqualTo(
        ("http://a%3A%01%40%2F%5C%3F%23%25b:c%3A%01%40%2F%5C%3F%23%25d@ef:8080/"
        + "g:%01@%2F%5C%3F%23%25h?i:%01@/\\?%23%25j#k:%01@/\\?#%25l"));
    assertThat(url.scheme()).isEqualTo("http");
    assertThat(url.username()).isEqualTo("a:\u0001@/\\?#%b");
    assertThat(url.password()).isEqualTo("c:\u0001@/\\?#%d");
    assertThat(url.pathSegments()).isEqualTo(List.of("g:\u0001@/\\?#%h"));
    assertThat(url.query()).isEqualTo("i:\u0001@/\\?#%j");
    assertThat(url.fragment()).isEqualTo("k:\u0001@/\\?#%l");
    assertThat(url.encodedUsername()).isEqualTo("a%3A%01%40%2F%5C%3F%23%25b");
    assertThat(url.encodedPassword()).isEqualTo("c%3A%01%40%2F%5C%3F%23%25d");
    assertThat(url.encodedPath()).isEqualTo("/g:%01@%2F%5C%3F%23%25h");
    assertThat(url.encodedQuery()).isEqualTo("i:%01@/\\?%23%25j");
    assertThat(url.encodedFragment()).isEqualTo("k:%01@/\\?#%25l");
  }

  @Test public void composeWithEncodedPath() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .encodedPath("/a%2Fb/c")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/a%2Fb/c");
    assertThat(url.encodedPath()).isEqualTo("/a%2Fb/c");
    assertThat(url.pathSegments()).isEqualTo(List.of("a/b", "c"));
  }

  @Test public void composeMixingPathSegments() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .encodedPath("/a%2fb/c")
        .addPathSegment("d%25e")
        .addEncodedPathSegment("f%25g")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/a%2fb/c/d%2525e/f%25g");
    assertThat(url.encodedPath()).isEqualTo("/a%2fb/c/d%2525e/f%25g");
    assertThat(url.encodedPathSegments()).isEqualTo(List.of("a%2fb", "c", "d%2525e", "f%25g"));
    assertThat(url.pathSegments()).isEqualTo(List.of("a/b", "c", "d%25e", "f%g"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeWithAddSegment(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegment("").build().encodedPath()).isEqualTo(
        "/a/b/c/");
    assertThat(
        (Object) base.newBuilder().addPathSegment("").addPathSegment("d").build().encodedPath()).isEqualTo(
        "/a/b/c/d");
    assertThat(base.newBuilder().addPathSegment("..").build().encodedPath()).isEqualTo(
        "/a/b/");
    assertThat(base.newBuilder().addPathSegment("").addPathSegment("..").build()
        .encodedPath()).isEqualTo("/a/b/");
    assertThat(base.newBuilder().addPathSegment("").addPathSegment("").build()
        .encodedPath()).isEqualTo("/a/b/c/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void pathSize(WebUrlFactoryMethod method) {
    assertThat(create("http://host/", method).pathSize()).isEqualTo(1);
    assertThat(create("http://host/a/b/c", method).pathSize()).isEqualTo(3);
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegments(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);

    // Add a string with zero slashes: resulting URL gains one slash.
    assertThat(base.newBuilder().addPathSegments("").build().encodedPath()).isEqualTo(
        "/a/b/c/");
    assertThat(base.newBuilder().addPathSegments("d").build().encodedPath()).isEqualTo(
        "/a/b/c/d");

    // Add a string with one slash: resulting URL gains two slashes.
    assertThat(base.newBuilder().addPathSegments("/").build().encodedPath()).isEqualTo(
        "/a/b/c//");
    assertThat(base.newBuilder().addPathSegments("d/").build().encodedPath()).isEqualTo(
        "/a/b/c/d/");
    assertThat(base.newBuilder().addPathSegments("/d").build().encodedPath()).isEqualTo(
        "/a/b/c//d");

    // Add a string with two slashes: resulting URL gains three slashes.
    assertThat(base.newBuilder().addPathSegments("//").build().encodedPath()).isEqualTo(
        "/a/b/c///");
    assertThat(base.newBuilder().addPathSegments("/d/").build().encodedPath()).isEqualTo(
        "/a/b/c//d/");
    assertThat(base.newBuilder().addPathSegments("d//").build().encodedPath()).isEqualTo(
        "/a/b/c/d//");
    assertThat(base.newBuilder().addPathSegments("//d").build().encodedPath()).isEqualTo(
        "/a/b/c///d");
    assertThat(base.newBuilder().addPathSegments("d/e/f").build().encodedPath()).isEqualTo(
        "/a/b/c/d/e/f");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentsOntoTrailingSlash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c/", method);

    // Add a string with zero slashes: resulting URL gains zero slashes.
    assertThat(base.newBuilder().addPathSegments("").build().encodedPath()).isEqualTo(
        "/a/b/c/");
    assertThat(base.newBuilder().addPathSegments("d").build().encodedPath()).isEqualTo(
        "/a/b/c/d");

    // Add a string with one slash: resulting URL gains one slash.
    assertThat(base.newBuilder().addPathSegments("/").build().encodedPath()).isEqualTo(
        "/a/b/c//");
    assertThat(base.newBuilder().addPathSegments("d/").build().encodedPath()).isEqualTo(
        "/a/b/c/d/");
    assertThat(base.newBuilder().addPathSegments("/d").build().encodedPath()).isEqualTo(
        "/a/b/c//d");

    // Add a string with two slashes: resulting URL gains two slashes.
    assertThat(base.newBuilder().addPathSegments("//").build().encodedPath()).isEqualTo(
        "/a/b/c///");
    assertThat(base.newBuilder().addPathSegments("/d/").build().encodedPath()).isEqualTo(
        "/a/b/c//d/");
    assertThat(base.newBuilder().addPathSegments("d//").build().encodedPath()).isEqualTo(
        "/a/b/c/d//");
    assertThat(base.newBuilder().addPathSegments("//d").build().encodedPath()).isEqualTo(
        "/a/b/c///d");
    assertThat(base.newBuilder().addPathSegments("d/e/f").build().encodedPath()).isEqualTo(
        "/a/b/c/d/e/f");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentsWithBackslash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/", method);
    assertThat(base.newBuilder().addPathSegments("d\\e").build().encodedPath()).isEqualTo("/d/e");
    assertThat(base.newBuilder().addEncodedPathSegments("d\\e").build().encodedPath()).isEqualTo("/d/e");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentsWithEmptyPaths(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegments("/d/e///f").build().encodedPath()).isEqualTo(
        "/a/b/c//d/e///f");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addEncodedPathSegments(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(
        (Object) base.newBuilder().addEncodedPathSegments("d/e/%20/\n").build().encodedPath()).isEqualTo(
        "/a/b/c/d/e/%20/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentDotDoesNothing(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegment(".").build().encodedPath()).isEqualTo("/a/b/c");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentEncodes(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegment("%2e").build().encodedPath()).isEqualTo("/a/b/c/%252e");
    assertThat(base.newBuilder().addPathSegment("%2e%2e").build().encodedPath()).isEqualTo("/a/b/c/%252e%252e");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentDotDotPopsDirectory(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegment("..").build().encodedPath()).isEqualTo("/a/b/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addPathSegmentDotAndIgnoredCharacter(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addPathSegment(".\n").build().encodedPath()).isEqualTo("/a/b/c/.%0A");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addEncodedPathSegmentDotAndIgnoredCharacter(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addEncodedPathSegment(".\n").build().encodedPath()).isEqualTo("/a/b/c");
  }

  @ParameterizedTestWithWebUrlFactory
  public void addEncodedPathSegmentDotDotAndIgnoredCharacter(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().addEncodedPathSegment("..\n").build().encodedPath()).isEqualTo("/a/b/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegment(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().setPathSegment(0, "d").build().encodedPath()).isEqualTo(
        "/d/b/c");
    assertThat(base.newBuilder().setPathSegment(1, "d").build().encodedPath()).isEqualTo(
        "/a/d/c");
    assertThat(base.newBuilder().setPathSegment(2, "d").build().encodedPath()).isEqualTo(
        "/a/b/d");
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegmentEncodes(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().setPathSegment(0, "%25").build().encodedPath()).isEqualTo(
        "/%2525/b/c");
    assertThat(base.newBuilder().setPathSegment(0, ".\n").build().encodedPath()).isEqualTo(
        "/.%0A/b/c");
    assertThat(base.newBuilder().setPathSegment(0, "%2e").build().encodedPath()).isEqualTo(
        "/%252e/b/c");
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegmentAcceptsEmpty(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().setPathSegment(0, "").build().encodedPath()).isEqualTo(
        "//b/c");
    assertThat(base.newBuilder().setPathSegment(2, "").build().encodedPath()).isEqualTo(
        "/a/b/");
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegmentRejectsDot(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setPathSegment(0, ".");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegmentRejectsDotDot(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setPathSegment(0, "..");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setPathSegmentWithSlash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    WebUrl url = base.newBuilder().setPathSegment(1, "/").build();
    assertThat(url.encodedPath()).isEqualTo("/a/%2F/c");
  }

  @Test public void setPathSegmentOutOfBounds() {
    try {
      WebUrl.builder().setPathSegment(1, "a");
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentEncodes(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    assertThat(base.newBuilder().setEncodedPathSegment(0, "%25").build().encodedPath()).isEqualTo(
        "/%25/b/c");
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentRejectsDot(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setEncodedPathSegment(0, ".");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentRejectsDotAndIgnoredCharacter(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setEncodedPathSegment(0, ".\n");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentRejectsDotDot(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setEncodedPathSegment(0, "..");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentRejectsDotDotAndIgnoredCharacter(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    try {
      base.newBuilder().setEncodedPathSegment(0, "..\n");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void setEncodedPathSegmentWithSlash(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    WebUrl url = base.newBuilder().setEncodedPathSegment(1, "/").build();
    assertThat(url.encodedPath()).isEqualTo("/a/%2F/c");
  }

  @Test public void setEncodedPathSegmentOutOfBounds() {
    try {
      WebUrl.builder().setEncodedPathSegment(1, "a");
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void removePathSegment(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    WebUrl url = base.newBuilder()
        .removePathSegment(0)
        .build();
    assertThat(url.encodedPath()).isEqualTo("/b/c");
  }

  @ParameterizedTestWithWebUrlFactory
  public void removePathSegmentDoesntRemovePath(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/a/b/c", method);
    WebUrl url = base.newBuilder()
        .removePathSegment(0)
        .removePathSegment(0)
        .removePathSegment(0)
        .build();
    assertThat(url.pathSegments()).isEqualTo(List.of(""));
    assertThat(url.encodedPath()).isEqualTo("/");
  }

  @Test public void removePathSegmentOutOfBounds() {
    try {
      WebUrl.builder().removePathSegment(1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  @ParameterizedTestWithWebUrlFactory
  public void toJavaNetUrl(WebUrlFactoryMethod method) {
    WebUrl httpUrl = create("http://username:password@host/path?query#fragment", method);
    URL javaNetUrl = httpUrl.url();
    assertThat(javaNetUrl.toString()).isEqualTo(
        "http://username:password@host/path?query#fragment");
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUri(WebUrlFactoryMethod method) {
    WebUrl httpUrl = create("http://username:password@host/path?query#fragment", method);
    URI uri = httpUrl.uri();
    assertThat(uri.toString()).isEqualTo(
        "http://username:password@host/path?query#fragment");
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUriSpecialQueryCharacters(WebUrlFactoryMethod method) {
    WebUrl httpUrl = create("http://host/?d=abc!@[]^`{}|\\", method);
    URI uri = httpUrl.uri();
    assertThat(uri.toString()).isEqualTo("http://host/?d=abc!@[]%5E%60%7B%7D%7C%5C");
  }

  @Test public void toUriWithUsernameNoPassword() {
    WebUrl httpUrl = WebUrl.builder()
        .scheme("http")
        .username("user")
        .host("host")
        .build();
    assertThat(httpUrl.toString()).isEqualTo("http://user@host/");
    assertThat(httpUrl.uri().toString()).isEqualTo("http://user@host/");
  }

  @Test public void toUriUsernameSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .username("=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/");
    assertThat(url.uri().toString()).isEqualTo(
        "http://%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/");
  }

  @Test public void toUriPasswordSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .username("user")
        .password("=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://user:%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/");
    assertThat(url.uri().toString()).isEqualTo(
        "http://user:%3D%5B%5D%3A%3B%22~%7C%3F%23%40%5E%2F$%25*@host/");
  }

  @Test public void toUriPathSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .addPathSegment("=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/=[]:;%22~%7C%3F%23@%5E%2F$%25*");
    assertThat(url.uri().toString()).isEqualTo(
        "http://host/=%5B%5D:;%22~%7C%3F%23@%5E%2F$%25*");
  }

  @Test public void toUriQueryParameterNameSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .addQueryParameter("=[]:;\"~|?#@^/$%*", "a")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://host/?%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*=a");
    assertThat(url.uri().toString()).isEqualTo(
        "http://host/?%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*=a");
    assertThat(url.queryParameter("=[]:;\"~|?#@^/$%*")).isEqualTo("a");
  }

  @Test public void toUriQueryParameterValueSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .addQueryParameter("a", "=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://host/?a=%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*");
    assertThat(url.uri().toString()).isEqualTo(
        "http://host/?a=%3D%5B%5D%3A%3B%22%7E%7C%3F%23%40%5E%2F%24%25*");
    assertThat(url.queryParameter("a")).isEqualTo("=[]:;\"~|?#@^/$%*");
  }

  @Test public void toUriQueryValueSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .query("=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/?=[]:;%22~|?%23@^/$%25*");
    assertThat(url.uri().toString()).isEqualTo("http://host/?=[]:;%22~%7C?%23@%5E/$%25*");
  }

  @Test public void queryCharactersEncodedWhenComposed() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .addQueryParameter("a", "!$(),/:;?@[]\\^`{|}~")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://host/?a=%21%24%28%29%2C%2F%3A%3B%3F%40%5B%5D%5C%5E%60%7B%7C%7D%7E");
    assertThat(url.queryParameter("a")).isEqualTo("!$(),/:;?@[]\\^`{|}~");
  }

  /**
   * When callers use {@code addEncodedQueryParameter()} we only encode what's strictly required.
   * We retain the encoded (or non-encoded) state of the input.
   */
  @Test public void queryCharactersNotReencodedWhenComposedWithAddEncoded() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .addEncodedQueryParameter("a", "!$(),/:;?@[]\\^`{|}~")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/?a=!$(),/:;?@[]\\^`{|}~");
    assertThat(url.queryParameter("a")).isEqualTo("!$(),/:;?@[]\\^`{|}~");
  }

  /**
   * When callers parse a URL with query components that aren't encoded, we shouldn't convert them
   * into a canonical form because doing so could be semantically different.
   */
  @ParameterizedTestWithWebUrlFactory
  public void queryCharactersNotReencodedWhenParsed(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?a=!$(),/:;?@[]\\^`{|}~", method);
    assertThat(url.toString()).isEqualTo("http://host/?a=!$(),/:;?@[]\\^`{|}~");
    assertThat(url.queryParameter("a")).isEqualTo("!$(),/:;?@[]\\^`{|}~");
  }

  @Test public void toUriFragmentSpecialCharacters() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .host("host")
        .fragment("=[]:;\"~|?#@^/$%*")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/#=[]:;\"~|?#@^/$%25*");
    assertThat(url.uri().toString()).isEqualTo("http://host/#=[]:;%22~%7C?%23@%5E/$%25*");
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUriWithControlCharacters(WebUrlFactoryMethod method) {
    // Percent-encoded in the path.
    assertThat(create("http://host/a\u0000b", method).uri()).isEqualTo(URI.create("http://host/a%00b"));
    assertThat(create("http://host/a\u0080b", method).uri()).isEqualTo(URI.create("http://host/a%C2%80b"));
    assertThat(create("http://host/a\u009fb", method).uri()).isEqualTo(URI.create("http://host/a%C2%9Fb"));
    // Percent-encoded in the query.
    assertThat(create("http://host/?a\u0000b", method).uri()).isEqualTo(URI.create("http://host/?a%00b"));
    assertThat(create("http://host/?a\u0080b", method).uri()).isEqualTo(URI.create("http://host/?a%C2%80b"));
    assertThat(create("http://host/?a\u009fb", method).uri()).isEqualTo(URI.create("http://host/?a%C2%9Fb"));
    // Stripped from the fragment.
    assertThat(create("http://host/#a\u0000b", method).uri()).isEqualTo(URI.create("http://host/#a%00b"));
    assertThat(create("http://host/#a\u0080b", method).uri()).isEqualTo(URI.create("http://host/#ab"));
    assertThat(create("http://host/#a\u009fb", method).uri()).isEqualTo(URI.create("http://host/#ab"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUriWithSpaceCharacters(WebUrlFactoryMethod method) {
    // Percent-encoded in the path.
    assertThat(create("http://host/a\u000bb", method).uri()).isEqualTo(URI.create("http://host/a%0Bb"));
    assertThat(create("http://host/a b", method).uri()).isEqualTo(URI.create("http://host/a%20b"));
    assertThat(create("http://host/a\u2009b", method).uri()).isEqualTo(URI.create("http://host/a%E2%80%89b"));
    assertThat(create("http://host/a\u3000b", method).uri()).isEqualTo(URI.create("http://host/a%E3%80%80b"));
    // Percent-encoded in the query.
    assertThat(create("http://host/?a\u000bb", method).uri()).isEqualTo(URI.create("http://host/?a%0Bb"));
    assertThat(create("http://host/?a b", method).uri()).isEqualTo(URI.create("http://host/?a%20b"));
    assertThat(create("http://host/?a\u2009b", method).uri()).isEqualTo(URI.create("http://host/?a%E2%80%89b"));
    assertThat(create("http://host/?a\u3000b", method).uri()).isEqualTo(URI.create("http://host/?a%E3%80%80b"));
    // Stripped from the fragment.
    assertThat(create("http://host/#a\u000bb", method).uri()).isEqualTo(URI.create("http://host/#a%0Bb"));
    assertThat(create("http://host/#a b", method).uri()).isEqualTo(URI.create("http://host/#a%20b"));
    assertThat(create("http://host/#a\u2009b", method).uri()).isEqualTo(URI.create("http://host/#ab"));
    assertThat(create("http://host/#a\u3000b", method).uri()).isEqualTo(URI.create("http://host/#ab"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUriWithNonHexPercentEscape(WebUrlFactoryMethod method) {
    assertThat(create("http://host/%xx", method).uri()).isEqualTo(URI.create("http://host/%25xx"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void toUriWithTruncatedPercentEscape(WebUrlFactoryMethod method) {
    assertThat(create("http://host/%a", method).uri()).isEqualTo(URI.create("http://host/%25a"));
    assertThat(create("http://host/%", method).uri()).isEqualTo(URI.create("http://host/%25"));
  }

  @Test public void fromJavaNetUrl() throws MalformedURLException {
    URL javaNetUrl = new URL("http://username:password@host/path?query#fragment");
    WebUrl httpUrl = WebUrl.from(javaNetUrl).orElse(null);
    assertThat(httpUrl.toString()).isEqualTo(
        "http://username:password@host/path?query#fragment");
  }

  @Test public void fromJavaNetUrlUnsupportedScheme() throws MalformedURLException {
    URL javaNetUrl = new URL("mailto:user@example.com");
    assertThat(WebUrl.from(javaNetUrl).orElse(null)).isNull();
  }

  @Test public void ofUri() {
    URI uri = URI.create("http://username:password@host/path?query#fragment");
    WebUrl httpUrl = WebUrl.of(uri);
    assertThat(httpUrl.toString()).isEqualTo(
        "http://username:password@host/path?query#fragment");
  }

  @Test public void ofUriUnsupportedScheme() {
    URI uri = URI.create("mailto:user@example.com");
    assertThrows(IllegalArgumentException.class, () -> WebUrl.of(uri));
  }

  @Test public void ofUriPartial() {
    URI uri = URI.create("/path");
    assertThrows(IllegalArgumentException.class, () -> WebUrl.of(uri));
  }

  @Test public void fromUri() {
    URI uri = URI.create("http://username:password@host/path?query#fragment");
    WebUrl httpUrl = WebUrl.from(uri).orElse(null);
    assertThat(httpUrl.toString()).isEqualTo(
        "http://username:password@host/path?query#fragment");
  }

  @Test public void fromUriUnsupportedScheme() {
    URI uri = URI.create("mailto:user@example.com");
    assertThat(WebUrl.from(uri).orElse(null)).isNull();
  }

  @Test public void fromUriPartial() {
    URI uri = URI.create("/path");
    assertThat(WebUrl.from(uri).orElse(null)).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQueryWithComponents(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/", method);
    WebUrl url = base.newBuilder().addQueryParameter("a+=& b", "c+=& d").build();
    assertThat(url.toString()).isEqualTo("http://host/?a%2B%3D%26%20b=c%2B%3D%26%20d");
    assertThat(url.queryParameterValue(0)).isEqualTo("c+=& d");
    assertThat(url.queryParameterName(0)).isEqualTo("a+=& b");
    assertThat(url.queryParameter("a+=& b")).isEqualTo("c+=& d");
    assertThat(url.queryParameterNames()).isEqualTo(Collections.singleton("a+=& b"));
    assertThat(url.queryParameterValues("a+=& b")).isEqualTo(singletonList("c+=& d"));
    assertThat(url.querySize()).isEqualTo(1);
    // Ambiguous! (Though working as designed.)
    assertThat(url.query()).isEqualTo("a+=& b=c+=& d");
    assertThat(url.encodedQuery()).isEqualTo("a%2B%3D%26%20b=c%2B%3D%26%20d");
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQueryWithEncodedComponents(WebUrlFactoryMethod method) {
    WebUrl base = create("http://host/", method);
    WebUrl url = base.newBuilder().addEncodedQueryParameter("a+=& b", "c+=& d").build();
    assertThat(url.toString()).isEqualTo("http://host/?a+%3D%26%20b=c+%3D%26%20d");
    assertThat(url.queryParameter("a =& b")).isEqualTo("c =& d");
  }

  @ParameterizedTestWithWebUrlFactory
  public void removeAllQueryParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addQueryParameter("a+=& b", "c+=& d")
        .removeAllQueryParameters()
        .build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.queryParameterNames()).isEmpty();
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQueryRemoveQueryParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addQueryParameter("a+=& b", "c+=& d")
        .removeAllQueryParameters("a+=& b")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.queryParameter("a+=& b")).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQueryRemoveEncodedQueryParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addEncodedQueryParameter("a+=& b", "c+=& d")
        .removeAllEncodedQueryParameters("a+=& b")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.queryParameter("a =& b")).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQuerySetQueryParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addQueryParameter("a+=& b", "c+=& d")
        .setQueryParameter("a+=& b", "ef")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/?a%2B%3D%26%20b=ef");
    assertThat(url.queryParameter("a+=& b")).isEqualTo("ef");
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQuerySetEncodedQueryParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addEncodedQueryParameter("a+=& b", "c+=& d")
        .setEncodedQueryParameter("a+=& b", "ef")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/?a+%3D%26%20b=ef");
    assertThat(url.queryParameter("a =& b")).isEqualTo("ef");
  }

  @ParameterizedTestWithWebUrlFactory
  public void composeQueryMultipleEncodedValuesForParameter(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .addQueryParameter("a+=& b", "c+=& d")
        .addQueryParameter("a+=& b", "e+=& f")
        .build();
    assertThat(url.toString()).isEqualTo(
        "http://host/?a%2B%3D%26%20b=c%2B%3D%26%20d&a%2B%3D%26%20b=e%2B%3D%26%20f");
    assertThat(url.querySize()).isEqualTo(2);
    assertThat(url.queryParameterNames()).isEqualTo(Collections.singleton("a+=& b"));
    assertThat(url.queryParameterValues("a+=& b")).isEqualTo(List.of("c+=& d", "e+=& f"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void absentQueryIsZeroNameValuePairs(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .query(null)
        .build();
    assertThat(url.querySize()).isEqualTo(0);
  }

  @ParameterizedTestWithWebUrlFactory
  public void emptyQueryIsSingleNameValuePairWithEmptyKey(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .query("")
        .build();
    assertThat(url.querySize()).isEqualTo(1);
    assertThat(url.queryParameterName(0)).isEqualTo("");
    assertThat(url.queryParameterValue(0)).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void ampersandQueryIsTwoNameValuePairsWithEmptyKeys(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .query("&")
        .build();
    assertThat(url.querySize()).isEqualTo(2);
    assertThat(url.queryParameterName(0)).isEqualTo("");
    assertThat(url.queryParameterValue(0)).isNull();
    assertThat(url.queryParameterName(1)).isEqualTo("");
    assertThat(url.queryParameterValue(1)).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void removeAllDoesNotRemoveQueryIfNoParametersWereRemoved(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/", method).newBuilder()
        .query("")
        .removeAllQueryParameters("a")
        .build();
    assertThat(url.toString()).isEqualTo("http://host/?");
  }

  @ParameterizedTestWithWebUrlFactory
  public void queryParametersWithoutValues(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?foo&bar&baz", method);
    assertThat(url.querySize()).isEqualTo(3);
    assertThat(url.queryParameterNames()).isEqualTo(
        new LinkedHashSet<>(List.of("foo", "bar", "baz")));
    assertThat(url.queryParameterValue(0)).isNull();
    assertThat(url.queryParameterValue(1)).isNull();
    assertThat(url.queryParameterValue(2)).isNull();
    assertThat(url.queryParameterValues("foo")).isEqualTo(singletonList((String) null));
    assertThat(url.queryParameterValues("bar")).isEqualTo(singletonList((String) null));
    assertThat(url.queryParameterValues("baz")).isEqualTo(singletonList((String) null));
  }

  @ParameterizedTestWithWebUrlFactory
  public void queryParametersWithEmptyValues(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?foo=&bar=&baz=", method);
    assertThat(url.querySize()).isEqualTo(3);
    assertThat(url.queryParameterNames()).isEqualTo(
        new LinkedHashSet<>(List.of("foo", "bar", "baz")));
    assertThat(url.queryParameterValue(0)).isEqualTo("");
    assertThat(url.queryParameterValue(1)).isEqualTo("");
    assertThat(url.queryParameterValue(2)).isEqualTo("");
    assertThat(url.queryParameterValues("foo")).isEqualTo(singletonList(""));
    assertThat(url.queryParameterValues("bar")).isEqualTo(singletonList(""));
    assertThat(url.queryParameterValues("baz")).isEqualTo(singletonList(""));
  }

  @ParameterizedTestWithWebUrlFactory
  public void queryParametersWithRepeatedName(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?foo[]=1&foo[]=2&foo[]=3", method);
    assertThat(url.querySize()).isEqualTo(3);
    assertThat(url.queryParameterNames()).isEqualTo(Collections.singleton("foo[]"));
    assertThat(url.queryParameterValue(0)).isEqualTo("1");
    assertThat(url.queryParameterValue(1)).isEqualTo("2");
    assertThat(url.queryParameterValue(2)).isEqualTo("3");
    assertThat(url.queryParameterValues("foo[]")).isEqualTo(List.of("1", "2", "3"));
  }

  @ParameterizedTestWithWebUrlFactory
  public void queryParameterLookupWithNonCanonicalEncoding(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?%6d=m&+=%20", method);
    assertThat(url.queryParameterName(0)).isEqualTo("m");
    assertThat(url.queryParameterName(1)).isEqualTo(" ");
    assertThat(url.queryParameter("m")).isEqualTo("m");
    assertThat(url.queryParameter(" ")).isEqualTo(" ");
  }

  @ParameterizedTestWithWebUrlFactory
  public void parsedQueryDoesntIncludeFragment(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/?#fragment", method);
    assertThat(url.fragment()).isEqualTo("fragment");
    assertThat(url.query()).isEqualTo("");
    assertThat(url.encodedQuery()).isEqualTo("");
  }

  @Test public void roundTripBuilder() {
    WebUrl url = WebUrl.builder()
        .scheme("http")
        .username("%")
        .password("%")
        .host("host")
        .addPathSegment("%")
        .query("%")
        .fragment("%")
        .build();
    assertThat(url.toString()).isEqualTo("http://%25:%25@host/%25?%25#%25");
    assertThat(url.newBuilder().build().toString()).isEqualTo(
        "http://%25:%25@host/%25?%25#%25");
    assertThat(url.resolve("").toString()).isEqualTo("http://%25:%25@host/%25?%25");
  }

  /**
   * Although WebUrl prefers percent-encodings in uppercase, it should preserve the exact structure
   * of the original encoding.
   */
  @ParameterizedTestWithWebUrlFactory
  public void rawEncodingRetained(WebUrlFactoryMethod method) {
    String urlString = "http://%6d%6D:%6d%6D@host/%6d%6D?%6d%6D#%6d%6D";
    WebUrl url = create(urlString, method);
    assertThat(url.encodedUsername()).isEqualTo("%6d%6D");
    assertThat(url.encodedPassword()).isEqualTo("%6d%6D");
    assertThat(url.encodedPath()).isEqualTo("/%6d%6D");
    assertThat(url.encodedPathSegments()).isEqualTo(List.of("%6d%6D"));
    assertThat(url.encodedQuery()).isEqualTo("%6d%6D");
    assertThat(url.encodedFragment()).isEqualTo("%6d%6D");
    assertThat(url.toString()).isEqualTo(urlString);
    assertThat(url.newBuilder().build().toString()).isEqualTo(urlString);
    assertThat(url.resolve("").toString()).isEqualTo(
        "http://%6d%6D:%6d%6D@host/%6d%6D?%6d%6D");
  }

  @ParameterizedTestWithWebUrlFactory
  public void clearFragment(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#fragment", method)
        .newBuilder()
        .fragment(null)
        .build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.fragment()).isNull();
    assertThat(url.encodedFragment()).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void clearEncodedFragment(WebUrlFactoryMethod method) {
    WebUrl url = create("http://host/#fragment", method)
        .newBuilder()
        .encodedFragment(null)
        .build();
    assertThat(url.toString()).isEqualTo("http://host/");
    assertThat(url.fragment()).isNull();
    assertThat(url.encodedFragment()).isNull();
  }

  @ParameterizedTestWithWebUrlFactory
  public void topPrivateDomain(WebUrlFactoryMethod method) {
    assertThat(create("https://google.com", method).topPrivateDomain()).isEqualTo("google.com");
    assertThat(create("https://adwords.google.co.uk", method).topPrivateDomain()).isEqualTo(
        "google.co.uk");
    assertThat(create("https://栃.栃木.jp", method).topPrivateDomain()).isEqualTo(
        "xn--ewv.xn--4pvxs.jp");
    assertThat(create("https://xn--ewv.xn--4pvxs.jp", method).topPrivateDomain()).isEqualTo(
        "xn--ewv.xn--4pvxs.jp");

    assertThat(create("https://co.uk", method).topPrivateDomain()).isNull();
    assertThat(create("https://square", method).topPrivateDomain()).isNull();
    assertThat(create("https://栃木.jp", method).topPrivateDomain()).isNull();
    assertThat(create("https://xn--4pvxs.jp", method).topPrivateDomain()).isNull();
    assertThat(create("https://localhost", method).topPrivateDomain()).isNull();
    assertThat(create("https://127.0.0.1", method).topPrivateDomain()).isNull();
  }
}
