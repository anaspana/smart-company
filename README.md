# Predmet: Napredne Veb Tehnologije

## Frontend: Angular
## Backend: Java + Spring Boot

## Student 1:
- **Ana Spanovic**, SV48/2021

## Student 2:
- **Andrija Slovic**, SV12/2021

## Student 3:
- **Vojkan Markovic**, SV65/2021

---

## Tehnologije

- **Backend:** Spring Boot (Java 17)
- **Frontend:** Angular (production build servira se preko nginx-a)
- **Baze podataka:** PostgreSQL (relaciona), InfluxDB (time series)
- **Message broker:** RabbitMQ
- **Keširanje:** Redis
- **Reverse proxy / statički sadržaj:** nginx
- **Skladištenje fajlova:** AWS S3 (presigned URL pristup)

---

## Potrebni servisi (Docker)

Sledeći servisi se pokreću kroz Docker: **PostgreSQL**, **InfluxDB**, **RabbitMQ**, **Redis**, i **nginx** (koji servira Angular production build i prosleđuje API pozive ka backendu).

```bash
docker compose up -d postgres influxdb rabbitmq redis
```

Zatim, build i pokretanje nginx-a (buildu-je Angular production build automatski):

```bash
docker compose up --build nginx
```

---

## Backend

Backend se pokreće **lokalno** (van Dockera), pošto se povezuje na Docker servise preko portova mapiranih na host mašinu:

```bash
cd back/smartcompany
mvn spring-boot:run
```

Backend sluša na `localhost:8080`, ali svi pozivi sa frontenda idu kroz nginx na `http://localhost` (port 80) — nginx prosleđuje `/api/*` putanje ka backendu.

Automatski se kreira superadmin nalog sa username-om `admin`, dok se lozinka nalazi u `superadmin.txt` u direktorijumu `back/smartcompany`.

---

## Popunjavanje baze testnim podacima

```bash
python seed_script.py
```

(Skripta se povezuje na `localhost:5432` i `localhost:8086` — portovi su mapirani iz Docker kontejnera na host.)

---

## Pristup aplikaciji

- **Frontend (kroz nginx):** http://localhost
- **Swagger / OpenAPI dokumentacija:** http://localhost:8080/api/swagger-ui/index.html
- **RabbitMQ management:** http://localhost:15672 (guest/guest)
- **InfluxDB UI:** http://localhost:8086

---

## Pokretanje simulatora

Simulator generiše podatke u realnom vremenu za fabrike, magacine i vozila. Pokreće se iz `factory-simulator` direktorijuma u tri odvojena terminala:

```bash
# Terminal 1 — fabrika
cd factory-simulator
set SPRING_CONFIG_LOCATION=classpath:/application-factory.properties
mvn spring-boot:run

# Terminal 2 — magacin
cd factory-simulator
set SPRING_CONFIG_LOCATION=classpath:/application-warehouse.properties
mvn spring-boot:run

# Terminal 3 — vozilo
cd factory-simulator
set SPRING_CONFIG_LOCATION=classpath:/application-vehicle.properties
mvn spring-boot:run
```

Simulator šalje heartbeat i podatke (temperatura, distanca, proizvodnja) putem RabbitMQ-a ka backend-u koji ih upisuje u InfluxDB.