/**
 * Copyright 2026 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.http;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HttpRequestTags}.
 */
class HttpRequestTagsTest {

    private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

    @Test
    void nullRequestShouldContributeUnknownMethodTag() {
        assertThat(HttpRequestTags.method(null)).isEqualTo(METHOD_UNKNOWN);
    }

    @Test
    void unknownMethodShouldContributeUnknownMethodTagValue() {
        assertThat(HttpRequestTags.method(mockRequest("WELL"))).isEqualTo(METHOD_UNKNOWN);
    }

    @Test
    void requestShouldContributeMethodTag() {
        Tag httpGet = Tag.of("method", "GET");
        assertThat(HttpRequestTags.method(mockRequest("GET"))).isEqualTo(httpGet);
    }

    @Test
    void standardMethodsShouldContributeMethodTag() {
        for (String method : new String[] { "GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE",
                "PATCH" }) {
            Tag expected = Tag.of("method", method);
            assertThat(HttpRequestTags.method(mockRequest(method))).isEqualTo(expected);
        }
    }

    @Test
    void attackerControlledMethodsShouldNotInflateTagCardinality() {
        // an attacker sending arbitrary request methods must not be able to create
        // an unbounded number of distinct `method` tag values
        for (String method : new String[] { "get", "Get", "ATTACK-0", "ATTACK-1", "", "GET /foo HTTP/1.1" }) {
            assertThat(HttpRequestTags.method(mockRequest(method))).isEqualTo(METHOD_UNKNOWN);
        }
    }

    @Test
    void defaultTagsProviderShouldNotInflateMethodTagCardinality() {
        // DefaultHttpServletRequestTagsProvider is what server instrumentation (e.g. the
        // Jetty TimedHandler) uses to tag every incoming request
        DefaultHttpServletRequestTagsProvider provider = new DefaultHttpServletRequestTagsProvider();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        for (String method : new String[] { "get", "ATTACK-0", "ATTACK-1", "", "GET /foo HTTP/1.1" }) {
            assertThat(provider.getTags(mockRequest(method), response)).contains(METHOD_UNKNOWN)
                    .noneMatch(tag -> "method".equals(tag.getKey()) && method.equals(tag.getValue()));
        }
        assertThat(provider.getTags(mockRequest("GET"), response)).contains(Tag.of("method", "GET"));
    }

    private HttpServletRequest mockRequest(String method) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        return request;
    }

}
