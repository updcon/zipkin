/*
 * Copyright 2015-2023 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import zipkin2.Endpoint;
import zipkin2.Span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static zipkin2.TestObjects.BACKEND;
import static zipkin2.TestObjects.TRACE;
import static zipkin2.codec.SpanBytesEncoderTest.ERROR_SPAN;
import static zipkin2.codec.SpanBytesEncoderTest.LOCAL_SPAN;
import static zipkin2.codec.SpanBytesEncoderTest.NO_ANNOTATIONS_ROOT_SERVER_SPAN;
import static zipkin2.codec.SpanBytesEncoderTest.SPAN;
import static zipkin2.codec.SpanBytesEncoderTest.UTF8_SPAN;
import static zipkin2.codec.SpanBytesEncoderTest.UTF_8;

class SpanBytesDecoderTest {
  Span span = SPAN;

  @Test void niceErrorOnTruncatedSpans_PROTO3() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      byte[] encoded = SpanBytesEncoder.PROTO3.encodeList(TRACE);
      SpanBytesDecoder.PROTO3.decodeList(Arrays.copyOfRange(encoded, 0, 10));
    });
    assertThat(exception.getMessage()).contains("Truncated: length 66 > bytes available 8 reading List<Span> from proto3");
  }

  @Test void niceErrorOnTruncatedSpan_PROTO3() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      byte[] encoded = SpanBytesEncoder.PROTO3.encode(SPAN);
      SpanBytesDecoder.PROTO3.decodeOne(Arrays.copyOfRange(encoded, 0, 10));
    });
    assertThat(exception.getMessage()).contains("Truncated: length 179 > bytes available 7 reading Span from proto3");
  }

  @Test void emptyListOk_JSON_V1() {
    assertThat(SpanBytesDecoder.JSON_V1.decodeList(new byte[0]))
      .isEmpty(); // instead of throwing an exception
    assertThat(SpanBytesDecoder.JSON_V1.decodeList(new byte[] {'[', ']'}))
      .isEmpty(); // instead of throwing an exception
  }

  @Test void emptyListOk_JSON_V2() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeList(new byte[0]))
      .isEmpty(); // instead of throwing an exception
    assertThat(SpanBytesDecoder.JSON_V2.decodeList(new byte[] {'[', ']'}))
      .isEmpty(); // instead of throwing an exception
  }

  @Test void emptyListOk_PROTO3() {
    assertThat(SpanBytesDecoder.PROTO3.decodeList(new byte[0]))
      .isEmpty(); // instead of throwing an exception
  }

  @Test void spanRoundTrip_JSON_V2() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_PROTO3() {
    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void localSpanRoundTrip_JSON_V2() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(LOCAL_SPAN)))
      .isEqualTo(LOCAL_SPAN);
  }

  @Test void localSpanRoundTrip_PROTO3() {
    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(LOCAL_SPAN)))
      .isEqualTo(LOCAL_SPAN);
  }

  @Test void errorSpanRoundTrip_JSON_V2() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(ERROR_SPAN)))
      .isEqualTo(ERROR_SPAN);
  }

  @Test void errorSpanRoundTrip_PROTO3() {
    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(ERROR_SPAN)))
      .isEqualTo(ERROR_SPAN);
  }

  @Test void spanRoundTrip_64bitTraceId_JSON_V2() {
    span = span.toBuilder().traceId(span.traceId().substring(16)).build();

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_64bitTraceId_PROTO3() {
    span = span.toBuilder().traceId(span.traceId().substring(16)).build();

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_shared_JSON_V2() {
    span = span.toBuilder().kind(Span.Kind.SERVER).shared(true).build();

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_shared_PROTO3() {
    span = span.toBuilder().kind(Span.Kind.SERVER).shared(true).build();

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  /**
   * This isn't a test of what we "should" accept as a span, rather that characters that trip-up
   * json don't fail in codec.
   */
  @Test void specialCharsInJson_JSON_V2() {
    assertThat(
      SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(UTF8_SPAN)))
      .isEqualTo(UTF8_SPAN);
  }

  @Test void specialCharsInJson_PROTO3() {
    assertThat(
      SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(UTF8_SPAN)))
      .isEqualTo(UTF8_SPAN);
  }

  @Test void falseOnEmpty_inputSpans_JSON_V2() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeList(new byte[0], new ArrayList<>()))
      .isFalse();
  }

  @Test void falseOnEmpty_inputSpans_PROTO3() {
    assertThat(SpanBytesDecoder.PROTO3.decodeList(new byte[0], new ArrayList<>()))
      .isFalse();
  }

  /**
   * Particular, thrift can mistake malformed content as a huge list. Let's not blow up.
   */
  @Test void niceErrorOnMalformed_inputSpans_JSON_V2() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      SpanBytesDecoder.JSON_V2.decodeList(new byte[] {'h', 'e', 'l', 'l', 'o'});
    });
    assertThat(exception.getMessage()).contains("Malformed reading List<Span> from ");
  }

  @Test void niceErrorOnMalformed_inputSpans_PROTO3() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      SpanBytesDecoder.PROTO3.decodeList(new byte[] {'h', 'e', 'l', 'l', 'o'});
    });
    assertThat(exception.getMessage()).contains("Truncated: length 101 > bytes available 3 reading List<Span> from proto3");
  }

  @Test void traceRoundTrip_JSON_V2() {
    byte[] message = SpanBytesEncoder.JSON_V2.encodeList(TRACE);

    assertThat(SpanBytesDecoder.JSON_V2.decodeList(message)).isEqualTo(TRACE);
  }

  @Test void traceRoundTrip_PROTO3() {
    byte[] message = SpanBytesEncoder.PROTO3.encodeList(TRACE);

    assertThat(SpanBytesDecoder.PROTO3.decodeList(message)).isEqualTo(TRACE);
  }

  @Test void traceRoundTrip_PROTO3_directBuffer() {
    byte[] message = SpanBytesEncoder.PROTO3.encodeList(TRACE);
    ByteBuffer buf = ByteBuffer.allocateDirect(message.length);
    buf.put(message);
    buf.flip();

    assertThat(SpanBytesDecoder.PROTO3.decodeList(buf)).isEqualTo(TRACE);
  }

  @Test void traceRoundTrip_PROTO3_heapBuffer() {
    byte[] message = SpanBytesEncoder.PROTO3.encodeList(TRACE);
    ByteBuffer buf = ByteBuffer.wrap(message);

    assertThat(SpanBytesDecoder.PROTO3.decodeList(buf)).isEqualTo(TRACE);
  }

  @Test void traceRoundTrip_PROTO3_heapBufferOffset() {
    byte[] message = SpanBytesEncoder.PROTO3.encodeList(TRACE);
    byte[] array = new byte[message.length + 4 + 5];
    System.arraycopy(message, 0, array, 4, message.length);
    ByteBuffer buf = ByteBuffer.wrap(array, 4, message.length);

    assertThat(SpanBytesDecoder.PROTO3.decodeList(buf)).isEqualTo(TRACE);
  }

  @Test void spansRoundTrip_JSON_V2() {
    List<Span> tenClientSpans = Collections.nCopies(10, span);

    byte[] message = SpanBytesEncoder.JSON_V2.encodeList(tenClientSpans);

    assertThat(SpanBytesDecoder.JSON_V2.decodeList(message))
      .isEqualTo(tenClientSpans);
  }

  @Test void spansRoundTrip_PROTO3() {
    List<Span> tenClientSpans = Collections.nCopies(10, span);

    byte[] message = SpanBytesEncoder.PROTO3.encodeList(tenClientSpans);

    assertThat(SpanBytesDecoder.PROTO3.decodeList(message))
      .isEqualTo(tenClientSpans);
  }

  @Test void spanRoundTrip_noRemoteServiceName_JSON_V2() {
    span = span.toBuilder()
      .remoteEndpoint(BACKEND.toBuilder().serviceName(null).build())
      .build();

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noRemoteServiceName_PROTO3() {
    span = span.toBuilder()
      .remoteEndpoint(BACKEND.toBuilder().serviceName(null).build())
      .build();

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_JSON_V2() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN;

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_PROTO3() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN;

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_incomplete_JSON_V2() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN.toBuilder().duration(null).build();

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_incomplete_PROTO3() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN.toBuilder().duration(null).build();

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_shared_JSON_V2() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN.toBuilder().shared(true).build();

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(SpanBytesEncoder.JSON_V2.encode(span)))
      .isEqualTo(span);
  }

  @Test void spanRoundTrip_noAnnotations_rootServerSpan_shared_PROTO3() {
    span = NO_ANNOTATIONS_ROOT_SERVER_SPAN.toBuilder().shared(true).build();

    assertThat(SpanBytesDecoder.PROTO3.decodeOne(SpanBytesEncoder.PROTO3.encode(span)))
      .isEqualTo(span);
  }

  @Test void niceErrorOnUppercase_traceId_JSON_V2() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"48485A3953BB6124\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\"\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("48485A3953BB6124 should be lower-hex encoded with no prefix");
  }

  @Test void readsTraceIdHighFromTraceIdField() {
    byte[] with128BitTraceId = ("{\n"
      + "  \"traceId\": \"48485a3953bb61246b221d5bc9e6496c\",\n"
      + "  \"name\": \"get-traces\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\"\n"
      + "}").getBytes(UTF_8);
    byte[] withLower64bitsTraceId = ("{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"name\": \"get-traces\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\"\n"
      + "}").getBytes(UTF_8);

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(with128BitTraceId))
      .isEqualTo(SpanBytesDecoder.JSON_V2.decodeOne(withLower64bitsTraceId).toBuilder()
        .traceId("48485a3953bb61246b221d5bc9e6496c").build());
  }

  @Test void ignoresNull_topLevelFields() {
    String json = "{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"parentId\": null,\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"name\": null,\n"
      + "  \"timestamp\": null,\n"
      + "  \"duration\": null,\n"
      + "  \"localEndpoint\": null,\n"
      + "  \"remoteEndpoint\": null,\n"
      + "  \"annotations\": null,\n"
      + "  \"tags\": null,\n"
      + "  \"debug\": null,\n"
      + "  \"shared\": null\n"
      + "}";

    SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
  }

  @Test void ignoresNull_endpoint_topLevelFields() {
    String json = "{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"name\": \"get-traces\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"localEndpoint\": {\n"
      + "    \"serviceName\": null,\n"
      + "    \"ipv4\": \"127.0.0.1\",\n"
      + "    \"ipv6\": null,\n"
      + "    \"port\": null\n"
      + "  }\n"
      + "}";

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8)).localEndpoint())
      .isEqualTo(Endpoint.newBuilder().ip("127.0.0.1").build());
  }

  @Test void skipsIncompleteEndpoint() {
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(("{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"localEndpoint\": {\n"
      + "    \"serviceName\": null,\n"
      + "    \"ipv4\": null,\n"
      + "    \"ipv6\": null,\n"
      + "    \"port\": null\n"
      + "  }\n"
      + "}")
      .getBytes(UTF_8)).localEndpoint()).isNull();
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(("{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"localEndpoint\": {\n"
      + "  }\n"
      + "}")
      .getBytes(UTF_8)).localEndpoint()).isNull();
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(("{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"remoteEndpoint\": {\n"
      + "    \"serviceName\": null,\n"
      + "    \"ipv4\": null,\n"
      + "    \"ipv6\": null,\n"
      + "    \"port\": null\n"
      + "  }\n"
      + "}")
      .getBytes(UTF_8)).remoteEndpoint()).isNull();
    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(("{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"remoteEndpoint\": {\n"
      + "  }\n"
      + "}")
      .getBytes(UTF_8)).remoteEndpoint()).isNull();
  }

  @Test void niceErrorOnIncomplete_annotation() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\",\n"
        + "  \"annotations\": [\n"
        + "    { \"timestamp\": 1472470996199000}\n"
        + "  ]\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("Incomplete annotation at $.annotations[0].timestamp");
  }

  @Test void niceErrorOnNull_traceId() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": null,\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\"\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("Expected a string but was NULL");
  }

  @Test void niceErrorOnNull_id() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": null\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("Expected a string but was NULL");
  }

  @Test void niceErrorOnNull_tagValue() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\",\n"
        + "  \"tags\": {\n"
        + "    \"foo\": NULL\n"
        + "  }\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("No value at $.tags.foo");
  }

  @Test void niceErrorOnNull_annotationValue() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\",\n"
        + "  \"annotations\": [\n"
        + "    { \"timestamp\": 1472470996199000, \"value\": NULL}\n"
        + "  ]\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("$.annotations[0].value");
  }

  @Test void niceErrorOnNull_annotationTimestamp() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

      String json = "{\n"
        + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
        + "  \"name\": \"get-traces\",\n"
        + "  \"id\": \"6b221d5bc9e6496c\",\n"
        + "  \"annotations\": [\n"
        + "    { \"timestamp\": NULL, \"value\": \"foo\"}\n"
        + "  ]\n"
        + "}";

      SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8));
    });
    assertThat(exception.getMessage()).contains("$.annotations[0].timestamp");
  }

  @Test void readSpan_localEndpoint_noServiceName() {
    String json = "{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"name\": \"get-traces\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"localEndpoint\": {\n"
      + "    \"ipv4\": \"127.0.0.1\"\n"
      + "  }\n"
      + "}";

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8)).localServiceName())
      .isNull();
  }

  @Test void readSpan_remoteEndpoint_noServiceName() {
    String json = "{\n"
      + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
      + "  \"name\": \"get-traces\",\n"
      + "  \"id\": \"6b221d5bc9e6496c\",\n"
      + "  \"remoteEndpoint\": {\n"
      + "    \"ipv4\": \"127.0.0.1\"\n"
      + "  }\n"
      + "}";

    assertThat(SpanBytesDecoder.JSON_V2.decodeOne(json.getBytes(UTF_8)).remoteServiceName())
      .isNull();
  }
}
