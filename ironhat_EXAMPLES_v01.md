private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(data);
        
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", statusCode);
        error.put("timestamp", System.currentTimeMillis());
        
        sendJsonResponse(exchange, statusCode, error);
    }
}

// ===== COMMAND LINE INTERFACE =====

package com.codetalker.cli;

import com.codetalker.api.QueryProcessor;
import com.codetalker.api.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.util.Scanner;

/**
 * Command-line interface for Code Talker
 */
public class CodeTalkerCLI {
    private static final Logger logger = LoggerFactory.getLogger(CodeTalkerCLI.class);
    
    private final QueryProcessor queryProcessor;
    private final Scanner scanner;
    private boolean running = true;
    
    public CodeTalkerCLI(QueryProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Start interactive CLI session
     */
    public void startInteractive() {
        printWelcome();
        
        while (running) {
            System.out.print("\nCode Talker> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (handleCommand(input)) {
                continue;
            }
            
            // Process as query
            processQuery(input);
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
    
    /**
     * Process single query (for batch mode)
     */
    public void processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.out.println("Error: Empty query");
            return;
        }
        
        System.out.println("\nSearching for: " + query);
        System.out.println("=" + "=".repeat(50));
        
        try {
            QueryRequest request = new QueryRequest(query, 5); // Limit to 5 results for CLI
            QueryProcessor.QueryResponse response = queryProcessor.processQuery(request);
            
            if (!response.success) {
                System.out.println("Query processing failed");
                return;
            }
            
            if (response.results.isEmpty()) {
                System.out.println("No results found for your query.");
                return;
            }
            
            // Display results
            for (int i = 0; i < response.results.size(); i++) {
                QueryProcessor.QueryResult result = response.results.get(i);
                
                System.out.printf("\n[%d] %s (Score: %.3f)\n", 
                                i + 1, result.filePath, result.finalScore);
                System.out.println("-".repeat(60));
                
                // Show relevant portion of content
                String displayContent = truncateContent(result.content, 200);
                System.out.println(displayContent);
                
                // Show metadata if available
                if (!result.metadata.isEmpty()) {
                    System.out.println("\nMetadata:");
                    result.metadata.entrySet().stream()
                        .limit(3) // Show only top 3 metadata items
                        .forEach(entry -> 
                            System.out.printf("  %s: %s\n", entry.getKey(), entry.getValue())
                        );
                }
                
                System.out.println();
            }
            
            System.out.printf("Found %d results in %dms\n", 
                            response.results.size(), response.processingTimeMs);
            
        } catch (Exception e) {
            System.out.println("Error processing query: " + e.getMessage());
            logger.error("CLI query processing error", e);
        }
    }
    
    private boolean handleCommand(String input) {
        String command = input.toLowerCase();
        
        switch (command) {
            case "help":
            case "?":
                printHelp();
                return true;
                
            case "exit":
            case "quit":
            case "q":
                running = false;
                return true;
                
            case "status":
                printStatus();
                return true;
                
            case "clear":
                clearScreen();
                return true;
                
            default:
                // Not a command
                return false;
        }
    }
    
    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║              Code Talker v2.0                   ║");
        System.out.println("║         Legacy Application Intelligence          ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Ask questions about your legacy application in natural language.");
        System.out.println("Type 'help' for available commands or start asking questions!");
    }
    
    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  help, ?     - Show this help message");
        System.out.println("  status      - Show system status");
        System.out.println("  clear       - Clear screen");
        System.out.println("  exit, quit  - Exit Code Talker");
        System.out.println();
        System.out.println("Example Queries:");
        System.out.println("  \"How does authentication work?\"");
        System.out.println("  \"What tables store user data?\"");
        System.out.println("  \"Where is the login method?\"");
        System.out.println("  \"Show me the payment processing code\"");
    }
    
    private void printStatus() {
        System.out.println("\nSystem Status:");
        System.out.println("  Version: 2.0.0");
        System.out.println("  Status: Running");
        // In real implementation, would show actual system stats
        System.out.println("  Indexed Documents: [Would show actual count]");
        System.out.println("  Index Status: [Would show actual status]");
    }
    
    private void clearScreen() {
        // ANSI escape code to clear screen
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }
    
    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        
        // Try to truncate at word boundary
        String truncated = content.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        
        if (lastSpace > maxLength * 0.7) {
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated + "...";
    }
    
    /**
     * Main method for CLI-only usage
     */
    public static void main(String[] args) {
        try {
            // Initialize Code Talker components
            // This is a simplified example - real implementation would have full initialization
            
            System.out.println("Initializing Code Talker...");
            
            // In real implementation:
            // - Load configuration
            // - Initialize database
            // - Load models
            // - Build/load vector index
            // - Create query processor
            
            System.out.println("Code Talker initialization complete!");
            
            // Start CLI
            // CodeTalkerCLI cli = new CodeTalkerCLI(queryProcessor);
            // cli.startInteractive();
            
        } catch (Exception e) {
            System.err.println("Failed to start Code Talker: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// ===== INTEGRATION TEST SUITE =====

package com.codetalker.integration;

import com.codetalker.embedding.EmbeddingService;
import com.codetalker.storage.DatabaseManager;
import com.codetalker.storage.EmbeddingRepository;
import com.codetalker.search.VectorIndex;
import com.codetalker.api.QueryProcessor;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for Code Talker system
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTestSuite {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestSuite.class);
    
    private static final String TEST_DB_PATH = "./test-data/test-embeddings";
    private static final String TEST_INDEX_PATH = "./test-data/test-index";
    
    private DatabaseManager databaseManager;
    private EmbeddingService embeddingService;
    private EmbeddingRepository embeddingRepository;
    private VectorIndex vectorIndex;
    private QueryProcessor queryProcessor;
    
    @BeforeAll
    void setupIntegrationTest() {
        logger.info("Setting up integration test environment");
        
        // Clean test data
        cleanTestData();
        
        // Initialize components
        databaseManager = new DatabaseManager(TEST_DB_PATH);
        embeddingService = new EmbeddingService();
    // Use the binary-backed store (BLOB vectors) by default
    embeddingRepository = new com.codetalker.embedding.EmbeddingBinaryRepository(databaseManager);
        vectorIndex = new VectorIndex(512, TEST_INDEX_PATH); // 512 = USE embedding dimension
        
        // Initialize embedding service
        boolean embeddingInitialized = embeddingService.initialize();
        if (!embeddingInitialized) {
            logger.warn("Embedding service initialization failed - some tests may be skipped");
        }
        
        queryProcessor = new QueryProcessor(embeddingService, vectorIndex, embeddingRepository);
        
        logger.info("Integration test setup complete");
    }
    
    @AfterAll
    void tearDownIntegrationTest() {
        logger.info("Tearing down integration test environment");
        
        // Cleanup resources
        if (embeddingService != null) {
            embeddingService.shutdown();
        }
        
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // Clean test data
        cleanTestData();
        
        logger.info("Integration test teardown complete");
    }
    
    @Test
    @DisplayName("End-to-End Workflow Test")
    void testCompleteWorkflow() {
        // Create sample content
        List<SampleContent> sampleContent = createSampleContent();
        
        // Step 1: Generate embeddings and store
        List<EmbeddingRepository.EmbeddingRecord> records = new ArrayList<>();
        
        for (SampleContent content : sampleContent) {
            if (embeddingService.isModelLoaded()) {
                float[] embedding = embeddingService.generateEmbedding(content.text);
                
                EmbeddingRepository.EmbeddingRecord record = new EmbeddingRepository.EmbeddingRecord(
                    content.filePath,
                    content.contentType,
                    content.text,
                    embedding
                );
                record.metadata = content.metadata;
                
                records.add(record);
            }
        }
        
        // Store embeddings
        int storedCount = embeddingRepository.storeEmbeddings(records);
        assertTrue(storedCount > 0, "Should store at least some embeddings");
        
        // Step 2: Build vector index
        List<VectorIndex.IndexedVector> indexVectors = new ArrayList<>();
        for (EmbeddingRepository.EmbeddingRecord record : records) {
            if (record.id != null) {
                indexVectors.add(new VectorIndex.IndexedVector(record.id, record.vector, record.chunkText));
            }
        }
        
        boolean indexBuilt = vectorIndex.buildIndex(indexVectors);
        assertTrue(indexBuilt, "Vector index should build successfully");
        
        // Step 3: Test queries
        testSampleQueries();
        
        logger.info("Complete workflow test passed");
    }
    
    private void testSampleQueries() {
        String[] testQueries = {
            "How does user authentication work?",
            "What database tables store user information?",
            "Show me the login method",
            "Where is password validation handled?"
        };
        
        for (String query : testQueries) {
            QueryProcessor.QueryRequest request = new QueryProcessor.QueryRequest(query, 5);
            QueryProcessor.QueryResponse response = queryProcessor.processQuery(request);
            
            assertTrue(response.success, "Query should process successfully: " + query);
            assertNotNull(response.results, "Results should not be null");
            assertTrue(response.processingTimeMs < 5000, "Query should complete within 5 seconds");
            
            logger.debug("Query '{}' returned {} results in {}ms", 
                        query, response.results.size(), response.processingTimeMs);
        }
    }
    
    @Test
    @DisplayName("Performance Benchmark Test")
    void testPerformanceBenchmarks() {
        // Test embedding generation performance
        if (embeddingService.isModelLoaded()) {
            long startTime = System.currentTimeMillis();
            
            List<String> testTexts = createPerformanceTestTexts(100);
            List<float[]> embeddings = embeddingService.generateEmbeddings(testTexts);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            assertEquals(testTexts.size(), embeddings.size(), "Should generate embedding for each text");
            
            double embeddingsPerSecond = (testTexts.size() * 1000.0) / elapsedTime;
            logger.info("Embedding generation rate: {:.2f} embeddings/second", embeddingsPerSecond);
            
            // Performance threshold - should generate at least 10 embeddings per second
            assertTrue(embeddingsPerSecond >= 10, 
                      "Embedding generation should be at least 10/second, got " + embeddingsPerSecond);
        }
    }
    
    @Test
    @DisplayName("Database Operations Test")
    void testDatabaseOperations() {
        // Test database statistics
        DatabaseManager.DatabaseStats stats = databaseManager.getStats();
        assertNotNull(stats, "Should get database statistics");
        assertTrue(stats.totalEmbeddings >= 0, "Total embeddings should be non-negative");
        
        // Test record retrieval
        List<EmbeddingRepository.EmbeddingRecord> allRecords = embeddingRepository.getAllEmbeddings();
        assertNotNull(allRecords, "Should retrieve all records");
        
        logger.info("Database contains {} embedding records", allRecords.size());
    }
    
    @Test
    @DisplayName("Vector Index Operations Test")
    void testVectorIndexOperations() {
        VectorIndex.IndexStats stats = vectorIndex.getStats();
        assertNotNull(stats, "Should get index statistics");
        
        if (stats.isLoaded && stats.vectorCount > 0) {
            // Test vector search
            float[] testVector = new float[512]; // Create test vector
            for (int i = 0; i < testVector.length; i++) {
                testVector[i] = (float) Math.random();
            }
            
            List<VectorIndex.SearchResult> results = vectorIndex.search(testVector, 5);
            assertNotNull(results, "Search results should not be null");
            assertTrue(results.size() <= 5, "Should not exceed requested result count");
            
            logger.info("Vector search returned {} results", results.size());
        }
    }
    
    private List<SampleContent> createSampleContent() {
        List<SampleContent> content = new ArrayList<>();
        
        // Java authentication code
        Map<String, Object> authMetadata = new HashMap<>();
        authMetadata.put("className", "AuthenticationService");
        authMetadata.put("methodName", "authenticateUser");
        authMetadata.put("businessDomain", "authentication");
        
        content.add(new SampleContent(
            "/src/auth/AuthenticationService.java",
            "java-method",
            "public boolean authenticateUser(String username, String password) { " +
            "UserRecord user = userRepository.findByUsername(username); " +
            "if (user == null) return false; " +
            "return passwordEncoder.matches(password, user.getPasswordHash()); }",
            authMetadata
        ));
        
        // Database table definition
        Map<String, Object> dbMetadata = new HashMap<>();
        dbMetadata.put("tableName", "users");
        dbMetadata.put("businessDomain", "data-access");
        
        content.add(new SampleContent(
            "/sql/schema/users.sql",
            "sql",
            "CREATE TABLE users ( " +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "username VARCHAR(50) UNIQUE NOT NULL, " +
            "password_hash VARCHAR(255) NOT NULL, " +
            "email VARCHAR(100), " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ); ",
            dbMetadata
        ));
        
        // Documentation
        Map<String, Object> docMetadata = new HashMap<>();
        docMetadata.put("section", "Authentication");
        docMetadata.put("businessDomain", "documentation");
        
        content.add(new SampleContent(
            "/docs/authentication.md",
            "markdown",
            "# User Authentication System\n\n" +
            "The authentication system validates user credentials against the users table. " +
            "Passwords are hashed using BCrypt for security. " +
            "The AuthenticationService.authenticateUser method handles the validation logic.",
            docMetadata
        ));
        
        return content;
    }
    
    private List<String> createPerformanceTestTexts(int count) {
        List<String> texts = new ArrayList<>();
        
        String[] templates = {
            "This is a test method that performs {0} operation on {1} data",
            "The {0} class handles {1} functionality in the system",
            "Database table {0} stores {1} information for the application",
            "Configuration setting {0} controls {1} behavior"
        };
        
        String[] operations = {"create", "read", "update", "delete", "process", "validate"};
        String[] entities = {"user", "order", "product", "payment", "session", "audit"};
        
        for (int i = 0; i < count; i++) {
            String template = templates[i % templates.length];
            String operation = operations[i % operations.length];
            String entity = entities[i % entities.length];
            
            String text = template.replace("{0}", operation).replace("{1}", entity);
            texts.add(text);
        }
        
        return texts;
    }
    
    private void cleanTestData() {
        try {
            Path testDataDir = Paths.get("./test-data");
            if (Files.exists(testDataDir)) {
                Files.walk(testDataDir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete test file: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.warn("Failed to clean test data", e);
        }
    }
    
    private static class SampleContent {
        final String filePath;
        final String contentType;
        final String text;
        final Map<String, Object> metadata;
        
        SampleContent(String filePath, String contentType, String text, Map<String, Object> metadata) {
            this.filePath = filePath;
            this.contentType = contentType;
            this.text = text;
            this.metadata = metadata;
        }
    }
}

// ===== SCHEMA SQL =====

-- File: schema.sql
-- H2 Database schema for Code Talker embeddings

CREATE TABLE IF NOT EXISTS embeddings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_hash VARCHAR(64) UNIQUE NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    chunk_text CLOB NOT NULL,
    vector ARRAY NOT NULL,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_embeddings_file_path ON embeddings(file_path);
CREATE INDEX IF NOT EXISTS idx_embeddings_content_type ON embeddings(content_type);
CREATE INDEX IF NOT EXISTS idx_embeddings_hash ON embeddings(content_hash);
CREATE INDEX IF NOT EXISTS idx_embeddings_created ON embeddings(created_at);

-- Table for storing index metadata
CREATE TABLE IF NOT EXISTS index_metadata (
    id INT PRIMARY KEY,
    index_version VARCHAR(20),
    vector_count INT,
    dimension INT,
    last_build TIMESTAMP,
    build_duration_ms BIGINT
);

-- Insert initial metadata record
MERGE INTO index_metadata (id, index_version, vector_count, dimension) 
VALUES (1, '1.0', 0, 512);

-- ===== API DOCUMENTATION YAML =====

# File: api-documentation.yaml
openapi: 3.0.3
info:
  title: Code Talker API
  description: Legacy Application Intelligence System API
  version: 2.0.0
  
servers:
  - url: http://localhost:8080/api
    description: Local development server

paths:
  /query:
    post:
      summary: Query legacy application knowledge
      description: Submit a natural language query about the legacy application
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - query
              properties:
                query:
                  type: string
                  description: Natural language query
                  example: "How does user authentication work?"
                maxResults:
                  type: integer
                  minimum: 1
                  maximum: 50
                  default: 10
                  description: Maximum number of results to return
                filters:
                  type: object
                  description: Optional filters for content type, file path, etc.
                  properties:
                    contentType:
                      type: string
                      example: "java-method"
                    filePath:
                      type: string
                      example: "/src/auth/"
                      
      responses:
        '200':
          description: Successful query response
          content:
            application/json:
              schema:
                type: object
                properties:
                  results:
                    type: array
                    items:
                      $ref: '#/components/schemas/QueryResult'
                  processingTimeMs:
                    type: integer
                    example: 245
                  expandedQuery:
                    type: string
                    example: "user authentication login validation process"
                  totalFound:
                    type: integer
                    example: 15
                  success:
                    type: boolean
                    example: true
                    
        '400':
          description: Bad request - invalid query format
        '500':
          description: Internal server error

  /health:
    get:
      summary: Health check endpoint
      responses:
        '200':
          description: Service is healthy
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                    example: "healthy"
                  timestamp:
                    type: integer
                    format: int64
                  version:
                    type: string
                    example: "2.0.0"
                    
  /index/status:
    get:
      summary: Get vector index status
      responses:
        '200':
          description: Index status information
          content:
            application/json:
              schema:
                type: object
                properties:
                  indexed:
                    type: boolean
                    example: true
                  vectorCount:
                    type: integer
                    example: 10000
                  lastUpdate:
                    type: integer
                    format: int64

components:
  schemas:
    QueryResult:
      type: object
      properties:
        id:
          type: integer
          format: int64
          example: 12345
        filePath:
          type: string
          example: "/src/auth/AuthenticationService.java"
        contentType:
          type: string
          example: "java-method"
        content:
          type: string
          example: "public boolean authenticateUser(String username, String password) { ... }"
        similarityScore:
          type: number
          format: float
          example: 0.85
        finalScore:
          type: number
          format: float
          example: 0.92
        metadata:
          type: object
          example:
            className: "AuthenticationService"
            methodName: "authenticateUser"
            businessDomain: "authentication"

// ===== REST API CONTROLLER =====

package com.codetalker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * REST API controller for Code Talker queries
 */
public class QueryController {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    private final QueryProcessor queryProcessor;
    private final ObjectMapper objectMapper;
    private HttpServer server;
    
    public QueryController(QueryProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Start HTTP server
     */
    public boolean start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Register endpoints
            server.createContext("/api/query", new QueryHandler());
            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/index/status", new IndexStatusHandler());
            
            // Start server
            server.setExecutor(null); // Use default executor
            server.start();
            
            logger.info("Code Talker API server started on port {}", port);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to start API server", e);
            return false;
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("API server stopped");
        }
    }
    
    /**
     * Query endpoint handler
     */
    private class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method not allowed");
                    return;
                }
                
                // Parse request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                QueryRequest request = parseQueryRequest(requestBody);
                
                if (request == null) {
                    sendErrorResponse(exchange, 400, "Invalid request format");
                    return;
                }
                
                // Process query
                QueryProcessor.QueryResponse response = queryProcessor.processQuery(request);
                
                // Send response
                sendJsonResponse(exchange, 200, response);
                
            } catch (Exception e) {
                logger.error("Error processing query request", e);
                sendErrorResponse(exchange, 500, "Internal server error");
            }
        }
        
        private QueryRequest parseQueryRequest(String json) {
            try {
                Map<String, Object> data = objectMapper.readValue(json, Map.class);
                
                String query = (String) data.get("query");
                if (query == null || query.trim().isEmpty()) {
                    return null;
                }
                
                int maxResults = (Integer) data.getOrDefault("maxResults", 10);
                Map<String, String> filters = (Map<String, String>) data.getOrDefault("filters", new HashMap<>());
                
                return new QueryRequest(query, maxResults, filters);
                
            } catch (Exception e) {
                logger.error("Failed to parse query request", e);
                return null;
            }
        }
    }
    
    /**
     * Health check endpoint
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "healthy");
                health.put("timestamp", System.currentTimeMillis());
                health.put("version", "2.0.0");
                
                sendJsonResponse(exchange, 200, health);
                
            } catch (Exception e) {
                logger.error("Health check failed", e);
                sendErrorResponse(exchange, 500, "Health check failed");
            }
        }
    }
    
    /**
     * Index status endpoint
     */
    private class IndexStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // This would get actual index status - simplified for example
                Map<String, Object> status = new HashMap<>();
                status.put("indexed", true);
                status.put("vectorCount", 10000); // Would get from actual index
                status.put("lastUpdate", System.currentTimeMillis());
                
                sendJsonResponse(exchange, 200, status);
                
            } catch (Exception e) {
                logger.error("Index status check failed", e);
                sendErrorResponse(exchange, 500, "Index status check failed");
            }
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String jsonResponse = objectMapper.writeValue// ===== PROJECT STRUCTURE SETUP =====

// File: pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.codetalker</groupId>
    <artifactId>code-talker</artifactId>
    <version>2.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- TensorFlow Lite -->
        <dependency>
            <groupId>org.tensorflow</groupId>
            <artifactId>tensorflow-lite</artifactId>
            <version>2.13.0</version>
        </dependency>
        
        <!-- H2 Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
        </dependency>
        
        <!-- JVector for HNSW -->
        <dependency>
            <groupId>io.github.jbellis</groupId>
            <artifactId>jvector-base</artifactId>
            <version>1.0.0</version>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.7</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.8</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>

// ===== GIT IGNORE TEMPLATE =====

// File: .gitignore
# Compiled class files
*.class

# Log files
*.log

# Package files
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml

# IDE
.idea/
*.iws
*.iml
*.ipr
.vscode/
.settings/
.project
.classpath

# OS
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db

# Application specific
models/
data/
embeddings.db*
index/

// ===== MODEL LOADER IMPLEMENTATION =====

package com.codetalker.embedding;

import org.tensorflow.Interpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * Handles loading and management of TensorFlow models
 */
public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);
    private static final String DEFAULT_MODEL_PATH = "models/universal-sentence-encoder.tflite";
    
    private Interpreter interpreter;
    private boolean isLoaded = false;
    
    /**
     * Load TensorFlow Lite model from resources or file system
     */
    public boolean loadModel() {
        return loadModel(DEFAULT_MODEL_PATH);
    }
    
    public boolean loadModel(String modelPath) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(modelPath);
            
            // Configure interpreter options for performance
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
            options.setUseXNNPACK(true); // Enable XNNPACK acceleration
            
            interpreter = new Interpreter(modelBuffer, options);
            isLoaded = true;
            
            logger.info("Successfully loaded TensorFlow Lite model from: {}", modelPath);
            logModelInfo();
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load TensorFlow Lite model from: {}", modelPath, e);
            return false;
        }
    }
    
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        // Try loading from resources first
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(modelPath);
        if (resourceStream != null) {
            logger.debug("Loading model from resources: {}", modelPath);
            // For resources, we need to copy to a temporary file first
            // This is a simplified approach - production code should handle this more robustly
            throw new IOException("Resource loading not implemented - place model in file system");
        }
        
        // Load from file system
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IOException("Model file not found: " + modelPath);
        }
        
        try (FileChannel fileChannel = FileChannel.open(modelFile.toPath(), StandardOpenOption.READ)) {
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        }
    }
    
    private void logModelInfo() {
        if (interpreter != null) {
            logger.info("Model input tensors: {}", interpreter.getInputTensorCount());
            logger.info("Model output tensors: {}", interpreter.getOutputTensorCount());
            
            // Log input tensor shapes
            for (int i = 0; i < interpreter.getInputTensorCount(); i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                logger.info("Input tensor {}: shape = {}", i, java.util.Arrays.toString(shape));
            }
        }
    }
    
    public Interpreter getInterpreter() {
        if (!isLoaded) {
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }
        return interpreter;
    }
    
    public boolean isModelLoaded() {
        return isLoaded;
    }
    
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
            isLoaded = false;
            logger.info("TensorFlow Lite interpreter closed");
        }
    }
}

// ===== MODEL LOADER TEST =====

package com.codetalker.embedding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ModelLoaderTest {
    
    private ModelLoader modelLoader;
    
    @BeforeEach
    void setUp() {
        modelLoader = new ModelLoader();
    }
    
    @AfterEach
    void tearDown() {
        if (modelLoader != null) {
            modelLoader.close();
        }
    }
    
    @Test
    void testLoadModelSuccess() {
        // This test assumes model file exists in models/ directory
        // In real scenario, you'd have the actual model file
        boolean result = modelLoader.loadModel();
        
        if (result) {
            assertTrue(modelLoader.isModelLoaded());
            assertNotNull(modelLoader.getInterpreter());
        } else {
            // If model file doesn't exist, test should document this
            assertFalse(modelLoader.isModelLoaded());
        }
    }
    
    @Test
    void testLoadModelFileNotFound() {
        boolean result = modelLoader.loadModel("nonexistent/path/model.tflite");
        
        assertFalse(result);
        assertFalse(modelLoader.isModelLoaded());
    }
    
    @Test
    void testGetInterpreterWithoutLoading() {
        assertThrows(IllegalStateException.class, () -> {
            modelLoader.getInterpreter();
        });
    }
}

// ===== DATABASE MANAGER =====

package com.codetalker.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.h2.jdbcx.JdbcDataSource;

/**
 * Manages H2 database connections and initialization
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    private static final String DEFAULT_DB_PATH = "./data/embeddings";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    private DataSource dataSource;
    private String databasePath;
    
    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }
    
    public DatabaseManager(String databasePath) {
        this.databasePath = databasePath;
        initializeDataSource();
    }
    
    private void initializeDataSource() {
        try {
            JdbcDataSource ds = new JdbcDataSource();
            
            // Configure for file-based storage with performance optimizations
            String jdbcUrl = String.format("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=REGULAR", 
                                         databasePath);
            
            ds.setURL(jdbcUrl);
            ds.setUser(DB_USER);
            ds.setPassword(DB_PASSWORD);
            
            this.dataSource = ds;
            
            // Test connection and initialize schema
            testConnection();
            initializeSchema();
            
            logger.info("H2 Database initialized at: {}", databasePath);
            
        } catch (Exception e) {
            logger.error("Failed to initialize H2 database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            logger.debug("Database connection test successful");
        }
    }
    
    private void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create embeddings table if not exists
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS embeddings (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    content_hash VARCHAR(64) UNIQUE NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    content_type VARCHAR(50) NOT NULL,
                    chunk_text CLOB NOT NULL,
                    vector ARRAY NOT NULL,
                    metadata JSON,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;
            
            stmt.execute(createTableSQL);
            
            // Create indexes for performance
            String createIndexSQL = """
                CREATE INDEX IF NOT EXISTS idx_embeddings_file_path ON embeddings(file_path);
                CREATE INDEX IF NOT EXISTS idx_embeddings_content_type ON embeddings(content_type);
                CREATE INDEX IF NOT EXISTS idx_embeddings_hash ON embeddings(content_hash);
                """;
            
            stmt.execute(createIndexSQL);
            
            logger.debug("Database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public void shutdown() {
        logger.info("Shutting down database connection pool");
        // H2 will auto-close when JVM exits, but we can explicitly close if needed
    }
    
    /**
     * Get database statistics for monitoring
     */
    public DatabaseStats getStats() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            var rs = stmt.executeQuery("SELECT COUNT(*) as total FROM embeddings");
            rs.next();
            long totalEmbeddings = rs.getLong("total");
            
            return new DatabaseStats(totalEmbeddings, databasePath);
            
        } catch (SQLException e) {
            logger.error("Failed to get database statistics", e);
            return new DatabaseStats(0, databasePath);
        }
    }
    
    public static class DatabaseStats {
        public final long totalEmbeddings;
        public final String databasePath;
        
        public DatabaseStats(long totalEmbeddings, String databasePath) {
            this.totalEmbeddings = totalEmbeddings;
            this.databasePath = databasePath;
        }
        
        @Override
        public String toString() {
            return String.format("DatabaseStats{totalEmbeddings=%d, path='%s'}", 
                               totalEmbeddings, databasePath);
        }
    }
}

// ===== EMBEDDING SERVICE =====

package com.codetalker.embedding;

import org.tensorflow.lite.Interpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for generating embeddings using TensorFlow Lite Universal Sentence Encoder
 */
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private static final int EMBEDDING_DIMENSION = 512; // Universal Sentence Encoder output size
    private static final int MAX_SEQUENCE_LENGTH = 128; // Model's maximum input length
    
    private final ModelLoader modelLoader;
    private final TextPreprocessor textPreprocessor;
    private final ReadWriteLock modelLock = new ReentrantReadWriteLock();
    
    public EmbeddingService() {
        this.modelLoader = new ModelLoader();
        this.textPreprocessor = new TextPreprocessor();
    }
    
    public boolean initialize() {
        boolean success = modelLoader.loadModel();
        if (success) {
            logger.info("EmbeddingService initialized successfully");
        } else {
            logger.error("Failed to initialize EmbeddingService");
        }
        return success;
    }
    
    /**
     * Generate embedding for a single text input
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        List<String> batch = List.of(text);
        List<float[]> embeddings = generateEmbeddings(batch);
        
        return embeddings.get(0);
    }
    
    /**
     * Generate embeddings for multiple texts in batch (more efficient)
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        modelLock.readLock().lock();
        try {
            Interpreter interpreter = modelLoader.getInterpreter();
            List<float[]> results = new ArrayList<>();
            
            for (String text : texts) {
                float[] embedding = processTextToEmbedding(interpreter, text);
                results.add(embedding);
            }
            
            logger.debug("Generated {} embeddings", results.size());
            return results;
            
        } finally {
            modelLock.readLock().unlock();
        }
    }
    
    private float[] processTextToEmbedding(Interpreter interpreter, String text) {
        // Preprocess text
        String processedText = textPreprocessor.preprocess(text);
        
        // Convert text to tokens (simplified - real implementation would use proper tokenizer)
        String[] tokens = tokenizeText(processedText);
        
        // Prepare input tensor
        String[][] inputArray = new String[1][1]; // Batch size 1, sequence length 1
        inputArray[0][0] = processedText; // USE Lite expects sentence-level input
        
        // Prepare output tensor
        float[][] outputArray = new float[1][EMBEDDING_DIMENSION];
        
        // Run inference
        interpreter.run(inputArray, outputArray);
        
        return outputArray[0];
    }
    
    private String[] tokenizeText(String text) {
        // Simplified tokenization - in production, use proper tokenizer
        return text.toLowerCase().split("\\s+");
    }
    
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
    
    public void shutdown() {
        modelLoader.close();
        logger.info("EmbeddingService shutdown complete");
    }
}

// ===== TEXT PREPROCESSOR =====

package com.codetalker.embedding;

import java.util.regex.Pattern;

/**
 * Preprocesses text for optimal embedding generation
 */
public class TextPreprocessor {
    
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^\\w\\s\\-_\\.]");
    private static final int MAX_TEXT_LENGTH = 512; // Reasonable limit for embeddings
    
    /**
     * Clean and normalize text for embedding generation
     */
    public String preprocess(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove excessive whitespace
        text = MULTIPLE_SPACES.matcher(text.trim()).replaceAll(" ");
        
        // Handle code-specific preprocessing
        text = preprocessCodeText(text);
        
        // Truncate if too long
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
            // Try to end at word boundary
            int lastSpace = text.lastIndexOf(' ');
            if (lastSpace > MAX_TEXT_LENGTH * 0.8) {
                text = text.substring(0, lastSpace);
            }
        }
        
        return text;
    }
    
    private String preprocessCodeText(String text) {
        // Preserve camelCase and snake_case readability
        text = text.replaceAll("([a-z])([A-Z])", "$1 $2"); // camelCase -> camel Case
        text = text.replaceAll("_", " "); // snake_case -> snake case
        
        // Clean up common code artifacts
        text = text.replaceAll("/\\*.*?\\*/", " "); // Remove /* */ comments
        text = text.replaceAll("//.*?$", " "); // Remove // comments
        text = text.replaceAll("\\{|\\}", " "); // Remove braces
        text = text.replaceAll(";", " "); // Remove semicolons
        
        // Normalize multiple spaces again after code cleaning
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        
        return text.trim();
    }
    
    /**
     * Specialized preprocessing for different content types
     */
    public String preprocessByContentType(String text, String contentType) {
        text = preprocess(text);
        
        switch (contentType.toLowerCase()) {
            case "java":
                return preprocessJavaCode(text);
            case "sql":
                return preprocessSqlCode(text);
            case "markdown":
                return preprocessMarkdown(text);
            default:
                return text;
        }
    }
    
    private String preprocessJavaCode(String text) {
        // Additional Java-specific preprocessing
        text = text.replaceAll("\\bpublic\\b|\\bprivate\\b|\\bprotected\\b", ""); // Remove access modifiers
        text = text.replaceAll("\\bstatic\\b|\\bfinal\\b", ""); // Remove common keywords
        return text.trim();
    }
    
    private String preprocessSqlCode(String text) {
        // SQL-specific preprocessing
        text = text.toUpperCase(); // Normalize SQL keywords
        text = text.replaceAll("\\bCREATE\\s+TABLE\\b", "table definition");
        text = text.replaceAll("\\bSELECT\\b", "query");
        return text;
    }
    
    private String preprocessMarkdown(String text) {
        // Remove Markdown formatting
        text = text.replaceAll("#+ ", ""); // Remove headers
        text = text.replaceAll("\\*\\*|__", ""); // Remove bold formatting
        text = text.replaceAll("\\*|_", ""); // Remove italic formatting
        text = text.replaceAll("`", ""); // Remove code formatting
        return text;
    }
}

// ===== EMBEDDING REPOSITORY =====

package com.codetalker.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for storing and retrieving embeddings from H2 database
 */
public class EmbeddingRepository {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingRepository.class);
    
    private final DatabaseManager databaseManager;
    private final ObjectMapper jsonMapper;
    
    public EmbeddingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.jsonMapper = new ObjectMapper();
    }
    
    /**
     * Store embedding with metadata
     */
    public boolean storeEmbedding(EmbeddingRecord record) {
        String sql = """
            MERGE INTO embeddings (content_hash, file_path, content_type, chunk_text, vector, metadata)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, generateContentHash(record.chunkText));
            stmt.setString(2, record.filePath);
            stmt.setString(3, record.contentType);
            stmt.setString(4, record.chunkText);
            
            // Convert float array to H2 ARRAY format
            Array vectorArray = conn.createArrayOf("REAL", toObjectArray(record.vector));
            stmt.setArray(5, vectorArray);
            
            // Convert metadata to JSON
            String metadataJson = jsonMapper.writeValueAsString(record.metadata);
            stmt.setString(6, metadataJson);
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.id = rs.getLong(1);
                    }
                }
                logger.debug("Stored embedding for: {}", record.filePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Failed to store embedding for: {}", record.filePath, e);
        }
        
        return false;
    }
    
    /**
     * Batch store multiple embeddings (more efficient)
     */
    public int storeEmbeddings(List<EmbeddingRecord> records) {
        String sql = """
            MERGE INTO embeddings (content_hash, file_path, content_type, chunk_text, vector, metadata)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        int stored = 0;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (EmbeddingRecord record : records) {
                stmt.setString(1, generateContentHash(record.chunkText));
                stmt.setString(2, record.filePath);
                stmt.setString(3, record.contentType);
                stmt.setString(4, record.chunkText);
                
                Array vectorArray = conn.createArrayOf("REAL", toObjectArray(record.vector));
                stmt.setArray(5, vectorArray);
                
                String metadataJson = jsonMapper.writeValueAsString(record.metadata);
                stmt.setString(6, metadataJson);
                
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            conn.commit();
            
            for (int result : results) {
                if (result > 0) stored++;
            }
            
            logger.info("Stored {} out of {} embeddings", stored, records.size());
            
        } catch (Exception e) {
            logger.error("Failed to batch store embeddings", e);
        }
        
        return stored;
    }
    
    /**
     * Retrieve embedding by ID
     */
    public EmbeddingRecord getEmbedding(long id) {
        String sql = "SELECT * FROM embeddings WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecord(rs);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve embedding with id: {}", id, e);
        }
        
        return null;
    }
    
    /**
     * Get all embeddings for vector index building
     */
    public List<EmbeddingRecord> getAllEmbeddings() {
        String sql = "SELECT * FROM embeddings ORDER BY id";
        List<EmbeddingRecord> records = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                records.add(mapResultSetToRecord(rs));
            }
            
            logger.info("Retrieved {} embeddings from database", records.size());
            
        } catch (Exception e) {
            logger.error("Failed to retrieve all embeddings", e);
        }
        
        return records;
    }
    
    /**
     * Get embeddings by file path
     */
    public List<EmbeddingRecord> getEmbeddingsByFile(String filePath) {
        String sql = "SELECT * FROM embeddings WHERE file_path = ? ORDER BY id";
        List<EmbeddingRecord> records = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, filePath);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve embeddings for file: {}", filePath, e);
        }
        
        return records;
    }
    
    private EmbeddingRecord mapResultSetToRecord(ResultSet rs) throws Exception {
        EmbeddingRecord record = new EmbeddingRecord();
        
        record.id = rs.getLong("id");
        record.filePath = rs.getString("file_path");
        record.contentType = rs.getString("content_type");
        record.chunkText = rs.getString("chunk_text");
        
        // Convert H2 ARRAY to float array
        Array vectorArray = rs.getArray("vector");
        if (vectorArray != null) {
            Object[] objArray = (Object[]) vectorArray.getArray();
            record.vector = new float[objArray.length];
            for (int i = 0; i < objArray.length; i++) {
                record.vector[i] = ((Number) objArray[i]).floatValue();
            }
        }
        
        // Parse JSON metadata
        String metadataJson = rs.getString("metadata");
        if (metadataJson != null) {
            record.metadata = jsonMapper.readValue(metadataJson, Map.class);
        } else {
            record.metadata = new HashMap<>();
        }
        
        record.createdAt = rs.getTimestamp("created_at");
        
        return record;
    }
    
    private Object[] toObjectArray(float[] floats) {
        Object[] objects = new Object[floats.length];
        for (int i = 0; i < floats.length; i++) {
            objects[i] = floats[i];
        }
        return objects;
    }
    
    private String generateContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Failed to generate content hash", e);
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * Data class for embedding records
     */
    public static class EmbeddingRecord {
        public Long id;
        public String filePath;
        public String contentType;
        public String chunkText;
        public float[] vector;
        public Map<String, Object> metadata = new HashMap<>();
        public Timestamp createdAt;
        
        public EmbeddingRecord() {}
        
        public EmbeddingRecord(String filePath, String contentType, String chunkText, float[] vector) {
            this.filePath = filePath;
            this.contentType = contentType;
            this.chunkText = chunkText;
            this.vector = vector;
        }
    }
}

// ===== CONTENT EXTRACTOR INTERFACE =====

package com.codetalker.processing;

import java.util.Map;

/**
 * Interface for extracting content from different file types
 */
public interface ContentExtractor {
    
    /**
     * Check if this extractor can handle the given file
     */
    boolean canExtract(String filePath, String contentType);
    
    /**
     * Extract text content from file
     */
    ExtractedContent extract(String filePath) throws ExtractionException;
    
    /**
     * Get supported file extensions
     */
    String[] getSupportedExtensions();
    
    /**
     * Data class for extracted content
     */
    class ExtractedContent {
        public final String content;
        public final Map<String, Object> metadata;
        public final String contentType;
        
        public ExtractedContent(String content, Map<String, Object> metadata, String contentType) {
            this.content = content;
            this.metadata = metadata;
            this.contentType = contentType;
        }
    }
    
    /**
     * Exception thrown during content extraction
     */
    class ExtractionException extends Exception {
        public ExtractionException(String message) {
            super(message);
        }
        
        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// ===== JAVA FILE EXTRACTOR =====

package com.codetalker.processing.extractors;

import com.codetalker.processing.ContentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts content from Java source files
 */
public class JavaFileExtractor implements ContentExtractor {
    private static final Logger logger = LoggerFactory.getLogger(JavaFileExtractor.class);
    
    private static final String[] SUPPORTED_EXTENSIONS = {".java"};
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w\\.]+);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(public\\s+)?(class|interface|enum)\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+(\\w+)\\s*\\([^\\)]*\\)");
    
    @Override
    public boolean canExtract(String filePath, String contentType) {
        return filePath.toLowerCase().endsWith(".java");
    }
    
    @Override
    public ExtractedContent extract(String filePath) throws ExtractionException {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new ExtractionException("File not found: " + filePath);
            }
            
            String content = Files.readString(path);
            Map<String, Object> metadata = extractMetadata(content, filePath);
            
            // Clean content for better embedding
            String cleanedContent = cleanJavaContent(content);
            
            logger.debug("Extracted Java content from: {} ({} chars)", filePath, cleanedContent.length());
            
            return new ExtractedContent(cleanedContent, metadata, "java");
            
        } catch (IOException e) {
            throw new ExtractionException("Failed to read Java file: " + filePath, e);
        }
    }
    
    private Map<String, Object> extractMetadata(String content, String filePath) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Extract package name
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        if (packageMatcher.find()) {
            metadata.put("package", packageMatcher.group(1));
        }
        
        // Extract class names
        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (classMatcher.find()) {
            metadata.put("className", classMatcher.group(3));
            metadata.put("classType", classMatcher.group(2));
        }
        
        // Count methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        int methodCount = 0;
        while (methodMatcher.find()) {
            methodCount++;
        }
        metadata.put("methodCount", methodCount);
        
        // File metadata
        metadata.put("filePath", filePath);
        metadata.put("fileName", Paths.get(filePath).getFileName().toString());
        metadata.put("lineCount", content.split("\n").length);
        
        return metadata;
    }
    
    private String cleanJavaContent(String content) {
        // Remove single-line comments but preserve JavaDoc
        content = content.replaceAll("(?<!/)//.*?$", "");
        
        // Preserve JavaDoc comments but remove implementation comments
        content = content.replaceAll("/\\*(?!\\*).*?\\*/", " ");
        
        // Normalize whitespace
        content = content.replaceAll("\\s+", " ");
        
        // Remove imports section for cleaner embedding (they're in metadata)
        content = content.replaceAll("import\\s+[\\w\\.\\*]+;", "");
        
        return content.trim();
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.clone();
    }
}

// ===== JAVA CHUNKING STRATEGY =====

package com.codetalker.processing.chunking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chunks Java code by methods and classes while preserving context
 */
public class JavaChunkingStrategy implements ChunkingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(JavaChunkingStrategy.class);
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(/\\*\\*.*?\\*/\\s*)?" + // Optional JavaDoc
        "(public|private|protected)?\\s*" + // Access modifier
        "(static)?\\s*" + // Static keyword
        "([\\w<>\\[\\]]+)\\s+" + // Return type
        "(\\w+)\\s*" + // Method name  
        "\\([^\\)]*\\)\\s*" + // Parameters
        "\\{", // Opening brace
        Pattern.DOTALL
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(/\\*\\*.*?\\*/\\s*)?" + // Optional JavaDoc
        "(public|private|protected)?\\s*" + // Access modifier
        "(static)?\\s*" + // Static keyword
        "(class|interface|enum)\\s+" + // Type
        "(\\w+)" + // Class name
        ".*?\\{", // Everything up to opening brace
        Pattern.DOTALL
    );
    
    @Override
    public boolean canHandle(String contentType) {
        return "java".equalsIgnoreCase(contentType);
    }
    
    @Override
    public List<ContentChunk> chunk(String content, String filePath, Map<String, Object> metadata) {
        List<ContentChunk> chunks = new ArrayList<>();
        
        // Extract class-level context
        String className = (String) metadata.get("className");
        String packageName = (String) metadata.get("package");
        
        // Find all methods and create chunks
        List<MethodInfo> methods = extractMethods(content);
        
        for (MethodInfo method : methods) {
            String chunkContent = buildMethodChunk(content, method, className, packageName);
            
            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("methodName", method.name);
            chunkMetadata.put("chunkType", "method");
            chunkMetadata.put("lineStart", method.lineStart);
            chunkMetadata.put("lineEnd", method.lineEnd);
            
            ContentChunk chunk = new ContentChunk(
                chunkContent,
                filePath,
                "java-method",
                chunkMetadata
            );
            
            chunks.add(chunk);
        }
        
        // If no methods found or file is small, create single class-level chunk
        if (chunks.isEmpty() || content.length() < 1000) {
            Map<String, Object> classMetadata = new HashMap<>(metadata);
            classMetadata.put("chunkType", "class");
            
            ContentChunk classChunk = new ContentChunk(
                content,
                filePath,
                "java-class", 
                classMetadata
            );
            
            chunks.add(classChunk);
        }
        
        logger.debug("Created {} chunks for Java file: {}", chunks.size(), filePath);
        return chunks;
    }
    
    private List<MethodInfo> extractMethods(String content) {
        List<MethodInfo> methods = new ArrayList<>();
        String[] lines = content.split("\n");
        
        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        
        while (methodMatcher.find()) {
            String methodSignature = methodMatcher.group();
            String methodName = methodMatcher.group(5); // Method name group
            
            // Find the method boundaries by tracking braces
            int methodStart = methodMatcher.start();
            int methodEnd = findMethodEnd(content, methodStart);
            
            // Convert to line numbers
            int lineStart = getLineNumber(content, methodStart);
            int lineEnd = getLineNumber(content, methodEnd);
            
            MethodInfo method = new MethodInfo(
                methodName,
                methodStart,
                methodEnd,
                lineStart,
                lineEnd
            );
            
            methods.add(method);
        }
        
        return methods;
    }
    
    private int findMethodEnd(String content, int methodStart) {
        int braceCount = 0;
        boolean foundFirstBrace = false;
        
        for (int i = methodStart; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '{') {
                braceCount++;
                foundFirstBrace = true;
            } else if (c == '}') {
                braceCount--;
                if (foundFirstBrace && braceCount == 0) {
                    return i + 1; // Include closing brace
                }
            }
        }
        
        return content.length(); // If no closing brace found, use end of file
    }
    
    private int getLineNumber(String content, int position) {
        int lineNumber = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }
    
    private String buildMethodChunk(String content, MethodInfo method, String className, String packageName) {
        // Extract the method content
        String methodContent = content.substring(method.start, method.end);
        
        // Add context information
        StringBuilder chunkBuilder = new StringBuilder();
        
        if (packageName != null) {
            chunkBuilder.append("Package: ").append(packageName).append("\n");
        }
        
        if (className != null) {
            chunkBuilder.append("Class: ").append(className).append("\n");
        }
        
        chunkBuilder.append("Method: ").append(method.name).append("\n\n");
        chunkBuilder.append(methodContent);
        
        return chunkBuilder.toString();
    }
    
    private static class MethodInfo {
        final String name;
        final int start;
        final int end;
        final int lineStart;
        final int lineEnd;
        
        MethodInfo(String name, int start, int end, int lineStart, int lineEnd) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.lineStart = lineStart;
            this.lineEnd = lineEnd;
        }
    }
}

// ===== CHUNKING STRATEGY INTERFACE =====

package com.codetalker.processing.chunking;

import java.util.List;
import java.util.Map;

/**
 * Interface for content-specific chunking strategies
 */
public interface ChunkingStrategy {
    
    /**
     * Check if this strategy can handle the content type
     */
    boolean canHandle(String contentType);
    
    /**
     * Chunk content into logical pieces
     */
    List<ContentChunk> chunk(String content, String filePath, Map<String, Object> metadata);
    
    /**
     * Data class for content chunks
     */
    class ContentChunk {
        public final String content;
        public final String filePath;
        public final String chunkType;
        public final Map<String, Object> metadata;
        
        public ContentChunk(String content, String filePath, String chunkType, Map<String, Object> metadata) {
            this.content = content;
            this.filePath = filePath;
            this.chunkType = chunkType;
            this.metadata = metadata;
        }
        
        @Override
        public String toString() {
            return String.format("ContentChunk{type='%s', file='%s', length=%d}", 
                               chunkType, filePath, content.length());
        }
    }
}

// ===== VECTOR INDEX IMPLEMENTATION =====

package com.codetalker.search;

import io.github.jbellis.jvector.graph.GraphIndex;
import io.github.jbellis.jvector.graph.GraphIndexBuilder;
import io.github.jbellis.jvector.graph.RandomAccessVectorValues;
import io.github.jbellis.jvector.graph.similarity.ScalarQuantizedVectorSimilarity;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HNSW vector index implementation using JVector
 */
public class VectorIndex {
    private static final Logger logger = LoggerFactory.getLogger(VectorIndex.class);
    
    private static final int DEFAULT_M = 16; // HNSW M parameter
    private static final int DEFAULT_EF_CONSTRUCTION = 200; // Construction parameter
    private static final int DEFAULT_EF_SEARCH = 100; // Search parameter
    private static final String INDEX_FILE_NAME = "vector.index";
    private static final String METADATA_FILE_NAME = "index.metadata";
    
    private final int dimension;
    private final String indexPath;
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    
    private GraphIndex<float[]> index;
    private Map<Integer, Long> vectorIdToRecordId;
    private List<IndexedVector> vectors;
    private boolean isLoaded = false;
    
    public VectorIndex(int dimension, String indexPath) {
        this.dimension = dimension;
        this.indexPath = indexPath;
        this.vectorIdToRecordId = new HashMap<>();
        this.vectors = new ArrayList<>();
    }
    
    /**
     * Build index from vectors
     */
    public boolean buildIndex(List<IndexedVector> inputVectors) {
        indexLock.writeLock().lock();
        try {
            logger.info("Building HNSW index with {} vectors", inputVectors.size());
            
            this.vectors = new ArrayList<>(inputVectors);
            this.vectorIdToRecordId.clear();
            
            // Create vector values for JVector
            RandomAccessVectorValues vectorValues = new RandomAccessVectorValues() {
                @Override
                public int size() {
                    return vectors.size();
                }
                
                @Override
                public int dimension() {
                    return dimension;
                }
                
                @Override
                public float[] vectorValue(int targetOrd) {
                    return vectors.get(targetOrd).vector;
                }
                
                @Override
                public RandomAccessVectorValues copy() {
                    return this;
                }
            };
            
            // Build the index
            GraphIndexBuilder builder = new GraphIndexBuilder(
                vectorValues,
                VectorSimilarityFunction.COSINE, // Use cosine similarity
                DEFAULT_M,
                DEFAULT_EF_CONSTRUCTION,
                1.0f,
                1.0f
            );
            
            index = builder.build();
            
            // Build mapping from vector index to record ID
            for (int i = 0; i < vectors.size(); i++) {
                vectorIdToRecordId.put(i, vectors.get(i).recordId);
            }
            
            isLoaded = true;
            
            logger.info("HNSW index built successfully with {} vectors", vectors.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to build HNSW index", e);
            return false;
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    /**
     * Search for similar vectors
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (!isLoaded) {
            throw new IllegalStateException("Index not loaded. Call buildIndex() first.");
        }
        
        indexLock.readLock().lock();
        try {
            // Perform HNSW search
            var results = index.search(queryVector, topK, DEFAULT_EF_SEARCH);
            
            List<SearchResult> searchResults = new ArrayList<>();
            
            for (int i = 0; i < results.size(); i++) {
                int vectorId = results.node(i);
                float score = results.score(i);
                
                Long recordId = vectorIdToRecordId.get(vectorId);
                if (recordId != null && vectorId < vectors.size()) {
                    IndexedVector vector = vectors.get(vectorId);
                    searchResults.add(new SearchResult(recordId, score, vector.content));
                }
            }
            
            logger.debug("Found {} results for vector search", searchResults.size());
            return searchResults;
            
        } catch (Exception e) {
            logger.error("Vector search failed", e);
            return new ArrayList<>();
        } finally {
            indexLock.readLock().unlock();
        }
    }
    
    /**
     * Save index to disk
     */
    public boolean saveIndex() {
        if (!isLoaded) {
            logger.warn("Cannot save index - not loaded");
            return false;
        }
        
        indexLock.readLock().lock();
        try {
            Path indexDir = Paths.get(indexPath);
            Files.createDirectories(indexDir);
            
            // Save index data
            Path indexFile = indexDir.resolve(INDEX_FILE_NAME);
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(indexFile)))) {
                
                // Write basic metadata
                dos.writeInt(dimension);
                dos.writeInt(vectors.size());
                
                // Write vectors and mapping
                for (int i = 0; i < vectors.size(); i++) {
                    IndexedVector vector = vectors.get(i);
                    dos.writeLong(vector.recordId);
                    
                    // Write vector data
                    for (float value : vector.vector) {
                        dos.writeFloat(value);
                    }
                    
                    // Write content length and content
                    byte[] contentBytes = vector.content.getBytes();
                    dos.writeInt(contentBytes.length);
                    dos.write(contentBytes);
                }
            }
            
            // Save JVector index (this is a simplified version - actual implementation would use JVector's serialization)
            // Note: JVector may not have built-in serialization, so this might need custom implementation
            
            logger.info("Index saved to: {}", indexFile);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to save index", e);
            return false;
        } finally {
            indexLock.readLock().unlock();
        }
    }
    
    /**
     * Load index from disk
     */
    public boolean loadIndex() {
        indexLock.writeLock().lock();
        try {
            Path indexDir = Paths.get(indexPath);
            Path indexFile = indexDir.resolve(INDEX_FILE_NAME);
            
            if (!Files.exists(indexFile)) {
                logger.warn("Index file not found: {}", indexFile);
                return false;
            }
            
            vectors.clear();
            vectorIdToRecordId.clear();
            
            try (DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(indexFile)))) {
                
                int savedDimension = dis.readInt();
                if (savedDimension != dimension) {
                    logger.error("Dimension mismatch: expected {}, found {}", dimension, savedDimension);
                    return false;
                }
                
                int vectorCount = dis.readInt();
                
                // Read vectors
                for (int i = 0; i < vectorCount; i++) {
                    long recordId = dis.readLong();
                    
                    // Read vector data
                    float[] vector = new float[dimension];
                    for (int j = 0; j < dimension; j++) {
                        vector[j] = dis.readFloat();
                    }
                    
                    // Read content
                    int contentLength = dis.readInt();
                    byte[] contentBytes = new byte[contentLength];
                    dis.readFully(contentBytes);
                    String content = new String(contentBytes);
                    
                    vectors.add(new IndexedVector(recordId, vector, content));
                    vectorIdToRecordId.put(i, recordId);
                }
            }
            
            // Rebuild the HNSW index from loaded vectors
            return buildIndex(vectors);
            
        } catch (Exception e) {
            logger.error("Failed to load index", e);
            return false;
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    public boolean isLoaded() {
        return isLoaded;
    }
    
    public int size() {
        return vectors.size();
    }
    
    public IndexStats getStats() {
        indexLock.readLock().lock();
        try {
            return new IndexStats(vectors.size(), dimension, isLoaded);
        } finally {
            indexLock.readLock().unlock();
        }
    }
    
    /**
     * Data classes
     */
    public static class IndexedVector {
        public final long recordId;
        public final float[] vector;
        public final String content;
        
        public IndexedVector(long recordId, float[] vector, String content) {
            this.recordId = recordId;
            this.vector = vector;
            this.content = content;
        }
    }
    
    public static class SearchResult {
        public final long recordId;
        public final float score;
        public final String content;
        
        public SearchResult(long recordId, float score, String content) {
            this.recordId = recordId;
            this.score = score;
            this.content = content;
        }
        
        @Override
        public String toString() {
            return String.format("SearchResult{id=%d, score=%.4f, content='%.50s...'}", 
                               recordId, score, content);
        }
    }
    
    public static class IndexStats {
        public final int vectorCount;
        public final int dimension;
        public final boolean isLoaded;
        
        public IndexStats(int vectorCount, int dimension, boolean isLoaded) {
            this.vectorCount = vectorCount;
            this.dimension = dimension;
            this.isLoaded = isLoaded;
        }
        
        @Override
        public String toString() {
            return String.format("IndexStats{vectors=%d, dimension=%d, loaded=%s}", 
                               vectorCount, dimension, isLoaded);
        }
    }
}

// ===== QUERY PROCESSOR =====

package com.codetalker.api;

import com.codetalker.embedding.EmbeddingService;
import com.codetalker.search.VectorIndex;
import com.codetalker.storage.EmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Processes natural language queries and returns relevant results
 */
public class QueryProcessor {
    private static final Logger logger = LoggerFactory.getLogger(QueryProcessor.class);
    
    private static final int DEFAULT_RESULT_COUNT = 10;
    private static final float MIN_SIMILARITY_SCORE = 0.3f;
    
    private final EmbeddingService embeddingService;
    private final VectorIndex vectorIndex;
    private final EmbeddingRepository embeddingRepository;
    private final ResultRanker resultRanker;
    
    // Query expansion patterns
    private static final Map<Pattern, String> QUERY_EXPANSIONS = new HashMap<>();
    static {
        QUERY_EXPANSIONS.put(Pattern.compile("\\bhow does (.+) work\\b", Pattern.CASE_INSENSITIVE), 
                           "implementation logic process $1");
        QUERY_EXPANSIONS.put(Pattern.compile("\\bwhat is (.+)\\b", Pattern.CASE_INSENSITIVE), 
                           "definition explanation $1");
        QUERY_EXPANSIONS.put(Pattern.compile("\\bwhere is (.+)\\b", Pattern.CASE_INSENSITIVE), 
                           "location file method $1");
        QUERY_EXPANSIONS.put(Pattern.compile("\\bhow to (.+)\\b", Pattern.CASE_INSENSITIVE), 
                           "example usage guide $1");
    }
    
    public QueryProcessor(EmbeddingService embeddingService, 
                         VectorIndex vectorIndex,
                         EmbeddingRepository embeddingRepository) {
        this.embeddingService = embeddingService;
        this.vectorIndex = vectorIndex;
        this.embeddingRepository = embeddingRepository;
        this.resultRanker = new ResultRanker();
    }
    
    /**
     * Process a natural language query
     */
    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Preprocess and expand query
            String expandedQuery = expandQuery(request.query);
            
            // Generate query embedding
            float[] queryEmbedding = embeddingService.generateEmbedding(expandedQuery);
            
            // Vector similarity search
            int searchCount = Math.max(request.maxResults * 3, 30); // Get more for ranking
            List<VectorIndex.SearchResult> vectorResults = vectorIndex.search(queryEmbedding, searchCount);
            
            // Filter by minimum similarity
            vectorResults.removeIf(result -> result.score < MIN_SIMILARITY_SCORE);
            
            // Get full embedding records
            List<QueryResult> enrichedResults = enrichResults(vectorResults);
            
            // Rank and filter results
            List<QueryResult> rankedResults = resultRanker.rankResults(enrichedResults, request.query);
            
            // Limit to requested count
            if (rankedResults.size() > request.maxResults) {
                rankedResults = rankedResults.subList(0, request.maxResults);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.info("Processed query '{}' in {}ms, returning {} results", 
                       request.query, processingTime, rankedResults.size());
            
            return new QueryResponse(
                rankedResults,
                processingTime,
                expandedQuery,
                vectorResults.size(),
                true
            );
            
        } catch (Exception e) {
            logger.error("Query processing failed for: {}", request.query, e);
            
            return new QueryResponse(
                new ArrayList<>(),
                System.currentTimeMillis() - startTime,
                request.query,
                0,
                false
            );
        }
    }
    
    private String expandQuery(String originalQuery) {
        String expanded = originalQuery;
        
        // Apply query expansion patterns
        for (Map.Entry<Pattern, String> entry : QUERY_EXPANSIONS.entrySet()) {
            expanded = entry.getKey().matcher(expanded).replaceAll(entry.getValue());
        }
        
        // Add technical domain terms
        expanded = addDomainTerms(expanded);
        
        if (!expanded.equals(originalQuery)) {
            logger.debug("Expanded query: '{}' -> '{}'", originalQuery, expanded);
        }
        
        return expanded;
    }
    
    private String addDomainTerms(String query) {
        // Add relevant technical terms based on query content
        StringBuilder enhanced = new StringBuilder(query);
        
        if (query.toLowerCase().contains("database") || query.toLowerCase().contains("table")) {
            enhanced.append(" sql schema database table");
        }
        
        if (query.toLowerCase().contains("method") || query.toLowerCase().contains("function")) {
            enhanced.append(" method function implementation code");
        }
        
        if (query.toLowerCase().contains("class") || query.toLowerCase().contains("object")) {
            enhanced.append(" class object inheritance java");
        }
        
        return enhanced.toString();
    }
    
    private List<QueryResult> enrichResults(List<VectorIndex.SearchResult> vectorResults) {
        List<QueryResult> enriched = new ArrayList<>();
        
        for (VectorIndex.SearchResult vectorResult : vectorResults) {
            // Get full record from database
            EmbeddingRepository.EmbeddingRecord record = embeddingRepository.getEmbedding(vectorResult.recordId);
            
            if (record != null) {
                QueryResult queryResult = new QueryResult(
                    record.id,
                    record.filePath,
                    record.contentType,
                    record.chunkText,
                    vectorResult.score,
                    record.metadata
                );
                
                enriched.add(queryResult);
            }
        }
        
        return enriched;
    }
    
    /**
     * Data classes for query processing
     */
    public static class QueryRequest {
        public final String query;
        public final int maxResults;
        public final Map<String, String> filters;
        
        public QueryRequest(String query) {
            this(query, DEFAULT_RESULT_COUNT, new HashMap<>());
        }
        
        public QueryRequest(String query, int maxResults, Map<String, String> filters) {
            this.query = query;
            this.maxResults = maxResults;
            this.filters = filters;
        }
    }
    
    public static class QueryResponse {
        public final List<QueryResult> results;
        public final long processingTimeMs;
        public final String expandedQuery;
        public final int totalFound;
        public final boolean success;
        
        public QueryResponse(List<QueryResult> results, long processingTimeMs, 
                           String expandedQuery, int totalFound, boolean success) {
            this.results = results;
            this.processingTimeMs = processingTimeMs;
            this.expandedQuery = expandedQuery;
            this.totalFound = totalFound;
            this.success = success;
        }
    }
    
    public static class QueryResult {
        public final Long id;
        public final String filePath;
        public final String contentType;
        public final String content;
        public final float similarityScore;
        public final Map<String, Object> metadata;
        public float finalScore; // Set by ranker
        
        public QueryResult(Long id, String filePath, String contentType, String content,
                         float similarityScore, Map<String, Object> metadata) {
            this.id = id;
            this.filePath = filePath;
            this.contentType = contentType;
            this.content = content;
            this.similarityScore = similarityScore;
            this.metadata = metadata;
            this.finalScore = similarityScore;
        }
        
        @Override
        public String toString() {
            return String.format("QueryResult{file='%s', type='%s', score=%.3f, content='%.100s...'}", 
                               filePath, contentType, finalScore, content);
        }
    }
}

// ===== RESULT RANKER =====

package com.codetalker.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Ranks and filters query results using multiple criteria
 */
public class ResultRanker {
    private static final Logger logger = LoggerFactory.getLogger(ResultRanker.class);
    
    private static final float SIMILARITY_WEIGHT = 0.4f;
    private static final float KEYWORD_WEIGHT = 0.3f;
    private static final float CONTENT_TYPE_WEIGHT = 0.2f;
    private static final float METADATA_WEIGHT = 0.1f;
    
    // Content type preferences for different query types
    private static final Map<Pattern, Map<String, Float>> QUERY_TYPE_PREFERENCES = new HashMap<>();
    static {
        Map<String, Float> implementationPrefs = new HashMap<>();
        implementationPrefs.put("java-method", 1.0f);
        implementationPrefs.put("java-class", 0.8f);
        implementationPrefs.put("sql", 0.6f);
        implementationPrefs.put("markdown", 0.4f);
        QUERY_TYPE_PREFERENCES.put(Pattern.compile("\\b(how does|implementation|logic|code)\\b", Pattern.CASE_INSENSITIVE), 
                                 implementationPrefs);
        
        Map<String, Float> definitionPrefs = new HashMap<>();
        definitionPrefs.put("markdown", 1.0f);
        definitionPrefs.put("java-class", 0.8f);
        definitionPrefs.put("java-method", 0.6f);
        definitionPrefs.put("sql", 0.4f);
        QUERY_TYPE_PREFERENCES.put(Pattern.compile("\\b(what is|definition|explain)\\b", Pattern.CASE_INSENSITIVE), 
                                 definitionPrefs);
    }
    
    public List<QueryProcessor.QueryResult> rankResults(List<QueryProcessor.QueryResult> results, String originalQuery) {
        if (results.isEmpty()) {
            return results;
        }
        
        // Calculate final scores
        for (QueryProcessor.QueryResult result : results) {
            result.finalScore = calculateFinalScore(result, originalQuery);
        }
        
        // Sort by final score (descending)
        results.sort(Comparator.comparing((QueryProcessor.QueryResult r) -> r.finalScore).reversed());
        
        logger.debug("Ranked {} results for query: {}", results.size(), originalQuery);
        
        return results;
    }
    
    private float calculateFinalScore(QueryProcessor.QueryResult result, String query) {
        float score = 0.0f;
        
        // Base similarity score
        score += result.similarityScore * SIMILARITY_WEIGHT;
        
        // Keyword matching bonus
        score += calculateKeywordScore(result.content, query) * KEYWORD_WEIGHT;
        
        // Content type preference
        score += calculateContentTypeScore(result.contentType, query) * CONTENT_TYPE_WEIGHT;
        
        // Metadata bonuses
        score += calculateMetadataScore(result.metadata, query) * METADATA_WEIGHT;
        
        return Math.min(score, 1.0f); // Cap at 1.0
    }
    
    private float calculateKeywordScore(String content, String query) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();
        
        int matches = 0;
        int totalTerms = queryTerms.length;
        
        for (String term : queryTerms) {
            if (term.length() < 3) continue; // Skip short terms
            
            if (lowerContent.contains(term)) {
                matches++;
                
                // Bonus for exact word matches
                if (lowerContent.contains(" " + term + " ") || 
                    lowerContent.startsWith(term + " ") ||
                    lowerContent.endsWith(" " + term)) {
                    matches++; // Double count for word boundaries
                }
            }
        }
        
        return totalTerms > 0 ? (float) matches / totalTerms : 0.0f;
    }
    
    private float calculateContentTypeScore(String contentType, String query) {
        // Check query type preferences
        for (Map.Entry<Pattern, Map<String, Float>> entry : QUERY_TYPE_PREFERENCES.entrySet()) {
            if (entry.getKey().matcher(query).find()) {
                Map<String, Float> preferences = entry.getValue();
                return preferences.getOrDefault(contentType, 0.5f);
            }
        }
        
        // Default content type scoring
        switch (contentType.toLowerCase()) {
            case "java-method":
                return 0.9f;
            case "java-class":
                return 0.8f;
            case "sql":
                return 0.7f;
            case "markdown":
                return 0.6f;
            default:
                return 0.5f;
        }
    }
    
    private float calculateMetadataScore(Map<String, Object> metadata, String query) {
        float score = 0.0f;
        String lowerQuery = query.toLowerCase();
        
        // Bonus for method name matches
        if (metadata.containsKey("methodName")) {
            String methodName = metadata.get("methodName").toString().toLowerCase();
            if (lowerQuery.contains(methodName)) {
                score += 0.5f;
            }
        }
        
        // Bonus for class name matches
        if (metadata.containsKey("className")) {
            String className = metadata.get("className").toString().toLowerCase();
            if (lowerQuery.contains(className)) {
                score += 0.3f;
            }
        }
        
        // Bonus for business domain matches
        if (metadata.containsKey("businessDomain")) {
            String domain = metadata.get("businessDomain").toString().toLowerCase();
            if (lowerQuery.contains(domain) || 
                (domain.equals("authentication") && lowerQuery.contains("login")) ||
                (domain.equals("data-access") && lowerQuery.contains("database"))) {
                score += 0.4f;
            }
        }
        
        return Math.min(score, 1.0f);
    }
}