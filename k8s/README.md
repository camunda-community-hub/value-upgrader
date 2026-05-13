# Introduction
This directory contains a help deployment to deploy Upgrader value.



# Deployment
Check the deployment file, and adapt it.

# Operation

```shell
kubectl create -f value-upgrader.yaml
```

Forward the Upgrader value access 

```shell
kubectl port-forward svc/upgrader_value 8080:8080 -n camunda
```

# Remove Upgrader

```shell
kubectl delete -f value-upgrader.yaml
```


# Docker compose

```shell
docker compose -f docker-compose-value-upgrader.yaml up -d

```


