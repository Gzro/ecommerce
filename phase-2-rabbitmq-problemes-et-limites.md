# Phase 2 --- Architecture asynchrone avec RabbitMQ : problèmes et limites

## 1. Objectif

La phase 2 a remplacé progressivement les appels HTTP synchrones par
RabbitMQ.

Workflow :

``` text
OrderService
     |
 OrderCreated
     v
 RabbitMQ
   /   \
  v     v
Inventory Payment
  |       |
Stock    Payment
Reserved Completed
   \     /
    v   v
 OrderService
      |
      v
   CONFIRMED
```

RabbitMQ nous a permis d'introduire le découplage temporel,
l'asynchronisme et la distribution d'un événement vers plusieurs
consommateurs.

------------------------------------------------------------------------

## 2. Ce que RabbitMQ améliore

### Découplage temporel

`OrderService` n'attend plus directement la réponse HTTP
d'`InventoryService` ou de `PaymentService`.

``` text
OrderService
     |
     | OrderCreated
     v
 RabbitMQ
     |
     v
InventoryService
```

Le traitement devient éventuellement cohérent.

### Distribution à plusieurs consommateurs

Le publisher publie une seule fois dans un exchange :

``` text
                    order.exchange
                         |
                    order.created
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
     inventory.queue  payment.queue  notification.queue
```

Le publisher ne connaît pas directement les consommateurs.

------------------------------------------------------------------------

# 3. Limite : absence de transaction distribuée

RabbitMQ ne transforme pas plusieurs transactions locales en une
transaction globale.

On peut avoir :

``` text
Order DB
    |
    | commit
    v
Order = PENDING

Inventory DB
    |
    | commit
    v
Stock = RESERVED
```

Il n'existe pas de rollback automatique entre ces bases.

RabbitMQ ne garantit donc pas :

> Toutes les opérations réussissent ou aucune ne réussit.

------------------------------------------------------------------------

# 4. Problème majeur : la compensation

Notre POC met particulièrement en évidence ce scénario :

``` text
OrderCreated
     |
     v
InventoryService
     |
     v
StockReserved
     |
     v
PaymentService
     |
     v
PaymentFailed
```

On peut alors obtenir :

``` text
Order      = CANCELLED
Stock      = RESERVED
Payment    = FAILED
```

Le système est incohérent.

Il faut une compensation :

``` text
PaymentFailed
      |
      v
OrderService
      |
      | ReleaseStock
      v
InventoryService
      |
      v
StockReleased
```

Mais cette compensation peut elle-même échouer :

``` text
PaymentFailed
      |
      v
ReleaseStock
      |
      X
InventoryService indisponible
```

RabbitMQ ne résout pas automatiquement ce problème.

------------------------------------------------------------------------

# 5. Eventual consistency

Avec RabbitMQ, l'état d'une commande évolue progressivement :

``` text
PENDING
   |
   | StockReserved
   v
stockReserved = true
   |
   | PaymentCompleted
   v
paymentCompleted = true
   |
   v
CONFIRMED
```

Entre deux événements, les services peuvent temporairement avoir des
états différents.

C'est le principe d'**eventual consistency**.

------------------------------------------------------------------------

# 6. Idempotence et messages redélivrés

Un message peut être reçu plusieurs fois.

Exemple :

``` text
OrderCreated
     |
     v
InventoryService
     |
     | DB commit
     v
Stock reserved
     |
     X ACK perdu
     |
     v
RabbitMQ redélivre
```

Sans idempotence, une deuxième réservation pourrait être effectuée.

Le consommateur doit donc être capable de détecter un traitement déjà
effectué.

Par exemple, `orderId` peut servir à vérifier si une réservation existe
déjà.

------------------------------------------------------------------------

# 7. Ordre des événements

Dans une architecture asynchrone, il ne faut pas supposer que tous les
événements seront toujours traités exactement dans l'ordre souhaité.

Il faut donc définir des règles métier permettant de gérer :

-   les doublons ;
-   les événements en retard ;
-   les événements déjà traités ;
-   les états inattendus.

------------------------------------------------------------------------

# 8. Gestion distribuée des erreurs

Avec REST :

``` text
InventoryService
      |
      | HTTP 409
      v
OrderService
```

Avec RabbitMQ :

``` text
OrderCreated
      |
      v
InventoryService
      |
      v
StockReservationFailed
      |
      v
OrderService
```

L'erreur devient elle-même un événement.

Cela augmente le découplage mais aussi la complexité du workflow.

------------------------------------------------------------------------

# 9. Observabilité

Une commande peut maintenant traverser plusieurs composants :

``` text
OrderService
    |
 RabbitMQ
    |
InventoryService
    |
 RabbitMQ
    |
PaymentService
    |
 RabbitMQ
    |
OrderService
```

Lorsqu'une commande reste `PENDING`, il faut pouvoir déterminer où elle
est bloquée.

Il devient donc important d'avoir :

-   des logs structurés ;
-   `orderId` comme identifiant de corrélation ;
-   des métriques ;
-   du tracing distribué.

------------------------------------------------------------------------

# 10. Contrats d'événements

Les microservices ne partagent plus leurs entités JPA, mais ils
partagent des contrats de messages.

Exemple :

``` json
{
  "orderId": 123,
  "customerId": 456,
  "totalAmount": 2400.00
}
```

Une modification brutale du format peut casser les consommateurs.

Il faut donc penser à :

-   compatibilité des versions ;
-   évolution des événements ;
-   validation ;
-   versionnement des contrats.

------------------------------------------------------------------------

# 11. Problème du dual write

Un service peut devoir faire deux opérations :

``` text
1. modifier sa DB
2. publier un événement
```

Exemple :

``` text
INSERT Order
     |
     v
 DB commit
     |
     v
publish OrderCreated
     |
     X
RabbitMQ indisponible
```

Résultat :

``` text
Order existe en DB
mais
OrderCreated n'a pas été publié
```

Le problème inverse est également possible :

``` text
OrderCreated publié
mais
transaction DB annulée
```

C'est le problème du **dual write**.

Une solution importante que nous étudierons avec Kafka est le
**Transactional Outbox Pattern**.

------------------------------------------------------------------------

# 12. Historique et replay

Avec RabbitMQ, le modèle utilisé dans notre POC est principalement :

``` text
publish
   |
   v
queue
   |
   v
consume
   |
   v
ACK
```

Une fois le message acquitté, le consommateur ne dispose pas
naturellement d'un historique complet lui permettant de rejouer tous les
événements passés.

Cela devient moins pratique lorsqu'on veut :

-   créer un nouveau consommateur ;
-   relire des événements historiques ;
-   reconstruire un état ;
-   rejouer un traitement.

C'est l'un des points que nous étudierons avec Kafka.

------------------------------------------------------------------------

# 13. RabbitMQ n'est pas le responsable de la cohérence métier

RabbitMQ transporte les messages.

Il ne connaît pas nos règles :

``` text
stockReserved && paymentCompleted
        => CONFIRMED
```

ou :

``` text
PaymentFailed
        => ReleaseStock
```

Ces règles restent dans les microservices.

Kafka ne changera pas non plus ce principe.

------------------------------------------------------------------------

# 14. RabbitMQ versus Kafka : ce que nous allons étudier

Le passage à Kafka ne signifie pas :

> Kafka résout tous les problèmes de RabbitMQ.

Les modèles sont différents.

Une vue simplifiée :

  -----------------------------------------------------------------------
  RabbitMQ                            Kafka
  ----------------------------------- -----------------------------------
  Exchange                            Topic

  Queue                               Consumer group / partitions selon
                                      le besoin

  Routing key                         Topic + clé de partition

  ACK                                 Offset / commit

  Message consommé puis généralement  Événement conservé selon la
  retiré de la queue                  rétention

  Bindings pour router vers les       Consumer groups lisant
  queues                              indépendamment le topic

  Replay moins naturel                Replay via les offsets
  -----------------------------------------------------------------------

Cette correspondance n'est pas exacte : une queue RabbitMQ et une
partition Kafka ne sont pas équivalentes.

------------------------------------------------------------------------

# 15. Phase 3 --- Kafka

La phase 3 nous permettra d'étudier :

## Topics

``` text
order-events
payment-events
stock-events
```

## Partitions

``` text
order-events
   |
   +-- partition 0
   +-- partition 1
   +-- partition 2
```

## Consumer groups

``` text
order-events
      |
      +-- inventory-group
      |
      +-- payment-group
      |
      +-- notification-group
```

Chaque consumer group peut lire indépendamment le même topic.

## Offsets

Comprendre où un consumer s'est arrêté et comment reprendre la lecture.

## Retention et replay

``` text
order-events
     |
     | événements historiques
     v
nouveau consumer
```

## Idempotence

Réutiliser le problème rencontré avec RabbitMQ et construire des
consommateurs robustes aux doublons.

## Transactional Outbox

Résoudre plus proprement le problème :

``` text
DB update
+
publication d'événement
```

## Saga / compensation

Kafka ne supprime pas le problème :

``` text
StockReserved
      |
PaymentFailed
      |
      v
ReleaseStock
```

La compensation reste une responsabilité métier.

------------------------------------------------------------------------

# 16. Comparaison globale des deux phases

## Phase 1 --- REST synchrone

``` text
OrderService
     |
     | HTTP
     v
InventoryService
     |
     | HTTP
     v
PaymentService
```

Problèmes principaux :

-   couplage fort ;
-   appels synchrones ;
-   propagation des pannes ;
-   latence cumulative ;
-   timeouts ;
-   absence de transaction distribuée ;
-   compensations.

## Phase 2 --- RabbitMQ

``` text
              RabbitMQ
             /        \
            v          v
       Inventory     Payment
            \        /
             v      v
             OrderService
```

Améliorations :

-   asynchronisme ;
-   découplage temporel ;
-   distribution d'événements ;
-   meilleure isolation des indisponibilités.

Nouveaux problèmes :

-   eventual consistency ;
-   doublons ;
-   idempotence ;
-   compensation ;
-   ordre des événements ;
-   observabilité ;
-   dual write ;
-   évolution des contrats ;
-   replay/historique.

## Phase 3 --- Kafka

L'objectif sera de comprendre en profondeur :

``` text
Kafka
 |
 +-- Topics
 +-- Partitions
 +-- Consumer Groups
 +-- Offsets
 +-- Retention
 +-- Replay
 +-- Idempotence
 +-- Transactional Outbox
 +-- Saga / Compensation
```

------------------------------------------------------------------------

# 17. Conclusion

RabbitMQ nous a permis de passer d'une architecture :

``` text
REST synchrone
```

à une architecture :

``` text
Event-driven / asynchrone
```

Mais nous avons découvert que le broker ne suffit pas pour garantir la
cohérence globale.

Les principaux problèmes identifiés sont :

1.  absence de transaction distribuée ;
2.  eventual consistency ;
3.  compensation ;
4.  messages dupliqués ;
5.  idempotence ;
6.  ordre des événements ;
7.  erreurs distribuées ;
8.  observabilité ;
9.  évolution des contrats ;
10. dual write ;
11. replay et historique.

Le cas le plus concret de notre POC est :

``` text
StockReserved
      |
PaymentFailed
      |
      v
Order = CANCELLED
Stock = RESERVED
```

Il faut alors compenser :

``` text
PaymentFailed
      |
      v
ReleaseStock
      |
      v
InventoryService
      |
      v
StockReleased
```

C'est ce problème, parmi d'autres, qui nous servira de point de départ
pour la phase 3.

**Important : Kafka ne va pas supprimer automatiquement les
compensations.**

La phase 3 aura pour objectif de comprendre ce que Kafka apporte
réellement par rapport à RabbitMQ, notamment grâce à son modèle de
topics, partitions, consumer groups, offsets, rétention et replay, puis
d'introduire des mécanismes comme l'idempotence et le Transactional
Outbox.

------------------------------------------------------------------------

## Vue globale du POC

``` text
PHASE 1
REST synchrone
      |
      v
Découverte des problèmes
      |
      v
PHASE 2
RabbitMQ
      |
      v
Découverte de nouveaux problèmes
      |
      +-- Eventual consistency
      +-- Idempotence
      +-- Dual write
      +-- Compensation
      +-- Observabilité
      +-- Replay limité
      |
      v
PHASE 3
Kafka
      |
      +-- Topics
      +-- Partitions
      +-- Consumer Groups
      +-- Offsets
      +-- Retention
      +-- Replay
      +-- Idempotence
      +-- Outbox
      +-- Saga / Compensation
```
