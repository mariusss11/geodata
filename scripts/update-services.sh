#!/bin/bash

set -e  # Exit on any error

echo "Applying Kubernetes YAML files..."

kubectl apply -f k8s/client-service.yaml
kubectl apply -f k8s/identity-service.yaml
kubectl apply -f k8s/borrow-service.yaml
kubectl apply -f k8s/item-service.yaml
kubectl apply -f k8s/review-service.yaml
kubectl apply -f k8s/frontend.yaml

echo -e "Restarting deployments if they exist..."

for svc in client-service identity-service item-service review-service borrow-service frontend; do
  if kubectl get deployment "$svc" >/dev/null 2>&1; then
    echo "⏳ Rolling out restart for: $svc"
    kubectl rollout restart deployment/"$svc"
    kubectl rollout status deployment/"$svc"
  else
    echo "⚠️ Deployment $svc not found. Skipping."
  fi
done

echo -e "Current status of pods:"
kubectl get pods -o wide

echo -e "Current status of services:"
kubectl get services
