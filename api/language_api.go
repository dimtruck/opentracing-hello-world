package main

import (
	"encoding/json"
	"net/http"

	log "github.com/sirupsen/logrus"
)

func AddLanguage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
}

func GetLanguages(w http.ResponseWriter, r *http.Request) {
	log.Debug("Get languages")

	languages := []Language{
		Language{
			Id:        1,
			ShortName: "en",
			LongName:  "English",
		}
	}

	languagesJSON, err := json.Marshal(languages)

	if err != nil {
		log.WithField("languages", languagesJSON).Error("Unable to generate languages result")
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.Write(languagesJSON)
	}
}

func UpdateApp(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
}
