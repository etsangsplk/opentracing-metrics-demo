package main

import (
	"context"
	"flag"
	"io/ioutil"
	"log"
	"math/rand"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/opentracing-contrib/go-stdlib/nethttp"
	opentracing "github.com/opentracing/opentracing-go"
	jaeger "github.com/uber/jaeger-client-go/config"
)

var name = flag.String("name", "", "Name of the client")
var failPct = flag.Float64("fail", 0, "Fail a given percentage of requests (0.0-1.0)")
var stop = make(chan struct{})

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())

	if *name == "" {
		log.Fatal("Missing required argument -name")
	}

	closer, err := jaeger.Configuration{
		Sampler: &jaeger.SamplerConfig{Type: "const", Param: 1},
	}.InitGlobalTracer(*name)
	if err != nil {
		log.Fatalf("Failed to initialize Jaeger tracer %s", err)
	}
	defer closer.Close()

	var serverChannel = make(chan os.Signal, 0)
	signal.Notify(serverChannel, os.Interrupt, syscall.SIGTERM)

	go runClient(opentracing.GlobalTracer())

	select {
	case <-serverChannel:
		log.Print("client is exiting")
		stop <- struct{}{}
	}
}

func runClient(tracer opentracing.Tracer) {
	client := &http.Client{Transport: &nethttp.Transport{}}

	t := time.NewTicker(100 * time.Millisecond)

	for {
		select {
		case <-t.C:
			// use function for extra scope
			func() {
				span, ctx := opentracing.StartSpanFromContext(context.Background(), "client")
				span.SetBaggageItem("callpath", *name)
				defer span.Finish()
				if rand.Float64() < *failPct {
					span.SetBaggageItem("fail", "true")
				}

				req, err := http.NewRequest("GET", "http://localhost:8080/ping", nil)
				if err != nil {
					log.Fatalf("bad request %s", err)
				}
				req = req.WithContext(ctx)

				req, ht := nethttp.TraceRequest(tracer, req)
				defer ht.Finish()

				res, err := client.Do(req)
				if err != nil {
					log.Printf("failed request %s", err)
				}
				out, _ := ioutil.ReadAll(res.Body)
				log.Printf("received response %d %s", res.StatusCode, string(out))
			}()
		case <-stop:
			return
		}
	}
}
