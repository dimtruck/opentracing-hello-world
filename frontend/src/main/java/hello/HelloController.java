package hello;

import com.google.gson.Gson;
import domains.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.*;


@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(
            HelloController.class);


    @Value("${api.url}")
    private String apiUrl;


    @GetMapping("/hello")
    public String hello(Model model, @RequestParam(required = false) String language) {
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
        return "hello";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404Exception(NoHandlerFoundException ex) {
        return "error";
    }

    @PostMapping("/hello")
    public String hello(@ModelAttribute HelloWorldRequest helloWorld, Model model) {
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


        List<Language> languageList = populateLanguages();

        Language languageObject = new Language();
        languageObject.setShortName("en");

        model.addAttribute("helloWorld", getHelloWorld(languageObject));

        model.addAttribute("language", new HelloWorldRequest());
        model.addAttribute("allLanguages", languageList);


        return "hello";
    }

    private HelloWorld getHelloWorld(Language language) {
        logger.info("About to get hello world");

        String apiEndpoint = this.apiUrl + "/helloWorld/" + language.getShortName();

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

            return new HelloWorld();
        }

        logger.info("RESULT: " + response.getStatusCodeValue() + " " + response.getBody());

        if (response.getStatusCodeValue() != 200) {
            return new HelloWorld();
        }

        JSONObject jsonObject = new JSONObject(response.getBody());

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

        JSONArray jsonArray = new JSONArray(response.getBody());
        List<Language> languageList = new ArrayList<>();


        for (Object language: jsonArray) {
            JSONObject languageJsonObject = (JSONObject)language;
            languageList.add(new Language(
                    languageJsonObject.getString("long"),
                    languageJsonObject.getString("short")
            ));
        }

        return languageList;
    }

    
}
