package hello;

import com.google.gson.Gson;
import domains.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(
            HelloController.class);


    @Value("${api.url}")
    private String apiUrl;


    @GetMapping("/hello")
    public String hello(Model model) {
        logger.info("About to return a hello");
        model.addAttribute("hello", new HelloWorld());
        return "hello";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404Exception(NoHandlerFoundException ex) {
        return "error";
    }

    @PostMapping("/hello")
    public String hello(@ModelAttribute HelloWorld helloWorld, Model model) {
        logger.info("About to create a hello " + helloWorld);

        String apiEndpoint = this.apiUrl + "/languages";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Accept", "application/json");
        httpHeaders.remove("Accept-Charset");

        logger.info("Inject headers: " + httpHeaders);

        Gson gson = new Gson();
        String helloWorldBody = gson.toJson(helloWorld);

        HttpEntity<String> httpEntity = new HttpEntity <String> (helloWorldBody, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        StringHttpMessageConverter converter = new StringHttpMessageConverter();
        converter.setWriteAcceptCharset(false);
        restTemplate.getMessageConverters().add(0, converter);

        logger.info("Make a request with " + helloWorldBody);
        ResponseEntity<String> response = null;

        try {
            response = restTemplate.postForEntity(apiEndpoint, httpEntity, String.class);

        } catch (RestClientException rce) {
            rce.printStackTrace();

            return "error";
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 201) {
            return "error";
        }

        return "result";
    }

    @ModelAttribute("allLanguages")
    public List<Language> populateLanguages() {
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

        try {
            response = restTemplate.getForEntity(apiEndpoint, String.class);

        } catch (RestClientException rce) {
            rce.printStackTrace();

            return Collections.emptyList();
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 200) {
            return Collections.emptyList();
        }

        JSONObject jsonObject = new JSONObject(response.getBody());
        List<Language> languageList = new ArrayList<>();


        for (Object language: languageList) {
            JSONObject languageJsonObject = (JSONObject)language;
            languageList.add(new Language(
                    languageJsonObject.getString("long"),
                    languageJsonObject.getString("short")
            ));
        }

        return languageList;
    }

    
}
