/*
 * Copyright (C) 2017 Square, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.GzipSink;
import okio.Okio;
import okio.Sink;

/**
 * Downloads the public suffix list from https://publicsuffix.org/list/public_suffix_list.dat and
 * transforms the file into an efficient format used by OkHttp.
 *
 * <p>The intent is to use this class to update the list periodically by manually running the main
 * method. This should be run from the top-level okhttp directory.
 *
 * <p>The resulting file is used by {@link PublicSuffixDatabase}.
 */
public final class PublicSuffixListGenerator {
  private static final Path OKHTTP_RESOURCE_DIR =
      Path.of("src/main/resources/net/disy/commons/web");

  private static final ByteString EXCEPTION_RULE_MARKER = ByteString.encodeUtf8("!");
  private static final String WILDCARD_CHAR = "*";

  public static void main(String... args) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder(URI.create("https://publicsuffix.org/list/public_suffix_list.dat")).build();
    SortedSet<ByteString> sortedRules = new TreeSet<>();
    SortedSet<ByteString> sortedExceptionRules = new TreeSet<>();
    HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
    BufferedSource source = Okio.buffer(Okio.source(response.body()));
    int totalRuleBytes = 0;
    int totalExceptionRuleBytes = 0;
    while (!source.exhausted()) {
      String line = source.readUtf8LineStrict();
      if (line.trim().isEmpty() || line.startsWith("//")) continue;

      if (line.contains(WILDCARD_CHAR)) {
        assertWildcardRule(line);
      }

      ByteString rule = ByteString.encodeUtf8(line);
      if (rule.startsWith(EXCEPTION_RULE_MARKER)) {
        rule = rule.substring(1);
        // We use '\n' for end of value.
        totalExceptionRuleBytes += rule.size() + 1;
        sortedExceptionRules.add(rule);
      } else {
        totalRuleBytes += rule.size() + 1; // We use '\n' for end of value.
        sortedRules.add(rule);
      }
    }

    Path resources = Files.createDirectories(OKHTTP_RESOURCE_DIR);

    Sink fileSink = Okio.sink(resources.resolve(PublicSuffixDatabase.PUBLIC_SUFFIX_RESOURCE));
    try (BufferedSink sink = Okio.buffer(new GzipSink(fileSink))) {
      sink.writeInt(totalRuleBytes);
      for (ByteString domain : sortedRules) {
        sink.write(domain).writeByte('\n');
      }

      sink.writeInt(totalExceptionRuleBytes);
      for (ByteString domain : sortedExceptionRules) {
        sink.write(domain).writeByte('\n');
      }
    }
    System.out.println("PublicSuffixListGenerator completed successfully!");
  }

  /**
   * These assertions ensure the {@link PublicSuffixDatabase} remains correct. The specification is
   * very flexible regarding wildcard rules, but this flexibility is not something currently used
   * in practice. To simplify the implementation, we've avoided implementing the flexible rules in
   * favor of supporting what's actually used in practice. That means if these assertions ever fail,
   * the implementation will need to be revisited to support a more flexible rule.
   */
  private static void assertWildcardRule(String rule) {
    if (rule.indexOf(WILDCARD_CHAR) != 0) {
      throw new RuntimeException("Wildcard Assertion Failure: " + rule + "\nA wildcard rule was "
          + "added with a wildcard that is not in leftmost position! We'll need to change the "
          + PublicSuffixDatabase.class.getName() + " to handle this.");
    }
    if (rule.indexOf(WILDCARD_CHAR, 1) != -1) {
      throw new RuntimeException("Wildcard Assertion Failure: " + rule + "\nA wildcard rule was "
          + "added with multiple wildcards! We'll need to change "
          + PublicSuffixDatabase.class.getName() + " to handle this.");
    }
    if (rule.length() == 1) {
      throw new RuntimeException("Wildcard Assertion Failure: " + rule + "\nA wildcard rule was "
          + "added that wildcards the first level! We'll need to change the "
          + PublicSuffixDatabase.class.getName() + " to handle this.");
    }
  }
}
