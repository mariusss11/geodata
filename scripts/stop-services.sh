#!/bin/bash

set -e  # Exit on any error

echo "Stopping Kubernetes Deployments (scaling to 0 replicas)..."

for svc in client-service identity-service item-service review-service frontend; do
  if kubectl get deployment "$svc" >/dev/null 2>&1; then
    echo "🔻 Scaling down $svc to 0 replicas..."
    kubectl scale deployment "$svc" --replicas=0
  else
    echo "⚠️ Deployment $svc not found. Skipping."
  fi
done

echo -e "\n✅ All specified deployments have been stopped (if they exist)."

echo -e "\n📦 Current status of deployments:"
kubectl get deployments

echo -e "\n📦 Current status of pods:"
kubectl get pods -o wide
