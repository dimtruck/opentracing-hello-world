package main

import (
	"encoding/json"
	"net/http"

	log "github.com/sirupsen/logrus"

	"github.com/gorilla/mux"

	_ "github.com/lib/pq"
)

func GetHelloWorld(w http.ResponseWriter, r *http.Request) {
	log.Debug("Get hello world")
	vars := mux.Vars(r)
	language := vars["language"]
	var response *HelloWorld

	// TODO: translate based on language.  Get translation from postgres
	// TODO: set language translation in redis.  set expire to 10 seconds

	redisClient := NewRedisClient()

	cachedTranslation, cacheErr := redisClient.Get(language).Result()

	if cacheErr != nil {
		log.WithField("cacheErr", cachedTranslation).Error("Unable to get cached translation")
	}

	if cachedTranslation != "" {
		response = &HelloWorld{
			Text: cachedTranslation,
		}
	} else {
		text := GetTranslationByLanguage(language)

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
