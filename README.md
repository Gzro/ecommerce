# E-commerce Maven Parent

Projet Maven multi-module servant de parent à une architecture de microservices e-commerce.

L'objectif du projet est de construire progressivement une architecture distribuée afin de comprendre les différences entre communication synchrone REST, messaging avec RabbitMQ et, dans une phase suivante, Kafka.

## Architecture

Le projet est organisé autour de plusieurs microservices indépendants :

* `order-service` — gestion des commandes
* `payment-service` — gestion des paiements
* `inventory-service` — gestion du stock
* `product-service` — gestion des produits

Chaque microservice possède son propre `pom.xml` et doit rester indépendant côté métier.

## Prérequis

Pour lancer le projet localement, les outils suivants sont nécessaires :

* **Java 21**
* **Maven 3.9+**
* **Docker**
* **Docker Compose**

Vérifier les installations :

```bash
java -version
mvn -version
docker --version
docker compose version
```

Le projet utilise actuellement **Spring Boot 3.5.x** et **Java 21**.

## Installation

### 1. Cloner le projet

```bash
git clone <URL_DU_REPOSITORY>
cd ecommerce
```

### 2. Démarrer les infrastructures avec Docker Compose

Les services utilisent des composants d'infrastructure exécutés dans Docker, notamment PostgreSQL et RabbitMQ.

Depuis la racine du projet :

```bash
docker compose up -d
```

Vérifier que les conteneurs sont démarrés :

```bash
docker compose ps
```

Pour RabbitMQ, l'interface d'administration est disponible à :

```text
http://localhost:15672
```

RabbitMQ utilise :

```text
AMQP     : localhost:5672
Management UI : localhost:15672
```
Identifiants utilisés par le POC :
Ils doivent être vérifiés dans leurs fichiers `application.properties` ou `application.yml`.

### 3. Construire le projet

Depuis la racine :

```bash
mvn clean verify
```

Le build compile l'ensemble des modules Maven et exécute les tests disponibles.

### 4. Lancer les microservices

Chaque microservice peut être lancé indépendamment.

Par exemple :

```bash
mvn -pl order-service spring-boot:run
```

```bash
mvn -pl inventory-service spring-boot:run
```

```bash
mvn -pl payment-service spring-boot:run
```

```bash
mvn -pl product-service spring-boot:run
```

Les ports utilisés par les différents services doivent être vérifiés dans leurs fichiers `application.properties` ou `application.yml`.

## Base de données

Le POC utilise PostgreSQL.

L'infrastructure PostgreSQL est démarrée avec Docker Compose.

Exemple de configuration locale :

```text
host: localhost
port: 5432
database: order_db
```

Les services possèdent leurs propres données et ne doivent pas accéder directement aux tables d'un autre microservice.

Chaque service est responsable de son propre modèle de données.

## RabbitMQ

La phase actuelle du projet utilise RabbitMQ pour la communication asynchrone entre les microservices.

Le principe utilisé est :

```text
Producer
   |
   v
Exchange
   |
   | routing key
   v
Queue
   |
   v
Consumer
```

Par exemple :

```text
OrderService
     |
     | OrderCreated
     v
order.exchange
     |
     +----------------------+
     |                      |
     v                      v
InventoryService      PaymentService
```

Un producteur publie un événement dans un exchange sans connaître directement les consommateurs.

Les queues et bindings sont configurés par les microservices consommateurs.

## Fonctionnement actuel

Le projet évolue progressivement par 3 phases, voir la description de chaque phase dans son fichier .md

## Build

Depuis la racine :

```bash
mvn clean verify
```

Pour construire un seul module :

```bash
mvn -pl order-service clean package
```

Pour construire un module et ses dépendances Maven :

```bash
mvn -pl order-service -am clean package
```

## Docker

Les composants d'infrastructure peuvent être démarrés avec :

```bash
docker compose up -d
```

Arrêter les conteneurs :

```bash
docker compose down
```

Pour supprimer également les volumes et repartir avec des données vierges :

```bash
docker compose down -v
```

Cette dernière commande supprime les données persistées des conteneurs. Elle est particulièrement utile pendant le POC lorsqu'on souhaite repartir d'un état propre.

## Structure du projet

```text
ecommerce/
├── pom.xml
├── README.md
├── docker-compose.yml
│
├── order-service/
│   ├── pom.xml
│   └── src/
│
├── payment-service/
│   ├── pom.xml
│   └── src/
│
├── inventory-service/
│   ├── pom.xml
│   └── src/
│
└── product-service/
    ├── pom.xml
    └── src/
```

## Principe d'indépendance des microservices

Chaque microservice doit rester indépendant côté métier.

Les domaines métier ne doivent pas être placés dans un module `common`.

En particulier, les microservices ne doivent pas partager :

* leurs entités JPA ;
* leurs repositories ;
* leurs services métier ;
* leurs modèles internes.

Les événements échangés via RabbitMQ ou Kafka doivent être considérés comme des **contrats de communication** entre services.

Les éléments réellement transverses pourront être extraits plus tard dans des modules dédiés si nécessaire, par exemple :

* logging ;
* tracing ;
* configuration technique ;
* observabilité ;
* utilitaires purement techniques.

## Nettoyage de l'environnement

Pour arrêter complètement l'environnement :

```bash
docker compose down
```

Pour repartir avec des bases de données et des données RabbitMQ vierges :

```bash
docker compose down -v
docker compose up -d
```

## Objectif pédagogique

Ce projet est volontairement construit progressivement.

L'objectif est de partir d'une architecture simple :

```text
REST
```

puis d'observer ses limites avant d'introduire :

```text
RabbitMQ
```

et enfin :

```text
Kafka
```

Cette progression permet de comprendre **pourquoi** certaines architectures distribuées utilisent du messaging et quels nouveaux problèmes apparaissent lorsqu'on abandonne les appels synchrones.
