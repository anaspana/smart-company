<h1 align="center">
  SmartCompany — Industrial IoT Management Platform
</h1>

<p align="center">
  A full-stack platform for managing companies, factories, warehouses, and vehicle fleets, with real-time sensor data ingestion from a factory/warehouse/vehicle simulator over RabbitMQ, time-series storage in InfluxDB, and a load-tested Spring Boot backend behind an Angular frontend.
</p>

<div align="center">

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-backend-brightgreen)
![Angular](https://img.shields.io/badge/Angular-frontend-DD0031)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-relational-336791)
![InfluxDB](https://img.shields.io/badge/InfluxDB-2.7-22ADF6)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-message%20broker-FF6600)
![Redis](https://img.shields.io/badge/Redis-caching-DC382D)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![status](https://img.shields.io/badge/Status-University%20Project-yellow)

</div>

## About the Project

SmartCompany is a full-stack platform for managing a company's factories, warehouses, product catalog, orders, and vehicle fleet, with a real-time monitoring layer for physical assets. A separate simulator application generates live sensor data — factory production, warehouse temperature, and vehicle distance/telemetry — and publishes it via RabbitMQ, which the backend consumes and persists as time-series data in InfluxDB.

The application supports role-based access, including a superadmin account created automatically on first run, and stores uploaded files (e.g. product images) in AWS S3 using presigned URLs. Redis is used for caching, and the whole system is fronted by nginx, which serves the Angular production build and reverse-proxies API calls to the backend. The project also includes a Locust-based load testing suite with a written performance report.

## Features

- Company, factory, and warehouse management, including geographic location data
- Product catalog and category management
- Order management workflow
- Vehicle fleet management with live telemetry
- User management with role-based access and an auto-provisioned superadmin account
- Account activation via emailed tokens
- Real-time sensor data ingestion (factory production, warehouse temperature, vehicle distance) via RabbitMQ listeners, persisted to InfluxDB as time series
- Standalone simulator application that generates and publishes realistic factory, warehouse, and vehicle data
- File upload/storage via AWS S3 using presigned URLs
- Redis-backed caching layer for frequently accessed data
- Swagger/OpenAPI documentation for the backend REST API
- nginx reverse proxy serving the Angular production build and routing `/api/*` to the backend
- Load testing suite (Locust) with a documented performance report

## Architecture

- **Backend (`back/smartcompany`, Spring Boot)** — exposes the REST API (companies, factories, warehouses, products, orders, vehicles, users, file storage, activation tokens), listens for RabbitMQ messages from the simulator, writes time-series readings to InfluxDB, caches data in Redis, and persists relational data in PostgreSQL.
- **Factory simulator (`factory-simulator`, Spring Boot)** — a separate application that can be launched in three roles (factory, warehouse, vehicle) via different Spring profiles, each generating and publishing heartbeat and sensor data to RabbitMQ.
- **Frontend (`front/smart-company-app`, Angular)** — a modular Angular app with dedicated feature areas for auth, companies, factories, warehouses, products, orders, vehicles, and superadmin, built for production and served through nginx.
- **nginx** — serves the Angular build and reverse-proxies `/api/*` requests to the backend, acting as the single entry point for the application.
- **Data stores** — PostgreSQL for relational data, InfluxDB for time-series sensor data, Redis for caching, RabbitMQ as the message broker between the simulator and the backend.
- **Load testing (`load_testing`)** — a Locust test suite used to benchmark the backend, with results documented in `SmartCompany_Load_Testing_Izvestaj.pdf`.

## Technologies

**Backend:** Java 17, Spring Boot, Spring Data JPA, RabbitMQ listeners, Swagger/OpenAPI

**Frontend:** Angular, TypeScript, served in production via nginx

**Data & Storage:** PostgreSQL, InfluxDB (time series), Redis (caching), AWS S3 (file storage via presigned URLs)

**Messaging:** RabbitMQ

**Infrastructure:** Docker Compose, nginx

**Load testing:** Python, Locust

## Project Structure

```
📦 root
 ┣ 📂 back/smartcompany         — Spring Boot backend (REST API, RabbitMQ listeners, InfluxDB/PostgreSQL/Redis integration)
 ┃ ┗ 📂 src/main/java/com/smartcompany
 ┃   ┣ controller / service / repository / model / dto   — layered by domain (company, factory, warehouse, product, order, user, vehicle)
 ┃   ┣ listener        — RabbitMQ message listeners (factory, warehouse, vehicle)
 ┃   ┗ config / exception / specification
 ┣ 📂 factory-simulator         — standalone Spring Boot app simulating factory/warehouse/vehicle data sources
 ┣ 📂 front/smart-company-app   — Angular application (auth, companies, factories, warehouses, products, orders, vehicles, superadmin)
 ┣ 📂 load_testing              — Locust load testing suite
 ┣ docker-compose.yml           — PostgreSQL, InfluxDB, RabbitMQ, Redis, nginx
 ┗ SmartCompany_Load_Testing_Izvestaj.pdf   — load testing report
```

## Running the Project

### Prerequisites

- Java 17
- Node.js
- Docker and Docker Compose
- Python (for load testing)

### Infrastructure

```bash
docker compose up -d postgres influxdb rabbitmq redis
```

Then build and run nginx (this also builds the Angular production bundle):

```bash
docker compose up --build nginx
```

### Backend

The backend runs locally (outside Docker), connecting to the Dockerized services via host-mapped ports:

```bash
cd back/smartcompany
mvn spring-boot:run
```

The backend listens on `localhost:8080`, but the frontend calls go through nginx on `http://localhost` (port 80), which proxies `/api/*` to the backend. A superadmin account (`admin`) is created automatically on startup; its password is written to `superadmin.txt` in `back/smartcompany`.

### Seeding test data

```bash
python seed_script.py
```

### Simulator

Run each role from `factory-simulator` in a separate terminal:

```bash
cd factory-simulator
$env:SPRING_CONFIG_LOCATION="classpath:/application-factory.properties"
mvn spring-boot:run
```

```bash
cd factory-simulator
$env:SPRING_CONFIG_LOCATION="classpath:/application-warehouse.properties"
mvn spring-boot:run
```

```bash
cd factory-simulator
$env:SPRING_CONFIG_LOCATION="classpath:/application-vehicle.properties"
mvn spring-boot:run
```

The simulator publishes heartbeat and sensor data (temperature, distance, production) via RabbitMQ, which the backend consumes and writes to InfluxDB.

### Access

- **Frontend (via nginx):** http://localhost
- **Swagger / OpenAPI docs:** http://localhost:8080/api/swagger-ui/index.html
- **RabbitMQ management:** http://localhost:15672 (guest/guest)
- **InfluxDB UI:** http://localhost:8086

## Live Demo Deployment

The whole system can run on one small server (1 GB of RAM, e.g. an Oracle Cloud
Always Free instance) with `docker-compose.prod.yml`:

- GitHub Actions builds the backend, simulator and web images on every push to
  `main` and publishes them to the GitHub Container Registry; the server only
  pulls them.
- Caddy serves the Angular app, forwards `/api` to the backend and obtains HTTPS
  automatically.
- The three simulators run in one process (`SPRING_PROFILES_ACTIVE=factory,warehouse,vehicle`),
  and every service is limited to as little memory as it works with.
- Uploaded files are stored on the server's disk (`app.storage.type=local`, the
  default); set `app.storage.type=s3` and the `aws.*` properties to use S3.
- Emails are switched off and demo accounts are created for every role
  (`demo-admin`, `demo-manager`, `demo-customer`), which can't be blocked or have
  their password changed.

On the server:

```bash
cp .env.example .env              # fill in the host name and passwords
mkdir -p data                     # data/smartcompanydb.dump is restored on first start
docker compose pull
docker compose up -d
```

`deploy/reset-demo.sh` puts the demo back to its starting state (database, empty
cache, original uploads). It copies the database from an untouched template,
which takes a minute or two instead of restoring the dump again; save the
template once after the first start:

```bash
sudo sh deploy/make-template.sh
```

Then run the reset nightly from root's cron (`sudo crontab -e`):

```bash
30 3 * * * sh /home/ubuntu/app/smart-company-platform/deploy/reset-demo.sh >> /home/ubuntu/app/reset-demo.log 2>&1
```

## Authors

- Andrija Slović, SV12/2021
- Ana Spanović, SV48/2021
- Vojkan Marković, SV65/2021

University project — Advanced Web Technologies course.
