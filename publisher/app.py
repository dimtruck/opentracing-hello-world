import json
import logging
import os
import pika

import falcon

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
        body = req.stream.read()
        if body is None or len(body) == 0:
            raise falcon.HTTPBadRequest('no data')
        self.logger.info("request to start run: {}".format(body))

        connection = pika.BlockingConnection(pika.ConnectionParameters(host='rabbitmq'))
        channel = connection.channel()

        channel.queue_declare(queue='hello')

        channel.basic_publish(exchange='',
                            routing_key='hello',
                            body=body)
        self.logger.info(" [x] Sent {}".format(body))
        connection.close()

        resp.status = falcon.HTTP_201


RESOURCES = {
    '/publish': Publish(LOGGER),
}

for route, resource in RESOURCES.items():
    api.add_route(route, resource)