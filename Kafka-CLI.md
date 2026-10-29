
#list des topic <br>
docker exec -it poc-kafka bash 

/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list <br>

/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order-created <br>

#créer un topic avec 2 partition(2 consumers) <br>
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic order-created --partitions 2 --replication-factor 1 <br>

#envoi des msg via un producer en utilisant une clé <br>
/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-created --property parse.key=true --property key.separator=: <br>
>1001:Order 1001 <br>
>1002:Order 1002 <br>

#Lancer le consumer avec le group <br>
/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-created --group order-service-group



#Vérifier l'assignation d'un group de consumers aux partitions <br>
/opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group order-service-group --members --verbose


/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order-created   <br/>
/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --describe --topic order-created       <br />

 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group inventory-service-group <br />

<p><strong>!! ATTENTION !!</strong> le send est asynchrone. le code peut continuer avec les instrcution après send alors que qlq instants + tard le producer échoue</p>
kafkaTemplate.send( <br/>
        "order-created", <br/>
        event.getOrderId().toString(), <br/>
        event <br/>
); <br/>


docker inspect order-service --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -i kafka  <br/>


## !Important!  ## 
#Erreur métier définitive <br/>
#→ catch <br/>
#→ événement d'échec <br/>
#→ ACK <br/>

#Erreur technique/transitoire <br/>
#→ retry <br/>
#→ éventuellement DLT(Dead Letter Topics) <br/>

#Le fait de déclarer les NewTopic dans plusieurs services n'est pas problématique : si le topic existe déjà, Kafka ne le recrée pas. <br/>
#Il est conseillé de laisser Spring Boot créer le KafkaTemplate comme ca KafkaConfig peut simplement contenir les topics <br/>
#Kafka garantit l'ordre dans une partition d'un même topic, mais pas un ordre global entre des topic differents stock-reserved et payment-failed, le découplage technique ne signifie pas forcément que tous les traitements doivent être parallèles. <br/>
#Course entre les events/services : Le problème n'est pas que PaymentFailed arrive avant StockReserved. Le problème est que notre architecture autorise deux traitements indépendants alors que notre règle métier semble imposer une dépendance entre eux.