# Docker

## Steps to run the application on a local Docker Swarm

Needs to be done once to setup Docker Swarm:

```bash
# Init Docker Swarm locally
$ docker swarm init
# Create web network to be used by traefik
$ docker network create -d overlay web
```

Full flow when starting from scratch:

```bash
# Go to Docker directory
$ cd docker
# Prepare docker environment
$ ./prepare.sh
# Build the container(s)
$ ./build.sh
```

## Local build without docker compose

In some cases it can be useful to build and run without docker compose and/or the shell scripts.
In that case, build can be done using:

```bash
# In root folder (can be run from other locations as well, but dockerfile location and docker context paths have to be changed in that case.
$ docker buildx build -t aerius-file-server:latest -f docker/service/Dockerfile .
```

Then to run the container, the following can be used:

```bash
# Can be run from anywhere, as long as the container has been build.
# To use a specific network so the service can be referenced from other containers, add something like --net local-testing-network --name aerius-fileserver
$ docker run --rm -p 8083:8083 -e spring_profiles_active=local aerius-file-server:latest
```
