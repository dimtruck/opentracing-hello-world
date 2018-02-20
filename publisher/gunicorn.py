bind = '0.0.0.0:8000'

def post_fork(server, worker):
    from jaeger_client import Config

    # defaulting to jaeger but would read from a config file
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
        service_name='publisher',
    )
    return config.initialize_tracer() 
