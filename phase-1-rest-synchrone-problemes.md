# Phase 1 — REST synchrone : problèmes rencontrés

> Mémento de la phase 1 du POC e-commerce avant introduction d'un broker.

## 1. Absence de transaction globale

`@Transactional` protège uniquement la transaction locale d'un service / de sa base de données.

Avec :

```text
OrderService
    ├── HTTP → InventoryService
    └── HTTP → PaymentService
```

un rollback de `OrderService` **n'annule pas** les commits déjà effectués dans Inventory ou Payment.

Conséquence : l'état des services peut devenir incohérent.

---

## 2. Compensations nécessaires

Un workflow peut réussir partiellement :

```text
Order créé
    ↓
Stock réservé        ✓
    ↓
Paiement refusé      ✗
```

Le rollback de `OrderService` ne libère pas le stock.

Il faut donc une action métier inverse, par exemple :

```text
PaymentFailed
    ↓
releaseReservation()
```

Cette compensation peut elle-même échouer.

---

## 3. Timeouts et ambiguïté

Un appel HTTP peut expirer alors que le traitement distant a réellement réussi :

```text
OrderService → PaymentService
                    ↓
               paiement réussi
                    ↓
              réponse perdue
                    ↓
                 TIMEOUT
```

OrderService ne sait plus forcément si le paiement a été effectué.

Un simple retry peut alors provoquer un double traitement (ex. double paiement).

---

## 4. Gestion des erreurs distribuées

Chaque service peut retourner des erreurs différentes :

- `4xx` : erreur métier / requête invalide ;
- `5xx` : erreur technique distante ;
- timeout ;
- connexion refusée ;
- service indisponible.

Une erreur distante doit être correctement propagée ou transformée par le service appelant.

Exemple : `RestClient` lève une exception pour une réponse HTTP `4xx/5xx`; elle n'est pas automatiquement désérialisée en DTO de succès.

---

## 5. Couplage fort entre services

Avec des appels REST directs, `OrderService` connaît :

- l'URL du service distant ;
- ses endpoints ;
- ses contrats HTTP ;
- ses réponses et erreurs ;
- ses contraintes de disponibilité.

Le workflow devient fortement dépendant de la disponibilité et du comportement des services appelés.

---

## 6. Latence cumulée

Les appels sont séquentiels :

```text
Client
  ↓
OrderService
  ↓ HTTP
InventoryService
  ↓
OrderService
  ↓ HTTP
PaymentService
  ↓
OrderService
```

Le temps de réponse de l'opération dépend de plusieurs appels réseau.

Plus le workflow contient de services, plus la latence globale peut augmenter.

---

## 7. Propagation des pannes

Si un service distant est lent ou indisponible :

```text
PaymentService DOWN
        ↓
OrderService attend
        ↓
timeout / threads occupés
        ↓
dégradation possible de OrderService
```

Les défaillances d'un service peuvent donc se propager aux services qui l'appellent.

---

## 8. Retries potentiellement dangereux

Face à un timeout, on peut vouloir retenter :

```text
payment()
   ↓ timeout
retry payment()
```

Mais si la première requête a en réalité réussi, le retry peut exécuter l'opération une deuxième fois.

Cela impose de réfléchir à **l'idempotence** des opérations distribuées.

---

## 9. Orchestration de plus en plus complexe

Avec plusieurs étapes :

```text
Order
  ↓
Inventory
  ↓
Payment
  ↓
Shipping
  ↓
Notification
```

le service orchestrateur doit gérer :

- l'ordre des appels ;
- les erreurs ;
- les timeouts ;
- les retries ;
- les compensations ;
- les états intermédiaires ;
- les cas de reprise.

Le workflow devient progressivement une machine à états distribuée.

---

## 10. Cohérence éventuelle des états

Chaque service possède sa propre base de données.

Il n'existe pas de transaction ACID unique couvrant :

```text
Order DB
Inventory DB
Payment DB
```

Il faut donc accepter ou gérer des situations temporaires où les états des services ne correspondent pas immédiatement.

---

## Synthèse

La phase REST synchrone nous a permis d'identifier :

```text
✓ absence de transaction globale
✓ compensations nécessaires
✓ timeouts et ambiguïté des résultats
✓ erreurs distribuées
✓ couplage fort
✓ latence cumulée
✓ propagation des pannes
✓ retries potentiellement dangereux
✓ orchestration complexe
✓ problèmes de cohérence inter-services
```

### Question pour la phase suivante

> Comment un broker de messages (RabbitMQ, ActiveMQ, Kafka...) peut-il réduire ces problèmes, et quels nouveaux problèmes introduit-il ?

