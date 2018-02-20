package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	log "github.com/sirupsen/logrus"

	opentracing "github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
)

type NewTranslationRequest struct {
	LongName    string `json:"longName"`
	ShortName   string `json:"shortName"`
	Translation string `json:"translation"`
}

func AddLanguage(w http.ResponseWriter, r *http.Request) {
	var serverSpan opentracing.Span
	appSpecificOperationName := "Add Language"
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

		childSpan := opentracing.StartSpan(
			"add language",
			opentracing.ChildOf(serverSpan.Context()))
		ext.SpanKindRPCClient.Set(childSpan)
		defer childSpan.Finish()

		httpClient := &http.Client{}
		httpReq, _ := http.NewRequest(
			"POST", fmt.Sprintf("%s/publish", PUBLISHER_ENDPOINT), bytes.NewBuffer(body))

		opentracing.GlobalTracer().Inject(
			childSpan.Context(),
			opentracing.HTTPHeaders,
			opentracing.HTTPHeadersCarrier(httpReq.Header))

		_, err = httpClient.Do(httpReq)

		if err != nil {
			log.WithField("request", err).Error("Unable to add language")
			w.WriteHeader(http.StatusInternalServerError)
		} else {
			w.WriteHeader(http.StatusCreated)
		}
	}

}

func GetLanguages(w http.ResponseWriter, r *http.Request) {
	log.Debug("Get languages")

	var serverSpan opentracing.Span
	appSpecificOperationName := "Get Languages"
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

	languages := GetAllLanguages(serverSpan)

	languagesJSON, err := json.Marshal(languages)

	if err != nil {
		log.WithField("languages", languagesJSON).Error("Unable to generate languages result")
		w.WriteHeader(http.StatusInternalServerError)
	} else {
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.Write(languagesJSON)
	}
}
