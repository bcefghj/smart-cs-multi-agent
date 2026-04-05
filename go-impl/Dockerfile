FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /smart-cs-agent .

FROM alpine:3.19
COPY --from=builder /smart-cs-agent /smart-cs-agent
EXPOSE 8090
ENTRYPOINT ["/smart-cs-agent"]
