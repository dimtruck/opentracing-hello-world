FROM python:3

COPY . /opt

WORKDIR /opt

RUN pip install -r requirements.txt

CMD gunicorn -b 0.0.0.0:8000 app:api
