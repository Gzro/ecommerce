# Phase 3 — Kafka : conclusions, problèmes rencontrés et limites

> Synthèse de la troisième phase du POC e-commerce. Cette phase remplace progressivement les appels HTTP synchrones entre microservices par des événements et des commandes transportés par Kafka.

## 1. Objectif de la phase

La phase 3 avait pour objectif de comprendre Kafka par la pratique, puis de l'intégrer dans le workflow de commande :

- découvrir les topics, partitions, clés, offsets et consumer groups ;
- publier `OrderCreated` depuis `OrderService` ;
- faire consommer cet événement par `InventoryService` ;
- publier le résultat de la réservation du stock ;
- ne déclencher le paiement qu'après une réservation réussie ;
- publier les résultats de paiement ;
- gérer l'échec du paiement et la compensation du stock ;
- commencer à protéger les traitements contre les messages dupliqués ;
- étudier le problème du *dual write* et le Transactional Outbox Pattern.

L'objectif n'était pas de faire disparaître tous les problèmes distribués, mais de comprendre ce que Kafka apporte et quelles responsabilités restent dans le code métier.

---

## 2. Architecture obtenue

Le flux métier cible est devenu :

```text
Client
  |
  v
OrderService
  |
  | enregistre Order = PENDING
  | publie OrderCreated
  v
Kafka : order-created
  |
  v
InventoryService
  |
  | réserve les quantités
  |
  +---- succès ---------------------> Kafka : stock-reserved
  |                                      |
  |                                      v
  |                                PaymentService
  |                                      |
  |                         +------------+------------+
  |                         |                         |
  |                         v                         v
  |                 payment-completed           payment-failed
  |                         |                         |
  +---- échec --------------|-------------------------+
        |                   |                         |
        v                   v                         v
stock-reservation-failed  OrderService             OrderService
                            |                         |
                            v                         v
                        CONFIRMED                 CANCELLED
                                                      |
                                                      | ReleaseStock
                                                      v
                                              Kafka : release-stock
                                                      |
                                                      v
                                               InventoryService
                                                      |
                                                      v
                                           réservation = RELEASED
```

Les services gardent chacun leur base de données et leurs DTO. Les événements échangés ne sont pas des entités JPA partagées.

### Règle métier importante

`PaymentService` ne consomme plus directement `order-created`. Il consomme `stock-reserved` et ne tente donc le paiement qu'après la réservation réussie du stock.

Cette dépendance est exprimée par la chaîne d'événements, pas par un appel HTTP direct entre `InventoryService` et `PaymentService`.

---

## 3. Ce que nous avons appris sur Kafka

### 3.1 Topics

Un topic est un flux d'événements, par exemple :

```text
order-created
stock-reserved
stock-reservation-failed
payment-completed
payment-failed
release-stock
```

Kafka ne fonctionne pas exactement comme RabbitMQ : un topic Kafka n'est pas une queue. Les événements sont conservés selon la politique de rétention du topic, même après qu'un consumer les a lus.

Dans le POC, les topics sont déclarés côté Spring avec des beans `NewTopic`, plutôt que d'être créés manuellement à chaque démarrage. Un topic existant n'est toutefois pas automatiquement ramené à la configuration déclarée : par exemple, définir deux partitions dans le code ne transforme pas nécessairement un topic existant à une partition en deux partitions.

### 3.2 Partitions

Un topic peut comporter plusieurs partitions :

```text
order-created
  +-- partition 0
  +-- partition 1
```

Les partitions permettent le parallélisme. Dans un même consumer group, une partition est assignée à un seul consumer du groupe à un instant donné. Si un groupe contient davantage de consumers que le topic n'a de partitions, certains consumers resteront inactifs.

Le nombre de partitions doit donc être choisi en tenant compte du parallélisme souhaité, du débit et des garanties d'ordre nécessaires.

### 3.3 Clé de partitionnement

Nous avons envoyé l'identifiant de commande comme clé :

```java
kafkaTemplate.send(topic, event.getOrderId().toString(), event);
```

La clé permet au partitionneur de diriger les événements d'une même commande vers la même partition, tant que la stratégie de partitionnement et la configuration du topic restent compatibles.

La clé n'est pas l'offset et n'est pas non plus l'identifiant technique du message Kafka : c'est une donnée utilisée pour choisir la partition et qui peut aussi servir à retrouver l'entité métier concernée.

### 3.4 Consumer groups

Nous avons observé deux comportements différents :

- **Même consumer group** : les instances se partagent les partitions du topic. Le but est de répartir le travail.
- **Consumer groups différents** : chaque groupe suit indépendamment le flux du topic. Il peut donc traiter le même événement à ses propres fins.

C'est ce qui permet à plusieurs applications indépendantes de consommer un même topic, sans que `OrderService` publie une fois pour Inventory et une autre fois pour Payment.

Le design final a néanmoins imposé une règle métier : Payment consomme `stock-reserved`, pas `order-created`, pour respecter l'ordre fonctionnel « stock réservé avant paiement ».

### 3.5 Offsets, rétention et replay

Un offset représente une position dans une partition. Les consumer groups maintiennent leur progression par partition.

Nous avons utilisé `--from-beginning` et les commandes `kafka-consumer-groups.sh` pour explorer la relecture et l'avancement des offsets. La relecture dépend du groupe, de ses offsets et des messages encore présents selon la rétention ; elle ne signifie pas que tous les événements seront conservés éternellement.

Une nouvelle application ou un groupe sans offset enregistré peut lire des événements historiques disponibles, ce qui doit être pris en compte lors des tests.

---

## 4. Ce que Kafka améliore par rapport au REST synchrone

### Découplage temporel

`OrderService` n'attend plus une réponse HTTP d'`InventoryService` pour pouvoir publier l'événement de commande. Les consommateurs peuvent traiter l'événement de manière asynchrone.

### Découplage des consommateurs

`OrderService` publie `OrderCreated` sans connaître la liste des services qui y réagissent. Les abonnés et les règles de consommation sont configurés indépendamment.

### Historique et replay

Contrairement à une queue classique acquittée dans notre modèle RabbitMQ, Kafka conserve les records selon la rétention du topic. Cela facilite l'inspection, le rejeu et l'ajout de nouveaux consumer groups.

### Parallélisme contrôlé

Les partitions peuvent être réparties entre les instances d'un même groupe. Le parallélisme reste limité par le nombre de partitions et ne garantit pas un ordre global entre tous les messages.

---

## 5. Kafka ne garantit pas à lui seul la cohérence métier

Kafka transporte les événements. Il ne connaît pas les règles de notre e-commerce et ne crée pas une transaction globale couvrant les bases de données d'Order, Inventory et Payment.

Les règles métier restent dans les services, par exemple :

```text
stockReserved = true ET paymentCompleted = true
                  => CONFIRMED
```

ou :

```text
PaymentFailed
    => CANCELLED
    => ReleaseStock si le stock a été réservé
```

L'état des services peut être temporairement différent. Le workflow est éventuellement cohérent et doit être conçu pour converger vers un état métier valide.

---

## 6. Ordre des événements et dépendances causales

Un problème concret a été observé dans les logs : `PaymentFailed` était traité par `OrderService` avant `StockReserved`. La commande était donc annulée alors que `stockReserved` valait encore `false`, et aucune libération immédiate n'était publiée.

La cause était l'architecture initiale : Inventory et Payment consommaient tous deux `OrderCreated` en parallèle. La clé `orderId` ne crée pas d'ordre global entre des topics différents.

Nous avons donc adapté le workflow :

```text
OrderCreated
     |
     v
InventoryService
     |
     | réservation réussie
     v
StockReserved
     |
     v
PaymentService
     |
     +--> PaymentCompleted
     +--> PaymentFailed
```

Cette modification impose la causalité métier nécessaire : le paiement ne commence qu'après l'émission de `StockReserved`.

Une nuance subsiste : même si `StockReserved` est publié avant le résultat du paiement, les consumers de `OrderService` lisent des topics distincts. `OrderService` peut donc encore traiter `PaymentFailed` avant de traiter localement `StockReserved`. Ses handlers doivent couvrir les deux ordres d'arrivée :

1. si `PaymentFailed` arrive après que `stockReserved` a été enregistré, lancer `ReleaseStock` ;
2. si la commande est déjà `CANCELLED` lorsque `StockReserved` est traité, lancer alors la compensation.

Pour une implémentation multi-instance ou fortement concurrente, les transitions de la commande doivent aussi être protégées contre les mises à jour concurrentes en base. Un verrou ou une stratégie optimiste peut être nécessaire ; le simple fait d'utiliser Kafka ne règle pas ce problème.

---

## 7. Gestion des exceptions métier et techniques

### Échecs métier de réservation

`StockNotFoundException` et `InsufficientStockException` représentent, dans notre POC, des échecs métier définitifs de réservation. Inventory les transforme en événement `StockReservationFailed` au lieu de laisser systématiquement le listener échouer et provoquer des redélivrances inutiles.

### Échecs de traitement techniques

Une indisponibilité de PostgreSQL, un timeout réseau ou un broker inaccessible ne doit pas être traité comme une simple insuffisance de stock. Ces erreurs peuvent nécessiter un retry avec une stratégie explicite, puis éventuellement un Dead Letter Topic (DLT) si le traitement échoue toujours.

La configuration complète des retries et du DLT reste un axe d'amélioration du POC.

---

## 8. Compensation / Saga

Quand le stock est réservé puis que le paiement échoue, il faut effectuer une action métier compensatoire :

```text
StockReserved
      |
      v
PaymentFailed
      |
      v
OrderService : CANCELLED
      |
      | ReleaseStock
      v
Kafka : release-stock
      |
      v
InventoryService
      |
      v
stock_reservations.status = RELEASED
```

La compensation n'est pas un rollback de transaction distribuée. C'est une nouvelle action métier, elle-même susceptible d'échouer, de prendre du retard ou d'être reçue plusieurs fois.

Le POC a montré comment libérer une réservation à travers Kafka sans appel HTTP direct entre Order et Inventory. Le système doit néanmoins prévoir la reprise si la libération échoue et rendre cette opération idempotente.

---

## 9. Idempotence : éviter les effets de bord en double

Les messages peuvent être livrés ou traités plusieurs fois, notamment si un service s'arrête après l'écriture en base mais avant que la progression du consumer soit enregistrée comme prévu. Il ne faut donc pas supposer que chaque événement sera toujours traité exactement une fois au niveau métier.

### Paiement

Dans `PaymentService`, un paiement existant est recherché par `orderId` avant de créer un nouvel enregistrement. Le test a montré un log du type :

```text
Paiement déjà enregistré : orderId=53, paymentId=22, status=COMPLETED
```

Une contrainte unique en base sur `order_id` est également recommandée si une commande doit avoir au maximum un paiement dans ce modèle. Le contrôle applicatif seul ne protège pas entièrement deux requêtes concurrentes ; la contrainte en base et la gestion de violation doivent être prises en compte.

Si le modèle métier autorise plusieurs tentatives de paiement, il faudra utiliser une clé d'idempotence plus précise qu'un simple `orderId`.

### Réservation du stock

Le contrôle de la table `stock_reservations` permet de reconnaître une réservation déjà enregistrée. Le traitement doit s'assurer qu'un doublon ne diminue pas une seconde fois `available_quantity` et n’augmente pas de nouveau `reserved_quantity`.

Il faut également définir explicitement ce qui se passe si une commande a déjà échoué, si sa réservation a été libérée, ou si un doublon arrive après une compensation.

### Libération du stock

La libération doit modifier les quantités seulement si la réservation est encore au statut `RESERVED`, puis passer cette réservation à `RELEASED` :

```text
RESERVED -> RELEASED
```

Une nouvelle réception de `ReleaseStock` doit constater `RELEASED` et ne pas créditer le stock une seconde fois.

Ces contrôles protègent les doublons successifs ; une concurrence réelle entre deux consumers nécessite aussi une protection transactionnelle en base. Les tests de doublons et de concurrence doivent faire partie de la validation finale.

---

## 10. Problème du dual write et Transactional Outbox

Un service doit souvent accomplir deux opérations :

```text
1. enregistrer une modification métier dans PostgreSQL ;
2. publier un événement dans Kafka.
```

Sans mécanisme supplémentaire, les deux opérations ne sont pas atomiques :

```text
INSERT Order + COMMIT
        |
        v
publish OrderCreated
        |
        X Kafka indisponible
```

La commande existe mais Inventory ne reçoit pas l'événement. Le problème inverse est également possible si un événement est publié alors que la transaction métier finit par être annulée.

### Principe de l'Outbox

La commande et l'événement à publier sont enregistrés dans la même transaction PostgreSQL :

```text
Transaction PostgreSQL
  +-- INSERT orders
  +-- INSERT outbox_events
          |
          v
        COMMIT
          |
          v
Outbox Publisher
          |
          v
Kafka : order-created
```

Si la transaction est annulée, les deux écritures sont annulées. Si elle réussit alors que Kafka est temporairement indisponible, l'événement reste dans l'Outbox et peut être publié plus tard.

### Polling et latence

L'implémentation de démonstration étudiée utilise un publisher périodique (par exemple `outbox.publisher.fixed-delay-ms=1000`). La publication se fait au prochain passage du publisher, ce qui ajoute une latence dépendant de l'intervalle, du temps d'exécution et de la charge. L'Outbox ne supprime donc pas le traitement asynchrone ; elle rend la publication fiable.

Un intervalle plus court réduit l'attente nominale mais augmente la fréquence des accès à la base. Un déclenchement après commit ou une solution CDC, par exemple Debezium, peut réduire le délai tout en conservant un mécanisme de reprise.

### Limite importante : livraison au moins une fois

Il reste une fenêtre de panne :

```text
1. Kafka accepte l'événement
2. le service s'arrête avant de marquer l'entrée Outbox comme publiée
3. le publisher redémarre et publie à nouveau l'événement
```

L'Outbox ne garantit donc pas à elle seule l'absence de doublons. Les consumers doivent rester idempotents.

**Statut dans ce POC :** le pattern et une première implémentation pour `OrderCreated` ont été étudiés. Il reste à valider explicitement le flux Outbox de bout en bout ainsi que les scénarios d'arrêt/redémarrage, de publication en double et de concurrence entre plusieurs publishers avant de considérer l'implémentation comme prête pour la production. Les publications des autres services devront être migrées vers le même modèle si l'on veut traiter le dual write de façon cohérente à l'échelle du workflow.

---

## 11. Problèmes rencontrés pendant l'intégration

La phase a également permis de diagnostiquer plusieurs erreurs concrètes :

- **Listeners Docker Kafka :** `localhost` dans un conteneur désigne le conteneur lui-même. Les microservices utilisent le listener interne `kafka:29092`, tandis que les outils lancés depuis la machine utilisent `localhost:9092`.
- **`advertised.listeners` :** même si le bootstrap fonctionne, un broker peut annoncer une adresse inaccessible au client. La configuration interne/externe des listeners doit être cohérente.
- **Topic absent :** le publisher échouait avec `Topic ... not present in metadata`. Il a fallu vérifier le topic et déclarer les topics par Spring avec `NewTopic`.
- **Sérialisation :** un `StringSerializer` appliqué à un objet événement provoquait un `ClassCastException`. La clé doit être sérialisée en chaîne et la valeur événement en JSON.
- **Types d'événements multiples :** un seul `spring.json.value.default.type` global ne convient pas automatiquement à plusieurs DTO consommés par des listeners différents. La configuration du type doit correspondre au topic/listener, via les propriétés du listener ou des factories distinctes.
- **Anciens messages :** Kafka conserve des événements selon la rétention. Après modification d'un DTO, un ancien JSON peut ne pas contenir une nouvelle propriété, comme `lines`, ce qui provoquait une `NullPointerException`. Les consommateurs doivent valider les messages et gérer les contrats évolutifs.
- **Publication asynchrone :** `KafkaTemplate.send()` est asynchrone. Il faut observer son résultat via un callback ou un `Future` afin de distinguer l'acceptation du message par Kafka d'un simple retour de la méthode.

---

## 12. Observabilité

L'exécution d'une commande traverse plusieurs microservices et plusieurs topics. Une commande bloquée en `PENDING` nécessite de savoir quel événement a été publié, consommé, échoué ou retardé.

Dans le POC, les logs qui mentionnent `orderId`, topic, partition et offset ont été utiles. Kafka UI peut simplifier l'observation des topics, messages, consumer groups, offsets et lags. La supervision de production nécessiterait en plus des métriques, des alertes, des logs corrélés et éventuellement du tracing distribué.

Il faut éviter de croire qu'un `HTTP 201` ou qu'un identifiant de commande signifie que tout le workflow asynchrone est terminé. Il signifie au mieux que la création initiale a été acceptée ; l'état métier final est atteint plus tard.

---

## 13. Évolution des contrats d'événements

Chaque microservice possède ses propres DTO pour les événements qu'il publie ou consomme. Ils partagent un contrat JSON, mais pas les classes JPA ni les objets internes.

Exemple simplifié :

```json
{
  "orderId": 53,
  "customerId": 123654,
  "totalAmount": 2470.00,
  "lines": [
    {
      "productId": 1234,
      "quantity": 2
    }
  ]
}
```

Une évolution de format peut casser les consommateurs ou rendre certains anciens messages incomplets. Il faut définir des champs obligatoires, des valeurs par défaut, une stratégie de compatibilité, une validation et éventuellement un versionnement explicite des événements.

---

## 14. Limites et sujets restant à renforcer

La phase a permis de construire et d'observer un workflow événementiel fonctionnel. Cela ne signifie pas que toutes les garanties d'une architecture de production ont été implémentées.

Les prochaines améliorations possibles sont :

1. **Valider l'Outbox de bout en bout** et injecter des pannes à chaque frontière : après commit DB, pendant l'envoi Kafka, après acceptation par Kafka mais avant le marquage `published`.
2. **Rendre les transitions d'état concurrentes sûres** en base et tester les événements traités par plusieurs threads ou instances.
3. **Finaliser retries, backoff et DLT** pour les erreurs techniques et les messages impossibles à désérialiser.
4. **Tester les doublons** de `OrderCreated`, `StockReserved`, `PaymentFailed` et `ReleaseStock`, y compris les doublons concurrents.
5. **Protéger la publication sortante de tous les services** par un mécanisme Outbox, ou définir explicitement les garanties de livraison de chaque flux.
6. **Améliorer les contrats** et la validation des payloads JSON, notamment pour les anciens événements.
7. **Documenter les états métier** : `PENDING`, `CONFIRMED`, `CANCELLED`, `stockReserved`, `paymentCompleted` et la façon dont chaque événement modifie ces valeurs.
8. **Ajouter de l'observabilité** : corrélation par `orderId`, métriques de lag, alertes sur les événements Outbox en attente et sur les commandes bloquées.

---

## 15. Bilan des trois phases

### Phase 1 — REST synchrone

```text
OrderService --HTTP--> InventoryService
     |
     +------------HTTP--> PaymentService
```

Problèmes identifiés : couplage fort, latence cumulée, propagation des pannes, timeouts ambigus, absence de transaction distribuée et nécessité de compensations.

### Phase 2 — RabbitMQ

Le broker introduit l'asynchronisme et un routage explicite avec exchanges, queues et bindings. Les problèmes de cohérence distribuée restent présents : doublons, erreurs de consommation, compensation, ordre métier, dual write, observabilité et historique/replay.

### Phase 3 — Kafka

```text
OrderService
     |
     | OrderCreated
     v
Kafka : order-created
     |
     v
InventoryService
     |
     | StockReserved / StockReservationFailed
     v
Kafka
     |
     v
PaymentService (après StockReserved)
     |
     | PaymentCompleted / PaymentFailed
     v
Kafka
     |
     v
OrderService
     |
     | si échec de paiement : ReleaseStock
     v
Kafka : release-stock
     |
     v
InventoryService : réservation RELEASED
```

Kafka apporte un modèle de journal partitionné, des consumer groups indépendants, des offsets et la possibilité de rejouer les événements encore retenus. Il ne fournit pas automatiquement une transaction globale, l'ordre entre plusieurs topics, la cohérence métier, l'idempotence métier ou la compensation.

---

## 16. Conclusion générale

Le principal apprentissage de ce POC est qu'un broker ne supprime pas la complexité distribuée : il en change la forme.

- **REST synchrone** rend les dépendances directes visibles, mais couple le workflow aux réponses et à la disponibilité des services appelés.
- **RabbitMQ** introduit le routage et l'asynchronisme avec exchanges et queues.
- **Kafka** apporte un log d'événements partitionné, des offsets, des consumer groups indépendants et des possibilités de replay plus naturelles.
- **La Saga** reste une responsabilité métier : l'échec d'une étape peut nécessiter une compensation.
- **L'idempotence** est nécessaire pour limiter les effets de doublons.
- **L'Outbox** traite le risque de désynchronisation entre la transaction métier et la publication, mais implique quand même une gestion des doublons, de la reprise et de la supervision.

L'architecture finale n'est donc pas « fiable parce qu'elle utilise Kafka ». Elle devient plus robuste quand les garanties sont définies explicitement : quels événements sont produits, dans quel ordre causal, comment les erreurs sont reprises, comment les doublons sont reconnus, comment les compensations convergent et comment la publication est réconciliée avec les transactions de base.

---

## Vue synthétique du POC

```text
PHASE 1 — REST synchrone
  -> couplage, timeouts, propagation des erreurs, compensation

PHASE 2 — RabbitMQ
  -> exchanges, queues, bindings, ACK/redelivery, asynchronisme

PHASE 3 — Kafka
  -> topics, partitions, clés, offsets, consumer groups, replay
  -> chaîne OrderCreated -> StockReserved -> Payment
  -> PaymentFailed -> ReleaseStock
  -> idempotence des traitements
  -> étude / première implémentation du Transactional Outbox

Sujets de renforcement
  -> valider Outbox sous panne
  -> retries et DLT
  -> concurrence et idempotence robuste
  -> contrats d'événements et observabilité
```
