package utils;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
public class OpenTracingImpl {
    private static final Logger logger = LoggerFactory.getLogger(OpenTracingImpl.class);

    @Value("${trace_enabled}")
    private boolean isTracingEnabled;

    @Value("${tracer}")
    private String tracer;

    @Value("${tracer_agent_host}")
    private String tracerHost;

    @Value("${tracer_agent_port}")
    private int tracerPort;

    @Value("${tracer_service_name}")
    private String tracerServiceName;

    public OpenTracingImpl() {}

    /**
     * Load the jvm singleton tarcer as the "tracer" for the application context.
     * @return
     */
    @Primary
    @Bean(name = "openTracingTracer")
    public Tracer getGlobalTracer(){
        if (!GlobalTracer.isRegistered()) {
            logger.error("GlobalTracer not yet registered");

            if (isTracingEnabled) {
                switch (tracer) {
                    case "jaeger":
                        GlobalTracer.register(
                                new com.uber.jaeger.Configuration(
                                        tracerServiceName,
                                        new com.uber.jaeger.Configuration.SamplerConfiguration("const", 1),
                                        new com.uber.jaeger.Configuration.ReporterConfiguration(
                                                true,  // logSpans
                                                tracerHost,
                                                tracerPort,
                                                1000,
                                                10000)
                                ).getTracer());
                        break;
                    default:
                        logger.error("Invalid tracer specified.  Problem with " + tracer);
                }
            }
        }
        return GlobalTracer.get();
    }

}
