# Twitch Auth Gateway

This project is a API Gateway which is designed to add authentication headers for your requests to Twitch API.

## Description

This _API Gateway_ will be useful if:

* if you are writing a simple script and don't want to bother with authorization in Twitch,
* you have a microservice architecture and want to store authorization tokens for Twitch in a separate service.

You can raise this service and make requests indirectly to the API of this service, it will add authorization headers
and proxy your request to the Twitch API.

## Usage

The easiest way to use this service is docker.

```shell
docker run -itd --rm --name twitch-auth -p 3000:80 ghcr.io/razornd/twitch-auth-gateway:1.0.0-SNAPSHOT \
  --twitch.registration.client-id=$CLIENT_ID \
  --twitch.registration.client-secret=$CLIENT_SECRET

```

Where `$CLIENT_ID` and `$CLIENT_SECRET` are the data of your application registered in
the [Twitch Dev Console](https://dev.twitch.tv/console/apps).

After running the docker container you can make requests to `localhost:3000` as if you were making these requests to the
Twitch API. Example:

![demo](docs/demo.gif)
