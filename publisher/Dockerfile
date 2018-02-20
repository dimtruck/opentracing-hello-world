FROM python:2

COPY . /opt

WORKDIR /opt

RUN pip install -r requirements.txt

CMD gunicorn -c gunicorn.py app:api
