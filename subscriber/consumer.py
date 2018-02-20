#!/usr/bin/env python
import pika
import time
import logging
import psycopg2
import json
import redis

import tracer
import opentracing
from opentracing.ext import tags as ext_tags


logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)
LOGGER = logging.getLogger(__name__)
LOGGER.setLevel(logging.INFO)


def get_postgres_connection():
    try:
        conn = psycopg2.connect("dbname=helloworld_db user=helloworld host=postgres")
        return conn.cursor(), conn
    except psycopg2.OperationalError as e:
        # postgres is not up yet.  Wait a second and try again
        time.sleep(1)
        return get_postgres_connection()


def postgres_close(cursor, conn):
    cursor.close()
    conn.close()


def get_redis_connection():
    return redis.StrictRedis(host='redis', port=6379, db=0)


def consume(postgres_cursor):
    LOGGER.info("about to consume")
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='rabbitmq'))
    channel = connection.channel()

    channel.queue_declare(queue='hello')

    def callback(ch, method, properties, body):
        LOGGER.info(" [x] Received %r" % body)
        LOGGER.info(" [x] Received %r" % method)
        LOGGER.info(" [x] Received %r" % properties)
        # TODO: save to postgres and save in redis
        json_body = None

        try:
            json_body = json.loads(body.decode('utf8'))
        except Exception as e:
            LOGGER.exception(e)
            try:
                json_body = json.loads(body.decode('latin-1'))
            except Exception as e:
                LOGGER.exception(e)
                try:
                    json_body = json.loads(body.decode('cp1252'))
                except Exception as e:
                    LOGGER.exception(e)
        LOGGER.info(json_body)

        extracted_ctx = opentracing.tracer.extract(
            format=opentracing.Format.TEXT_MAP,
            carrier=json_body)

        LOGGER.info(" [x] Received %r" % extracted_ctx)

        span = None

        if extracted_ctx is not None:
            span = opentracing.tracer.start_span(
                operation_name="consumer",
                references=[opentracing.follows_from(extracted_ctx)],
                tags={ext_tags.SPAN_KIND: "RabbitMQ Consumer"}
            )
        else:
            span = opentracing.tracer.start_span(
                operation_name="consumer",
                tags={ext_tags.SPAN_KIND: "RabbitMQ Consumer"}
            )

        try:
            with opentracing.tracer.start_span(
                operation_name="insert into languages",
                child_of=span.context,
                tags={
                    ext_tags.SPAN_KIND: "Postgres",
                    ext_tags.COMPONENT: "RabbitMQ Consumer"
                    }):               
                postgres_cursor.execute(
                    "INSERT INTO languages (abbreviation, full_name) VALUES (%s, %s) RETURNING id;",
                    (json_body["shortName"], json_body["longName"],))
                new_lang_id = postgres_cursor.fetchone()[0]

            with opentracing.tracer.start_span(
                operation_name="insert into helloworld",
                child_of=span.context,
                tags={
                    ext_tags.SPAN_KIND: "Postgres",
                    ext_tags.COMPONENT: "RabbitMQ Consumer"
                    }):
                postgres_cursor.execute(
                    "INSERT INTO helloworld (translation, lang_id) VALUES (%s, %s);",
                    (json_body["translation"], new_lang_id,))
                postgres_conn.commit()

            with opentracing.tracer.start_span(
                operation_name="insert into redis cache",
                child_of=span.context,
                tags={
                    ext_tags.SPAN_KIND: "Redis",
                    ext_tags.COMPONENT: "RabbitMQ Consumer"
                    }):
                redis_conn = get_redis_connection()
                redis_conn.set(json_body["shortName"], json_body["translation"])
                redis_conn.expire(json_body["shortName"], 30)
        finally:
            span.finish()

    channel.basic_consume(callback,
                        queue='hello',
                        no_ack=True)

    return channel


def start_consuming(postgres_cursor):
    try:
        channel = consume(postgres_cursor)
        LOGGER.info(' [*] Waiting for messages. To exit press CTRL+C')
        channel.start_consuming()
    except Exception as e:
        # we are not ready!  let's wait 5 seconds
        LOGGER.exception(e)
        LOGGER.error(e)
        LOGGER.warning("WE WAIT")
        time.sleep(5)
        start_consuming(postgres_cursor)


if __name__ == '__main__':
    LOGGER.warning("Start consuming")
    tracer = tracer.instrument_tracer('jaeger')
    LOGGER.info(tracer)
    LOGGER.info(opentracing.tracer)
    postgres_cursor, postgres_conn = get_postgres_connection()
    start_consuming(postgres_cursor)

