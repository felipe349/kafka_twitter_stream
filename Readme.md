#Kafka Twitter Stream

##About :
Kafka Twitter Stream is a sample application that reads data
from the Twitter Api filtering tweets in real time using keywords
and stores them on a topic called "twitter_topic". We also have the
consumer side with takes the data from the topic and send it to an 
elasticsearch cluster.

##Requisites :
- Create an application.properties based on the example that's located under
resources of both modules, using your twitter api keys (producer) and 
elasticsearch host (consumer).
- Have an Kafka Broker running with the topic "twitter_topic"
