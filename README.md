# OpenTracing and Metrics

```
sum(rate(span_count{operation="pong"}[1m])) by (error)
sum(rate(span_count{operation="pong"}[1m])) by (error, callpath)
```