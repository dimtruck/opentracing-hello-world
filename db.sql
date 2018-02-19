--
-- PostgreSQL database dump
--

-- Dumped from database version 10.2 (Debian 10.2-1.pgdg90+1)
-- Dumped by pg_dump version 10.2 (Debian 10.2-1.pgdg90+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

CREATE TABLE IF NOT EXISTS languages (
    id serial PRIMARY KEY, 
    abbreviation varchar NOT NULL,
    full_name varchar NOT NULL);

ALTER TABLE languages OWNER TO helloworld;

CREATE TABLE IF NOT EXISTS helloWorld (
    id serial PRIMARY KEY, 
    translation varchar NOT NULL,
    lang_id integer REFERENCES languages (id)
);

ALTER TABLE helloWorld OWNER TO helloworld;

INSERT INTO languages (abbreviation, full_name) VALUES ('en', 'English');

INSERT INTO helloWorld (translation, lang_id) VALUES ('Hello World', 1);