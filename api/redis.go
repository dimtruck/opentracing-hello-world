package main

import (
	"github.com/go-redis/redis"
	log "github.com/sirupsen/logrus"
)

// NewRedisClient - returns new instance of redis client
func NewRedisClient() *redis.Client {
	client := redis.NewClient(&redis.Options{
		Addr:     "redis:6379",
		Password: "",
		DB:       0,
	})

	_, err := client.Ping().Result()

	if err != nil {
		log.WithField("error", err).Error("Unable to start redis")
		panic(err)
	} else {
		return client
	}
}
