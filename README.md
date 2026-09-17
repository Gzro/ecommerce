# E-commerce Maven Parent

Projet Maven multi-module servant de parent aux microservices.

## Modules

- order-service
- payment-service
- inventory-service
- product-service

Chaque microservice possède son propre `pom.xml` et doit rester indépendant côté métier.

## Build

Depuis la racine :

```bash
mvn clean verify
```

## Structure recommandée

Les domaines métier ne doivent pas être placés dans un module `common`.
Les éléments réellement transverses (logging, tracing, configuration technique, etc.)
pourront être extraits plus tard dans des modules dédiés si nécessaire.
