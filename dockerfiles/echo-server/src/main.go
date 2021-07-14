package main

import (
	"github.com/gin-gonic/gin"
	"io/ioutil"
	"net/http"
)

func EchoRequest(c *gin.Context) {
	if c.Request.Body == nil {
		c.Data(http.StatusOK, c.Request.Header.Get("content-type"), nil)
	}
	requestBody, _ := ioutil.ReadAll(c.Request.Body)
	c.Data(http.StatusOK, c.Request.Header.Get("content-type"), requestBody)
}

func main() {
	r := gin.Default()
	r.GET("/api/echo", EchoRequest)
	r.POST("/api/echo", EchoRequest)
	r.Run()
}
