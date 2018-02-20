package main

import (
	"fmt"

	"database/sql"

	_ "github.com/lib/pq"
	opentracing "github.com/opentracing/opentracing-go"
	ext "github.com/opentracing/opentracing-go/ext"
)

func getDB() *sql.DB {
	dbinfo := fmt.Sprintf("user=%s dbname=%s host=%s sslmode=disable",
		DB_USER, DB_NAME, DB_HOST)
	db, err := sql.Open("postgres", dbinfo)
	checkErr(err)
	return db

}

func GetAllLanguages(span opentracing.Span) []Language {
	childSpan := opentracing.StartSpan(
		"get all languages",
		opentracing.ChildOf(span.Context()))
	ext.DBType.Set(childSpan, "postgres")

	defer childSpan.Finish()

	db := getDB()
	defer db.Close()

	fmt.Println("# Querying")
	rows, err := db.Query("SELECT abbreviation, full_name FROM languages")
	checkErr(err)

	var result []Language
	for rows.Next() {
		var abbreviation string
		var full_name string
		err = rows.Scan(&abbreviation, &full_name)
		checkErr(err)

		result = append(result, Language{
			LongName:  full_name,
			ShortName: abbreviation,
		})
	}

	return result
}

func GetTranslationByLanguage(language string, span opentracing.Span) string {
	childSpan := opentracing.StartSpan(
		"get translation by language",
		opentracing.ChildOf(span.Context()))
	ext.DBType.Set(childSpan, "postgres")
	defer childSpan.Finish()

	db := getDB()
	defer db.Close()

	fmt.Println("# Querying")
	rows, err := db.Query(
		"SELECT translation FROM helloWorld h, languages l WHERE lang_id = l.id AND l.abbreviation = $1", language)
	checkErr(err)

	var translation string
	for rows.Next() {
		err = rows.Scan(&translation)
		checkErr(err)
	}

	return translation
}

func checkErr(err error) {
	if err != nil {
		panic(err)
	}
}
