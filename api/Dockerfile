FROM golang:alpine

RUN apk update && apk add git && rm -rf /var/cache/apk/*

RUN go get -u github.com/golang/dep/cmd/dep

COPY . /go/src/github.com/dimtruck/opentracing-hello-world
WORKDIR /go/src/github.com/dimtruck/opentracing-hello-world

RUN dep init && dep ensure

RUN go build

CMD ./api

