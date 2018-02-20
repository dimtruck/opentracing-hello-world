from jaeger_client import Config


def instrument_tracer(tracer_name):

    if tracer_name == 'jaeger':
        config = Config(
            config={
                'enabled': True,
                'sampler': {
                    'type': 'const',
                    'param': 1,
                },
                'logging': True,
                'reporter_batch_size': 1000,
                'reporter_queue_size': 1000,
                'local_agent': {
                    'reporting_host': 'jaeger',
                    'reporting_port': '5775',
                    }
            },  
            service_name='consumer',
        )
        return config.initialize_tracer() 
    else:
        raise Exception("invalid tracer name")


