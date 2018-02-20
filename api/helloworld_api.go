package main

import (
	"encoding/json"
	"net/http"

	opentracing "github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	log "github.com/sirupsen/logrus"

	"github.com/gorilla/mux"

	_ "github.com/lib/pq"
)

func GetHelloWorld(w http.ResponseWriter, r *http.Request) {
	log.Debug("Get hello world")

	var serverSpan opentracing.Span
	appSpecificOperationName := "Get Hello World"
	wireContext, err := opentracing.GlobalTracer().Extract(
		opentracing.HTTPHeaders,
		opentracing.HTTPHeadersCarrier(r.Header))
	if err != nil {
		log.WithField("request", err).Error("Unable to extract span context")
	}

	serverSpan = opentracing.StartSpan(
		appSpecificOperationName,
		ext.RPCServerOption(wireContext))

	defer serverSpan.Finish()

	vars := mux.Vars(r)
	language := vars["language"]
	var response *HelloWorld

	// TODO: translate based on language.  Get translation from postgres
	// TODO: set language translation in redis.  set expire to 10 seconds

	childSpan := opentracing.StartSpan(
		"get from redis",
		opentracing.ChildOf(serverSpan.Context()))

	ext.DBType.Set(childSpan, "redis")

	redisClient := NewRedisClient()

	cachedTranslation, cacheErr := redisClient.Get(language).Result()

	childSpan.Finish()

	if cacheErr != nil {
		log.WithField("cacheErr", cachedTranslation).Error("Unable to get cached translation")
	}

	if cachedTranslation != "" {
		response = &HelloWorld{
			Text: cachedTranslation,
		}
	} else {
		text := GetTranslationByLanguage(language, serverSpan)

		if text != "" {
			response = &HelloWorld{
				Text: text,
			}
		} else {
			response = &HelloWorld{
				Text: "hello world",
			}
		}
	}

	responseJSON, err := json.Marshal(*response)

	if err != nil {
		log.WithField("response", responseJSON).Error("Unable to generate response result")
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.Write(responseJSON)
	}
}
