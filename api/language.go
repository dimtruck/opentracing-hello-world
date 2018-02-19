package main

type Language struct {
	Id int64 `json:"id,omitempty"`

	LongName string `json:"long"`

	ShortName string `json:"short"`
}
