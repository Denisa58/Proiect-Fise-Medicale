import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final MinHeap triageQueue = new MinHeap(10);
    private static final BTree medicalDatabase = new BTree(2);

    public static void main(String[] args) throws IOException {
        // Pornim serverul pe portul 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        System.out.println("🚀 Backend-ul Java a pornit! Asteapta cereri de la HTML pe http://localhost:8080");

        //Înregistrare Pacient Nou din formularul HTML
        server.createContext("/api/addPacient", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseQueryParams(body);

                    try {
                        int id = Integer.parseInt(params.get("id"));
                        String name = params.get("nume");
                        String diagnosis = params.get("diagnostic");
                        String symptoms = params.get("simptome");
                        int priorityValue = Integer.parseInt(params.get("prioritate"));

                        Priority priority = Priority.NORMAL;
                        if (priorityValue == 1) priority = Priority.VERY_URGENT;
                        else if (priorityValue == 2) priority = Priority.URGENT;
                        else if (priorityValue == 3) priority = Priority.NORMAL;
                        else if (priorityValue == 4) priority = Priority.LOW;

                        MedicalRecord record = new MedicalRecord(id, name, diagnosis, symptoms, priority);

                        // Sincronizare în structuri
                        triageQueue.insert(record);
                        medicalDatabase.insert(record);

                        String raspuns = "{\"status\":\"success\", \"mesaj\":\"Pacientul a fost adaugat cu succes!\"}";
                        trimiteRaspunsJSON(exchange, raspuns, 200);

                    } catch (Exception e) {
                        String eroare = "{\"status\":\"error\", \"mesaj\":\"Date invalide introduse in formular!\"}";
                        trimiteRaspunsJSON(exchange, eroare, 400);
                    }
                } else {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
            }
        });

        // RUTA 2: Preia Următorul Pacient Urgent (Buton în HTML pentru medic)
        server.createContext("/api/getUrmatorul", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                if (triageQueue.isEmpty()) {
                    trimiteRaspunsJSON(exchange, "{\"status\":\"empty\", \"mesaj\":\"Nu mai sunt pacienti in coada de asteptare!\"}", 200);
                } else {
                    MedicalRecord urmatorul = triageQueue.extractMin();
                    String raspuns = "{\"status\":\"ok\", \"id\":" + urmatorul.id +
                            ", \"nume\":\"" + urmatorul.name +
                            "\", \"diagnostic\":\"" + urmatorul.diagnosis +
                            "\", \"prioritate\":\"" + urmatorul.priority.label + "\"}";
                    trimiteRaspunsJSON(exchange, raspuns, 200);
                }
            }
        });

        //  Extrage toate elementele din MinHeap pentru Monitorizare live
        server.createContext("/api/getPacientiActivi", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                MedicalRecord[] elemente = triageQueue.getHeapElements();

                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < elemente.length; i++) {
                    MedicalRecord r = elemente[i];
                    jsonBuilder.append("{")
                            .append("\"id\":").append(r.id).append(",")
                            .append("\"nume\":\"").append(r.name).append("\",")
                            .append("\"prioritate\":\"").append(r.priority.label).append("\"")
                            .append("}");
                    if (i < elemente.length - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");

                trimiteRaspunsJSON(exchange, jsonBuilder.toString(), 200);
            }
        });

        // Șterge un pacient anume din MinHeap
        server.createContext("/api/stergePacient", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseQueryParams(body);

                    try {
                        int idDeSters = Integer.parseInt(params.get("id"));

                        boolean sters = triageQueue.stergeDupaId(idDeSters);

                        if (sters) {
                            String raspuns = "{\"status\":\"success\", \"message\":\"Fișa #" + idDeSters + " a fost ștearsă din Heap.\"}";
                            trimiteRaspunsJSON(exchange, raspuns, 200);
                        } else {
                            String raspuns = "{\"status\":\"error\", \"message\":\"Fișa nu a fost găsită în Heap!\"}";
                            trimiteRaspunsJSON(exchange, raspuns, 200);
                        }
                    } catch (Exception e) {
                        String eroare = "{\"status\":\"error\", \"message\":\"ID format invalid.\"}";
                        trimiteRaspunsJSON(exchange, eroare, 400);
                    }
                } else {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
            }
        });

        // Căutare după nume exact in B-Tree
        server.createContext("/api/cautaNume", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String numeCautat = params.getOrDefault("nume", "");

                MedicalRecord rezultat = medicalDatabase.search(numeCautat);

                if (rezultat != null) {
                    String raspuns = "{\"status\":\"gasit\", \"id\":" + rezultat.id +
                            ", \"nume\":\"" + rezultat.name +
                            "\", \"diagnostic\":\"" + rezultat.diagnosis +
                            "\", \"simptome\":\"" + rezultat.symptoms + "\"}";
                    trimiteRaspunsJSON(exchange, raspuns, 200);
                } else {
                    trimiteRaspunsJSON(exchange, "{\"status\":\"negasit\", \"mesaj\":\"Pacientul nu exista in B-Tree.\"}", 200);
                }
            }
        });

        // Ștergere simultana dupa String
        server.createContext("/api/stergeBTree", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseQueryParams(body);

                    try {
                        String numeDeSters = params.get("nume");

                        if (numeDeSters != null && !numeDeSters.trim().isEmpty()) {
                            // Căutăm istoricul complet pentru a afla ID-ul asociat numelui
                            MedicalRecord record = medicalDatabase.search(numeDeSters);

                            if (record != null) {
                                int idDeSters = record.id;

                                // Ștergere structurală din B-Tree
                                medicalDatabase.delete(numeDeSters);

                                // Ștergere structurală automată și din MinHeap folosind ID-ul salvat
                                triageQueue.stergeDupaId(idDeSters);

                                String raspuns = "{\"status\":\"success\", \"message\":\"Dosarul și fișa pacientului " + numeDeSters + " (#" + idDeSters + ") au fost eliminate complet din ambele structuri (B-Tree și Min-Heap).\"}";
                                trimiteRaspunsJSON(exchange, raspuns, 200);
                            } else {
                                String raspuns = "{\"status\":\"error\", \"message\":\"Pacientul specificat nu a fost găsit în indexul B-Tree!\"}";
                                trimiteRaspunsJSON(exchange, raspuns, 200);
                            }
                        } else {
                            String raspuns = "{\"status\":\"error\", \"message\":\"Numele primit este invalid.\"}";
                            trimiteRaspunsJSON(exchange, raspuns, 400);
                        }
                    } catch (Exception e) {
                        String eroare = "{\"status\":\"error\", \"message\":\"Eroare la procesarea ștergerii în cascadă.\"}";
                        trimiteRaspunsJSON(exchange, eroare, 400);
                    }
                } else {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
            }
        });

        // Căutare avansată în simptome/diagnostic cu KMP
        server.createContext("/api/cautaSimptom", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                enableCORS(exchange);

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String textCautat = params.getOrDefault("simptom", "");

                StringBuilder pacientiGasiti = new StringBuilder("[");
                int[] count = new int[]{0};

                medicalDatabase.searchWithKMP(medicalDatabase.getRoot(), textCautat, pacientiGasiti, count);
                pacientiGasiti.append("]");

                String raspuns = "{\"status\":\"succes\", \"rezultate\":" + count[0] + ", \"pacienti\":" + pacientiGasiti.toString() + "}";
                trimiteRaspunsJSON(exchange, raspuns, 200);
            }
        });

        server.setExecutor(null);
        server.start();
    }

    private static void trimiteRaspunsJSON(HttpExchange exchange, String json, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void enableCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] idx = pair.split("=");
            if (idx.length == 2) {
                String key = URLDecoder.decode(idx[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(idx[1], StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }
}