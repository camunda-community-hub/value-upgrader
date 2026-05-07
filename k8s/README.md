# Introduction
This directory contains a help deployment to deploy Upgrader value.



# Deployment
Check the deployment file, and adapt it.

# Operation

```shell
kubectl create -f upgrader-value.yaml
```

Forward the Upgrader value access 

```shell
kubectl port-forward svc/upgrader_value 8080:8080 -n camunda
```

# Remove Upgrader

```shell
kubectl delete -f upgrader-value.yaml
```



