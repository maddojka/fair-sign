package com.soroko.fairsign;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrptApi {
    final static String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    final TimeUnit timeUnit;
    final int requestLimit;
    final AtomicInteger requestCounter = new AtomicInteger(0);
    Date lastResetTime = new Date();

    /**
     * Thread safe method which creating a document for entering goods produced in the Russian Federation
     * @param document received document
     * @param signature received signature
     */
    public synchronized void createDocument(Object document, String signature)
            throws IOException, InterruptedException, ParseException {

        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastResetTime.getTime();
        if (timePassed >= timeUnit.toMillis(1)) {
            requestCounter.set(0);
            lastResetTime = new Date(currentTime);
        }

        while (requestCounter.get() >= requestLimit) {
            wait(timeUnit.toMillis(1) - timePassed);
            currentTime = System.currentTimeMillis();
            timePassed = currentTime - lastResetTime.getTime();

            if (timePassed >= timeUnit.toMillis(1)) {
                requestCounter.set(0);
                lastResetTime = new Date(currentTime);
            }
        }

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(API_URL);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(document);

        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Signature", signature);

        CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        String responseString = EntityUtils.toString(responseEntity, "UTF-8");

        requestCounter.incrementAndGet();
        System.out.println("Document creation response: " + responseString);
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Product {
        String certificate_document;
        String certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        String production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Document {
        Description description;
        String doc_id;
        String doc_status;
        String doc_type;
        boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        String production_date;
        String production_type;
        ArrayList<Product> products;
        String reg_date;
        String reg_number;

    }

    public static void main(String[] args)
            throws IOException, InterruptedException, ParseException {

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);

        Object document = new Document();
        String signature = "signature";

        crptApi.createDocument(document, signature);
    }
}