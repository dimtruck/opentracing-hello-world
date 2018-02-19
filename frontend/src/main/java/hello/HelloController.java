package hello;

import com.google.gson.Gson;
import domains.*;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.apache.http.HttpHost;
import org.apache.http.RequestLine;
import org.apache.http.client.protocol.HttpClientContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.NoHandlerFoundException;
import utils.HttpHeaderInjector;
import utils.OpenTracingImpl;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Controller
public class HelloController {

    OpenTracingImpl openTracingService;

    private static final Logger logger = LoggerFactory.getLogger(
            HelloController.class);


    @Value("${api.url}")
    private String apiUrl;


    @Autowired
    public void setOpenTracingService(OpenTracingImpl openTracingService) {
        this.openTracingService = openTracingService;
    }


    @GetMapping("/hello")
    public String hello(Model model, @RequestParam(required = false) String language, HttpServletRequest request) {
        logger.info("start an active span");
        Span activeSpan = openTracingService.getGlobalTracer()
                    .buildSpan(String.format("%s %s", request.getMethod(),
                            request.getRequestURI()))
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();

        openTracingService.getGlobalTracer().scopeManager().activate(
                activeSpan, true);

        logger.info("About to return a hello. {} {}", model, language);

        List<Language> languageList = populateLanguages();

        Language languageObject = null;

        if (language != null && !language.isEmpty()) {
            Optional<Language> languageOptional = languageList.stream().filter(
                    language1 -> language1.getShortName().equals(language)).findFirst();
            if (languageOptional.isPresent())
                languageObject = languageOptional.get();
        }

        if (languageObject == null) {
            languageObject = new Language();
            languageObject.setShortName("en");
        }
        model.addAttribute("helloWorld", getHelloWorld(languageObject));

        model.addAttribute("language", new HelloWorldRequest());
        model.addAttribute("allLanguages", languageList);

        logger.info("language", new Language());

        if (activeSpan != null) {
            activeSpan.finish();
        }

        return "hello";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404Exception(NoHandlerFoundException ex) {
        return "error";
    }

    @PostMapping("/hello")
    public String hello(@ModelAttribute HelloWorldRequest helloWorld, Model model, HttpServletRequest request) {
        logger.info("start an active span");
        Span activeSpan = openTracingService.getGlobalTracer()
                .buildSpan(String.format("%s %s", request.getMethod(),
                        request.getRequestURI()))
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();

        openTracingService.getGlobalTracer().scopeManager().activate(
                activeSpan, true);

        logger.info("About to create a hello " + helloWorld);

        String apiEndpoint = this.apiUrl + "/languages";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Accept", "application/json");
        httpHeaders.remove("Accept-Charset");

        Gson gson = new Gson();
        String helloWorldBody = gson.toJson(helloWorld);

        RestTemplate restTemplate = new RestTemplate();
        StringHttpMessageConverter converter = new StringHttpMessageConverter();
        converter.setWriteAcceptCharset(false);
        restTemplate.getMessageConverters().add(0, converter);

        logger.info("Make a request with " + helloWorldBody);
        ResponseEntity<String> response = null;
        Span childSpan = null;

        try {
            childSpan = injectChildSpan(
                    openTracingService.getGlobalTracer(), httpHeaders, apiEndpoint, "POST");
            HttpEntity<String> httpEntity = new HttpEntity <String> (helloWorldBody, httpHeaders);

            logger.info("Inject headers: " + httpHeaders);

            response = restTemplate.postForEntity(apiEndpoint, httpEntity, String.class);

        } catch (RestClientException rce) {
            rce.printStackTrace();

            if (childSpan != null) {
                childSpan.setTag(Tags.ERROR.getKey(), rce.getLocalizedMessage());
                childSpan.finish();
            }

            if (activeSpan != null) {
                activeSpan.setTag(Tags.ERROR.getKey(), rce.getLocalizedMessage());
                activeSpan.finish();
            }

            return "error";
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 201) {

            if (childSpan != null) {
                childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
                childSpan.setTag(Tags.ERROR.getKey(), response.getBody());
                childSpan.finish();
            }

            if (activeSpan != null) {
                activeSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
                activeSpan.setTag(Tags.ERROR.getKey(), response.getBody());
                activeSpan.finish();
            }
            return "error";
        }

        if (childSpan != null) {
            childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
            childSpan.finish();
        }


        List<Language> languageList = populateLanguages();

        Language languageObject = new Language();
        languageObject.setShortName("en");

        model.addAttribute("helloWorld", getHelloWorld(languageObject));

        model.addAttribute("language", new HelloWorldRequest());
        model.addAttribute("allLanguages", languageList);

        if (activeSpan != null) {
            activeSpan.finish();
        }

        return "hello";
    }

    private HelloWorld getHelloWorld(Language language) {
        logger.info("About to get hello world");

        String apiEndpoint = this.apiUrl + "/helloWorld/" + language.getShortName();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", "application/json");
        httpHeaders.remove("Accept-Charset");


        RestTemplate restTemplate = new RestTemplate();
        StringHttpMessageConverter converter = new StringHttpMessageConverter();
        converter.setWriteAcceptCharset(false);
        restTemplate.getMessageConverters().add(0, converter);

        ResponseEntity<String> response = null;
        Span childSpan = null;

        try {
            childSpan = injectChildSpan(
                    openTracingService.getGlobalTracer(), httpHeaders, apiEndpoint, "GET");
            HttpEntity<String> httpEntity = new HttpEntity <String> (httpHeaders);
            logger.info("Inject headers: " + httpHeaders);
            response = restTemplate.exchange(apiEndpoint, HttpMethod.GET, httpEntity, String.class);

        } catch (RestClientException rce) {
            rce.printStackTrace();

            if (childSpan != null) {
                childSpan.setTag(Tags.ERROR.getKey(), rce.getLocalizedMessage());
                childSpan.finish();
            }
            return new HelloWorld();
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 200) {
            if (childSpan != null) {
                childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
                childSpan.setTag(Tags.ERROR.getKey(), response.getBody());
                childSpan.finish();
            }
            return new HelloWorld();
        }

        JSONObject jsonObject = new JSONObject(response.getBody());

        if (childSpan != null) {
            childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
            childSpan.finish();
        }

        if (jsonObject.has("text")) {
            return new HelloWorld(language, jsonObject.getString("text"));
        } else {
            return new HelloWorld();
        }
    }

    private List<Language> populateLanguages() {
        logger.info("About to populate languages");

        String apiEndpoint = this.apiUrl + "/languages";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", "application/json");
        httpHeaders.remove("Accept-Charset");

        logger.info("Inject headers: " + httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        StringHttpMessageConverter converter = new StringHttpMessageConverter();
        converter.setWriteAcceptCharset(false);
        restTemplate.getMessageConverters().add(0, converter);

        ResponseEntity<String> response = null;
        Span childSpan = null;

        try {
            logger.info("Inject tracing headers");
            childSpan = injectChildSpan(
                    openTracingService.getGlobalTracer(), httpHeaders, apiEndpoint, "GET");

            HttpEntity<String> httpEntity = new HttpEntity <String> (httpHeaders);
            logger.info("Inject headers: " + httpHeaders);
            response = restTemplate.exchange(apiEndpoint, HttpMethod.GET, httpEntity, String.class);

        } catch (RestClientException rce) {
            rce.printStackTrace();

            if (childSpan != null) {
                childSpan.setTag(Tags.ERROR.getKey(), rce.getLocalizedMessage());
                childSpan.finish();
            }
            return Collections.emptyList();
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 200) {
            if (childSpan != null) {
                childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
                childSpan.setTag(Tags.ERROR.getKey(), response.getBody());
                childSpan.finish();
            }
            return Collections.emptyList();
        }

        JSONArray jsonArray = new JSONArray(response.getBody());
        List<Language> languageList = new ArrayList<>();


        for (Object language: jsonArray) {
            JSONObject languageJsonObject = (JSONObject)language;
            languageList.add(new Language(
                    languageJsonObject.getString("long"),
                    languageJsonObject.getString("short")
            ));
        }

        if (childSpan != null) {
            childSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatusCodeValue());
            childSpan.finish();
        }

        return languageList;
    }

    private Span injectChildSpan(Tracer tracer, HttpHeaders httpHeaders, String endpoint, String method) {
        Span currentActiveSpan = tracer.activeSpan();

        Tracer.SpanBuilder clientSpanBuilder = tracer.buildSpan(
                String.format("%s %s", method, endpoint));
        if (currentActiveSpan != null) {
            clientSpanBuilder.asChildOf(currentActiveSpan);
        }

        Span clientSpan = clientSpanBuilder.start();

        Tags.SPAN_KIND.set(clientSpan, Tags.SPAN_KIND_CLIENT);
        Tags.HTTP_URL.set(clientSpan, endpoint);

        openTracingService.getGlobalTracer().inject(
                clientSpan.context(), Format.Builtin.HTTP_HEADERS,
                new HttpHeaderInjector(httpHeaders));

        return clientSpan;
    }
}
