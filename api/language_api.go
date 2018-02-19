package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	log "github.com/sirupsen/logrus"
)

type NewTranslationRequest struct {
	LongName    string `json:"longName"`
	ShortName   string `json:"shortName"`
	Translation string `json:"translation"`
}

func AddLanguage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")

	// call a pub sub python thingie
	body, err := ioutil.ReadAll(r.Body)

	if err != nil {
		log.WithField("request", err).Error("Unable to parse body")
		w.WriteHeader(http.StatusBadRequest)
	} else {
		var newTranslationRequest NewTranslationRequest

		err = json.Unmarshal(body, &newTranslationRequest)

		if err != nil {
			log.WithField("request", err).Error("Unable to unmarshall body")
			w.WriteHeader(http.StatusBadRequest)
		}

		log.WithField("request", body).Info("Send a new request ")

		http.Post(
			fmt.Sprintf("%s/publish", PUBLISHER_ENDPOINT),
			"application/json", bytes.NewBuffer(body))
		w.WriteHeader(http.StatusCreated)
	}

}

func GetLanguages(w http.ResponseWriter, r *http.Request) {
	log.Debug("Get languages")

	languages := GetAllLanguages()

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
