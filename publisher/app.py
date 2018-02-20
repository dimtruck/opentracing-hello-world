import json
import logging
import os
import pika

import falcon

import tracer
import opentracing
from opentracing.ext import tags as ext_tags


logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)
LOGGER = logging.getLogger(__name__)
LOGGER.setLevel(logging.INFO)

api = appplication = falcon.API()
api.req_options.auto_parse_form_urlencoded = False


class Publish(object):
    '''
    API entrypoint for /publish.
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_post(self, req, resp):
        '''
        Add a language
        '''
        extracted_ctx = opentracing.tracer.extract(
            format=opentracing.Format.HTTP_HEADERS,
            carrier=req.headers)

        self.logger.info(" [x] Received %r" % extracted_ctx)

        span = None

        if extracted_ctx is not None:
            span = opentracing.tracer.start_span(
                operation_name="publisher",
                child_of=extracted_ctx,
                tags={ext_tags.SPAN_KIND: "API endpoint for publisher"}
            )
        else:
            span = opentracing.tracer.start_span(
                operation_name="publisher",
                tags={ext_tags.SPAN_KIND: "API endpoint for publisher"}
            )

        body = req.stream.read()
        if body is None or len(body) == 0:
            raise falcon.HTTPBadRequest('no data')
        self.logger.info("request to start run: {}".format(body))

        json_body = None
        encoding = 'utf8'

        try:
            json_body = json.loads(body.decode('utf8'))
        except Exception as e:
            LOGGER.error(e)
            try:
                json_body = json.loads(body.decode('latin-1'))
                encoding = 'latin-1'
            except Exception as e:
                try:
                    json_body = json.loads(body.decode('cp1252'))
                    encoding = 'cp1252'
                except Exception as e:
                    LOGGER.error(e)
        LOGGER.info(json_body)

        connection = pika.BlockingConnection(pika.ConnectionParameters(host='rabbitmq'))
        channel = connection.channel()

        channel.queue_declare(queue='hello')

        with opentracing.tracer.start_span(
            operation_name="publish event",
            child_of=span.context,
            tags={
                ext_tags.SPAN_KIND: "RabbitMQ Publisher",
                ext_tags.COMPONENT: "RabbitMQ Publisher"
                }) as child_span: 

            opentracing.tracer.inject(
                span_context=child_span,
                format=opentracing.Format.TEXT_MAP,
                carrier=json_body)
                          
            channel.basic_publish(exchange='',
                                routing_key='hello',
                                body=json.dumps(json_body, ensure_ascii=False).encode(encoding=encoding))
            self.logger.info(" [x] Sent {}".format(body))
        connection.close()

        span.finish()

        resp.status = falcon.HTTP_201


RESOURCES = {
    '/publish': Publish(LOGGER),
}

for route, resource in RESOURCES.items():
    api.add_route(route, resource)
