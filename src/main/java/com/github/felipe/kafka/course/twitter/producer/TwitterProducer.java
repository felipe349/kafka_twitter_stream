package com.github.felipe.kafka.course.twitter.producer;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    final Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    private String consumerKey;
    private String consumerSecret;
    private String token;
    private String secret;
    private String topic = "twitter_topic";
    private String keyword = "bitcoin";

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {
        loadProperties();

        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        Client twitterClient = createTwitterClient(msgQueue);
        twitterClient.connect();

        KafkaProducer<String, String> producer = getKafkaProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping application...");
            logger.info("Shutting down twitter client...");
            twitterClient.stop();
            logger.info("Shutting down kafka producer...");
            producer.close();
            logger.info("Done!");
        }));

        while (!twitterClient.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                twitterClient.stop();
            }
            if (msg != null) {
                logger.info(msg);
                producer.send(new ProducerRecord<>(topic, msg), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if (e != null) {
                            logger.error("Something bad hapenned", e);
                        }
                    }
                });
            }
        }
        logger.info("End of Application");
    }

    private KafkaProducer<String, String> getKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<String, String>(properties);
    }


    public Client createTwitterClient(BlockingQueue<String> msgQueue) {
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        List<String> terms = Lists.newArrayList(keyword);
        hosebirdEndpoint.trackTerms(terms);
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        Client hosebirdClient = new ClientBuilder()
                .name("Hosebird-Client-01")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .build();

        return hosebirdClient;
    }

    private void loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        consumerKey = properties.getProperty("consumer.key");
        consumerSecret = properties.getProperty("consumer.secret");
        token = properties.getProperty("twitter.access.token");
        secret = properties.getProperty("twitter.access.token.secret");
    }
}
