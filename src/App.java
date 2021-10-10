import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.json.simple.parser.ParseException;
import java.lang.ClassCastException;
import java.lang.NullPointerException;

public class App {
    static private int SUCCESS_CODE = 200;

    static private int BADREQUEST_CODE = 400;
    static private int NOTFOUND_CODE = 404;
    static private int NOTALLOWED_CODE = 405;
    private static void writeJson(HttpExchange exchange, JSONObject jsonObjectResponse, int STATUS_CODE) throws IOException {
        /*
        Static method to write Json object to exchange
        */
        
        // Setup write for json
        StringWriter outPutString = new StringWriter();
        jsonObjectResponse.writeJSONString(outPutString);
        String jsonText = outPutString.toString();
        
        // Send text json
        exchange.sendResponseHeaders(STATUS_CODE, jsonText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(jsonText.getBytes());
        output.flush();
    }

    private static void writeErrorMessage(HttpExchange exchange, 
            String errorMessage, int STATUS_CODE) throws IOException {
                JSONObject response = new JSONObject();
                response.put("errorMessage", errorMessage);
                writeJson(exchange, response, STATUS_CODE);
    }
    public static void main(String[] args) throws IOException {
        System.out.println("Hello, the api service is running");
        int serverPort = 7845;
        HttpServer apiServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

        // Handle endpoint localhost:port/
        apiServer.createContext("/", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                if  (!path.equals(new String("/"))) {
                    String errorMessage = "URL not found";
                    writeErrorMessage(exchange, errorMessage, NOTFOUND_CODE);
                }
                else { 
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    // Create json object
                    String description = 
                    "This is api service that helps calculate sum of 2 numbers. "
                    + "You should declare 2 numbers within 2 keys only: first & second. "
                    + "This service will not accept other format input data"; 
                    JSONObject response = new JSONObject();
                    response.put("description", description);
                    
                    writeJson(exchange, response, SUCCESS_CODE);
                }
            } else {
                exchange.sendResponseHeaders(NOTALLOWED_CODE, -1);
                String errorMessage = "Method not allowed";
                writeErrorMessage(exchange, errorMessage, NOTALLOWED_CODE);
            }
            exchange.close();
        }));

        // Handle endpoint localhost:port/calc-sum
        apiServer.createContext("/calc-sum", (exchange -> {
            // Only allow post method
            if ("POST".equals(exchange.getRequestMethod())) {
                // Handle body data
                InputStream bodyDataStreamer = exchange.getRequestBody();
                String textBody = new String (bodyDataStreamer.readAllBytes(), StandardCharsets.UTF_8);

                try {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonData = (JSONObject) parser.parse(textBody);
                    Double firstNum = ((Number) jsonData.get("first")).doubleValue();
                    Double secondNum = ((Number) jsonData.get("second")).doubleValue();
                    Double sum = firstNum + secondNum;

                    BigDecimal bd = new BigDecimal(sum).setScale(2, RoundingMode.HALF_UP);
                    Double sumRoundUp = bd.doubleValue();
                    // Create json object 
                    JSONObject response = new JSONObject();
                    response.put("sum", sumRoundUp);
                    
                    writeJson(exchange, response, SUCCESS_CODE);

                } catch (ClassCastException e ) {
                    String errorMessage = "You should parse a number";
                    writeErrorMessage(exchange, errorMessage, BADREQUEST_CODE);
                }  catch (NullPointerException e) {
                    String errorMessage = "You should parse key: first & second only";
                    writeErrorMessage(exchange, errorMessage, BADREQUEST_CODE);
                } catch (ParseException e) {
                    String errorMessage = "Incorrect json format";
                    writeErrorMessage(exchange, errorMessage, BADREQUEST_CODE);
                }
            } else {
                exchange.sendResponseHeaders(NOTALLOWED_CODE, -1);
                String errorMessage = "Method not allowed";
                writeErrorMessage(exchange, errorMessage, NOTALLOWED_CODE);
            }
            exchange.close();
        }));
        apiServer.setExecutor(null);
        apiServer.start();
    }
}
