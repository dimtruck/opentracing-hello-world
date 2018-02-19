package utils;

import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;

import java.util.Iterator;
import java.util.Map;

public class HttpHeaderInjector implements TextMap {

    private HttpHeaders headers;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpHeaderInjector.class);

    public HttpHeaderInjector(HttpHeaders headers) {
        this.headers = headers;
    }


    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("Should be used only with tracer#inject()");
    }

    @Override
    public void put(String key, String value) {

        try {
            this.headers.set(key, value);
        } catch (Exception e) {
            LOG.error("Unable to add a trace header.  Key: " + key + ", value: " + value);
        }
    }
}