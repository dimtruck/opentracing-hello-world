/*
 * CI/CD Dashboard API
 *
 * API for CI/CD Dashboard.
 *
 * API version: 1.0.0
 * Contact: dimitry.ushakov@rackspace.com
 * Generated by: Swagger Codegen (https://github.com/swagger-api/swagger-codegen.git)
 */

package main

import (
	"log"
	"net/http"

	jaegercfg "github.com/uber/jaeger-client-go/config"
	jaegerlog "github.com/uber/jaeger-client-go/log"

	jaeger "github.com/uber/jaeger-client-go"
)

func main() {
	log.Printf("Server started")

	cfg := jaegercfg.Configuration{
		Sampler: &jaegercfg.SamplerConfig{
			Type:  jaeger.SamplerTypeConst,
			Param: 1,
		},
		Reporter: &jaegercfg.ReporterConfig{
			LogSpans:           true,
			LocalAgentHostPort: "jaeger:5775",
		},
	}

	jLogger := jaegerlog.StdLogger

	// Initialize tracer with a logger and a metrics factory
	closer, err := cfg.InitGlobalTracer(
		"api",
		jaegercfg.Logger(jLogger),
	)
	if err != nil {
		log.Printf("Could not initialize jaeger tracer: %s", err.Error())
		return
	}
	defer closer.Close()

	router := NewRouter()

	log.Fatal(http.ListenAndServe(":8080", router))
}
