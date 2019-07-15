#!/bin/bash

set -e

# CLUSTER_IP=$(minikube ip)
CLUSTER_IP=$(kubectl get node $(kind get nodes) -o jsonpath='{.status.addresses[0].address}')

# setup rbac rules for tiller, don't do it in production like this.
kubectl apply -f - <<EOF                                                                                                                                                                 
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding                                                                                                                                                                                                                                                                                                       
metadata:                                                                                                                                                                                                                                                                                                                      
  name: tiller-rbac
roleRef:
  apiGroup: rbac.authorization.k8s.io                                                                                                                                                                 
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: default
  namespace: kube-system
EOF

helm init --wait                                                                                                                                                                 
helm repo update
helm upgrade dev-ingress stable/traefik --install --set serviceType=NodePort \
--set deployment.hostPort.httpEnabled=true \
--set deployment.hostPort.httpsEnabled=true \
--set deployment.hostPort.dashboardEnabled=true \
--set rbac.enabled=true
