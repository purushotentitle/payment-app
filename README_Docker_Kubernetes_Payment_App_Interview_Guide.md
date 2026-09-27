# Docker + Kubernetes Interview Learning Guide
## Payment App Lab — Learn by Understanding, Not Memorizing

This guide uses one simple mental model:

```text
Java Payment App
        ↓
    Docker Image
        ↓
    Container
        ↓
    Kubernetes
        ↓
    Deployment
        ↓
    ReplicaSet
        ↓
       Pods
        ↓
      Service
        ↓
   Payment API
```

Around it:

```text
Git → Jenkins → Build → Docker Image → Kubernetes → Payment App

Kafka = async communication
Prometheus = metrics
Grafana = dashboards
```

---

# 1. Docker — Remember Only 5 Questions

Do not memorize Docker commands. Remember these questions:

```text
1. What images do I have?
2. What containers are running?
3. Why did a container fail?
4. How do I enter the container?
5. How do I start, stop, or remove it?
```

Commands:

```bash
docker images
docker ps
docker logs <container>
docker exec -it <container> bash
docker stop <container>
docker start <container>
docker rm <container>
```

## Interview Memory

```text
Image = Blueprint
Container = Running instance
```

Easy analogy:

```text
Java Class = Docker Image
Java Object = Docker Container
```

Not technically identical, but easy to remember.

## Interview Answer

> A Docker image is the packaged application. A container is a running instance of that image.

---

# 2. How Java Becomes a Container

```text
Java Code
   ↓
mvn package
   ↓
JAR
   ↓
Dockerfile
   ↓
Docker Image
   ↓
Container
```

## Interview Answer

> Docker packages the application with its runtime dependencies, so the same artifact can run consistently across environments.

---

# 3. Kubernetes — Remember Only 7 Questions

```text
1. What is running?
2. Where is it running?
3. Why is it failing?
4. What configuration is it using?
5. How is traffic reaching it?
6. Can Kubernetes recover it?
7. Can Kubernetes scale it?
```

Commands:

```bash
kubectl get pods
kubectl get pods -o wide
kubectl describe pod <pod>
kubectl logs <pod>
kubectl get configmap
kubectl get svc
kubectl scale deployment <deployment> --replicas=3
```

---

# 4. The Most Important Kubernetes Mental Model

```text
Deployment
    ↓
ReplicaSet
    ↓
Pod
    ↓
Container
```

### Deployment

Says:

```text
"I want 3 copies of my Payment Service."
```

### ReplicaSet

Keeps the required number of Pods running.

### Pod

Runs one or more containers.

### Container

Runs the application process.

## Interview Crux

> Deployment defines the desired state, and Kubernetes continuously tries to make the actual state match it.

---

# 5. Learn Self-Healing

Check Pods:

```bash
kubectl get pods
```

Delete one:

```bash
kubectl delete pod <payment-pod>
```

Watch:

```bash
kubectl get pods -w
```

You should see:

```text
Terminating
ContainerCreating
Running
```

Mental model:

```text
Desired = 1 Pod
Actual = 0 Pods
        ↓
Kubernetes detects mismatch
        ↓
Creates a new Pod
```

## Interview Answer

> Kubernetes is self-healing because controllers continuously compare desired state with actual state and recreate missing or unhealthy workloads.

---

# 6. Learn Scaling

```bash
kubectl get deployment payment-service
kubectl scale deployment payment-service --replicas=3
kubectl get pods -w
```

Return to one:

```bash
kubectl scale deployment payment-service --replicas=1
```

Mental model:

```text
1 Pod
 ↓
3 Pods
 ↓
Kubernetes creates 2 more
```

## Interview Answer

> Horizontal scaling means increasing the number of application instances instead of only increasing CPU or memory of one instance.

---

# 7. Understand Kubernetes Service

Pods are temporary. Their IP addresses can change.

Clients should not call Pod IPs directly.

```bash
kubectl get svc
```

Mental model:

```text
                  Pod 1
                 /
Client → Service → Pod 2
                 \
                  Pod 3
```

## Interview Crux

> A Service gives a stable network endpoint in front of changing Pods.

---

# 8. How Service Finds Pods

```bash
kubectl get pods --show-labels
kubectl describe service payment-service
```

Look for:

```text
Selector
Endpoints
```

Mental model:

```text
Pod label:
app=payment-service

      ↑
      │ selector
      │
Service selector:
app=payment-service
```

## Interview Answer

> A Kubernetes Service normally finds its target Pods using label selectors.

---

# 9. Kubernetes Troubleshooting Flow

Do not randomly run commands.

Use this order:

```text
Node healthy?
    ↓
Pod running?
    ↓
If not, why?
    ↓
What do logs say?
    ↓
Does Service point to Pods?
    ↓
Is the application healthy?
```

Commands:

```bash
kubectl get nodes
kubectl get pods
kubectl describe pod <pod>
kubectl logs <pod>
kubectl get svc
kubectl get endpoints
```

Then check:

```text
/actuator/health
```

## Interview Memory

```text
GET → DESCRIBE → LOGS → SERVICE → HEALTH
```

---

# 10. The Most Useful Debug Command

```bash
kubectl describe pod <pod>
```

Scroll to:

```text
Events:
```

Typical failures:

```text
FailedScheduling
ErrImagePull
ImagePullBackOff
CrashLoopBackOff
Unhealthy
FailedMount
```

## Interview Answer

> I start with `kubectl get`, then use `describe` to inspect events, and then check logs for the application-level failure.

---

# 11. Logs

Current logs:

```bash
kubectl logs <pod>
```

Follow logs:

```bash
kubectl logs -f <pod>
```

Previous crashed container:

```bash
kubectl logs <pod> --previous
```

Why `--previous` matters:

```text
Container crashes
      ↓
Kubernetes restarts it
      ↓
Current logs may look normal
      ↓
--previous shows the failed instance logs
```

---

# 12. ConfigMap and Secret

```bash
kubectl get configmap
kubectl get secrets
```

Mental model:

```text
Docker Image = Application code
ConfigMap     = Normal configuration
Secret        = Sensitive configuration
```

## Important Principle

> Use the same Docker image across environments. Change configuration outside the image.

```text
DEV
TEST
UAT
PROD
```

Same image. Different configuration.

---

# 13. Liveness, Readiness, and Startup

Your Payment Service already exposes:

```text
/actuator/health
```

### Liveness

```text
"Is the application alive?"
```

If not, Kubernetes can restart it.

### Readiness

```text
"Can it receive traffic now?"
```

If not, it stays running but does not receive Service traffic.

### Startup

```text
"Has startup finished?"
```

## Interview Crux

> Liveness decides restart. Readiness decides traffic.

---

# 14. Deployment Rollout

```bash
kubectl rollout status deployment/payment-service
kubectl rollout history deployment/payment-service
kubectl rollout undo deployment/payment-service
```

Mental model:

```text
Version 1
   ↓
Gradual replacement
   ↓
Version 2
```

## Interview Answer

> Deployment manages rolling updates and supports rollback if the new version is unhealthy.

---

# 15. Docker and Kubernetes Connection

```text
Docker Image
     ↓
Kubernetes Deployment
     ↓
Pod
     ↓
Container
```

## Interview Answer

> Docker packages the application. Kubernetes manages container instances, keeps them healthy, scales them, and exposes them through Services.

---

# 16. Jenkins Connection

Your current setup:

```text
Git
 ↓
Jenkins
 ↓
Build Java
 ↓
Create Docker Image
 ↓
Deploy Kubernetes
 ↓
Deployment
 ↓
Pod
 ↓
Service
 ↓
Payment API
```

## Interview Answer

> Jenkins automates CI/CD. Docker creates the deployable artifact, and Kubernetes runs and manages that artifact in the target environment.

---

# 17. Prometheus and Grafana

```text
Payment App
    ↓
Metrics
    ↓
Prometheus
    ↓
Grafana
```

### Prometheus

Collects and stores metrics.

### Grafana

Shows dashboards and graphs.

## Interview Answer

> Prometheus collects application and platform metrics. Grafana visualizes them.

---

# 18. Kafka in the Same System

```text
Payment Service
      ↓
    Kafka
      ↓
Consumer Service
```

Use Kafka when:

```text
Immediate response is not required
or
Loose coupling is useful
or
Replay is needed
or
Multiple consumers need the same event
```

## Interview Answer

> Kafka is useful for asynchronous, event-driven communication where services should be loosely coupled.

---

# 19. Entire Architecture Mental Model

```text
Developer
   ↓
Git
   ↓
Jenkins
   ↓
Maven Build
   ↓
Docker Image
   ↓
Kubernetes Deployment
   ↓
ReplicaSet
   ↓
Pods
   ↓
Service
   ↓
Payment API
```

Async flow:

```text
Payment Service → Kafka → Consumers
```

Monitoring:

```text
Payment Service → Prometheus → Grafana
```

---

# 20. Important Docker Commands

```bash
docker images
docker ps
docker ps -a
docker logs <container>
docker exec -it <container> bash
docker inspect <container>
docker stop <container>
docker start <container>
docker rm <container>
```

Remember:

```text
SEE
LOG
ENTER
INSPECT
CONTROL
```

---

# 21. Important Kubernetes Commands

```bash
kubectl get nodes
kubectl get pods
kubectl get pods -o wide
kubectl get svc
kubectl get deployments
kubectl get all

kubectl describe pod <pod>
kubectl logs <pod>
kubectl logs <pod> --previous

kubectl scale deployment <deployment> --replicas=3

kubectl rollout status deployment/<deployment>
kubectl rollout history deployment/<deployment>
kubectl rollout undo deployment/<deployment>
```

Remember:

```text
GET
DESCRIBE
LOGS
SCALE
ROLLOUT
```

---

# 22. Interview Scenario — CrashLoopBackOff

Question:

> A Pod is in CrashLoopBackOff. What do you do?

Answer:

> First I inspect Pod events with `kubectl describe pod`. Then I check current and previous logs. I verify configuration, Secrets, dependencies, probes, and resource limits. I fix the cause instead of repeatedly restarting the Pod.

Memory:

```text
Describe
   ↓
Logs
   ↓
Config
   ↓
Dependency
   ↓
Probe
   ↓
Resources
```

---

# 23. Interview Scenario — Pod Is Running but API Does Not Work

Check:

```text
Pod
 ↓
Application
 ↓
Service
 ↓
Endpoints
 ↓
Network
```

Commands:

```bash
kubectl get pods
kubectl logs <pod>
kubectl get svc
kubectl get endpoints
kubectl describe service payment-service
```

## Crux

> A Running Pod does not automatically mean the application is reachable.

---

# 24. Interview Scenario — Service Has No Endpoints

Likely reason:

```text
Service selector
      ≠
Pod label
```

Check:

```bash
kubectl get pods --show-labels
kubectl describe service payment-service
```

## Interview Answer

> I compare the Service selector with Pod labels. If they do not match, the Service cannot discover the Pods.

---

# 25. Interview Scenario — ImagePullBackOff

Think:

```text
Kubernetes cannot get the container image
```

Check:

```bash
kubectl describe pod <pod>
```

Possible causes:

```text
Wrong image name
Wrong tag
Registry unavailable
Private registry authentication missing
Image does not exist
```

---

# 26. Interview Scenario — CrashLoopBackOff Causes

Think:

```text
Container starts
   ↓
Application crashes
   ↓
Kubernetes restarts it
   ↓
Crash again
```

Check:

```bash
kubectl logs <pod> --previous
kubectl describe pod <pod>
```

Possible causes:

```text
Application exception
Bad configuration
Database unavailable
Kafka unavailable
Wrong environment variable
Probe failure
```

---

# 27. Interview Scenario — Pending Pod

Think:

```text
Pod cannot be scheduled
```

Check:

```bash
kubectl describe pod <pod>
```

Possible causes:

```text
Not enough CPU
Not enough memory
Node selector mismatch
Volume unavailable
Scheduling constraint
```

---

# 28. Interview Scenario — More Pods but No More Throughput

Possible reason:

```text
Application scales
      ↓
Shared bottleneck remains
      ↓
Database / Kafka / Connection Pool
```

Example:

```text
1 Pod  → 20 DB connections
10 Pods → 200 DB connections
```

The database may become slower.

## Crux

> Horizontal scaling helps only if downstream dependencies can also handle the extra load.

---

# 29. Architect-Level Spring Boot Deployment Answer

If the interviewer asks:

> Explain how you deploy a Spring Boot microservice.

Answer:

> I build the Spring Boot application into a JAR, package it as an immutable Docker image, and store that image in a registry. Kubernetes Deployment runs the required number of Pods from that image. A Service provides stable access. ConfigMaps and Secrets provide configuration. Readiness and liveness probes manage traffic and recovery. CI/CD deploys new versions using rolling updates, while Prometheus and Grafana provide observability.

This one answer connects most concepts.

---

# 30. Hands-On Learning Order

Practice in this order:

```text
1. Docker image
        ↓
2. Docker container
        ↓
3. Kubernetes Pod
        ↓
4. Deployment
        ↓
5. Self-healing
        ↓
6. Scaling
        ↓
7. Service
        ↓
8. Labels and selectors
        ↓
9. ConfigMap and Secret
        ↓
10. Health probes
        ↓
11. Logs and troubleshooting
        ↓
12. Rolling deployment
        ↓
13. Kafka
        ↓
14. Prometheus
        ↓
15. Grafana
        ↓
16. Jenkins CI/CD
```

---

# 31. Final Memory Map

If you remember only this, you can rebuild most of the knowledge.

```text
CODE
 ↓
BUILD
 ↓
IMAGE
 ↓
CONTAINER
 ↓
POD
 ↓
DEPLOYMENT
 ↓
SERVICE
 ↓
USER
```

Configuration:

```text
ConfigMap + Secret
```

Reliability:

```text
Deployment + Probes
```

Scaling:

```text
Replicas + HPA
```

Debugging:

```text
GET → DESCRIBE → LOGS
```

Deployment:

```text
ROLLOUT → VERIFY → ROLLBACK
```

Monitoring:

```text
APP → PROMETHEUS → GRAFANA
```

Messaging:

```text
PRODUCER → KAFKA → CONSUMER
```

CI/CD:

```text
GIT → JENKINS → DOCKER → KUBERNETES
```

---

# 32. Five Lines to Remember Before an Interview

> Docker packages the application. Kubernetes manages the containers.

> Deployment maintains desired state. Service gives stable access to changing Pods.

> Liveness decides restart. Readiness decides traffic.

> For troubleshooting: get, describe, logs, Service, health.

> More Pods do not guarantee more throughput if the database, Kafka, or another shared dependency is the real bottleneck.

---

# 33. Practice Rule

Do not only read this guide.

Use this loop:

```text
Understand
   ↓
Run
   ↓
Break
   ↓
Observe
   ↓
Fix
   ↓
Explain
```

If you can deliberately break the Payment App, identify the failure, fix it, and explain why Kubernetes behaved that way, you understand the concept better than someone who memorized fifty commands.
