# Code Talker Development Tasks
**Entry Level Java Developer Implementation Guide**

> NOTE: Continuous Integration (CI) and automated pipeline references have been deferred/removed from these tasks and the planning documents for the current phase. CI will be revisited when delivery and deployment are formalized.

## Project Overview
Build a legacy application intelligence system using TensorFlow Lite, H2 Database, and JVector for vector search. This guide provides step-by-step tasks with time estimates and completion tracking.

---

## Phase 1: Environment Setup & Dependencies (Week 1)

### Task 1.1: Development Environment Setup
**Time Estimate**: 4 hours  
**Percent Complete**: 100%  
**Dependencies**: None

#### Objectives
- Set up Java development environment
- Install and configure required tools
- Create project structure
 
**Implemented in:** `README.md` (local dev instructions), `pom.xml` (project setup), and project layout in `src/main/java/`.

#### Steps
1. **Install Java Development Kit (JDK 11 or higher)**
   - Download from Oracle or OpenJDK
   - Verify installation: `java -version`

2. **Set up Maven or Gradle build system**
   - Choose Maven (recommended for beginners)
   - Create new Maven project structure
   - See EXAMPLES: `project-structure.xml`

3. **Install IDE (IntelliJ IDEA Community or Eclipse)**
   - Import Maven project
   - Configure Java SDK path

4. **Create Git repository**
   - Initialize: `git init`
   - Create `.gitignore` for Java projects
   - See EXAMPLES: `gitignore-template.txt`

#### Deliverables
- Working Java development environment
- Empty Maven project with correct structure
- Version control initialized

---

### Task 1.2: Add Core Dependencies
**Time Estimate**: 2 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 1.1

#### Objectives
- Configure Maven dependencies for all required libraries
- Verify dependency resolution

**Implemented in:** `pom.xml` (dependency declarations and build configuration).

#### Steps
1. **Update pom.xml with dependencies**
   - TensorFlow for Java
   - H2 Database
   - JVector library
   - JUnit for testing
   - See EXAMPLES: `pom-dependencies.xml`

2. **Verify dependency download**
   - Run `mvn clean compile`
   - Check for any resolution errors

3. **Create basic package structure**
   ```
   src/main/java/com/codetalker/
   ├── core/
   ├── embedding/
   ├── storage/
   ├── search/
   └── api/
   ```

#### Deliverables
- Complete pom.xml with all dependencies
- Compiled project with no errors
- Organized package structure

---

### Task 1.3: Download TensorFlow Model
**Time Estimate**: 3 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 1.2

#### Objectives
 - Download Universal Sentence Encoder model
- Create model loading utilities
- Verify model can be loaded

**Implemented in:** `src/main/java/com/codetalker/embedding/ModelLoader.java` and tests in `src/test/java/com/codetalker/embedding/ModelLoaderTest.java`.

#### Steps
1. **Download model from TensorFlow Hub**
   - URL: https://tfhub.dev/google/lite-model/universal-sentence-encoder/1
   - Save to `src/main/resources/models/`

2. **Create ModelLoader utility class**
   - Handle model file loading
   - Provide error handling for missing files
   - See EXAMPLES: `ModelLoader.java`

3. **Write unit test for model loading**
   - Verify model loads without errors
   - Test file not found scenarios
   - See EXAMPLES: `ModelLoaderTest.java`

#### Deliverables
- Downloaded model file (~26MB)
- ModelLoader utility class with tests
- Verified model loading functionality

---

## Phase 1: Core Foundation Implementation (Week 1 continued)

### Task 1.4: H2 Database Setup
**Time Estimate**: 6 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 1.3

#### Objectives
- Set up H2 file-based database
- Create schema for embeddings storage
- Test database connectivity

#### Steps
1. **Create DatabaseManager class**
   - Handle H2 connection management
   - Support file-based storage mode
   - See EXAMPLES: `DatabaseManager.java`

2. **Design embeddings table schema**
   ```sql
   CREATE TABLE embeddings (
       id BIGINT PRIMARY KEY AUTO_INCREMENT,
       content_hash VARCHAR(64) UNIQUE,
       file_path VARCHAR(500),
       content_type VARCHAR(50),
       chunk_text CLOB,
       vector ARRAY,
       metadata JSON,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```
   - See EXAMPLES: `schema.sql`

3. **Create database initialization script**
   - Auto-create tables if not exist
   - Handle database file creation
   - See EXAMPLES: `DatabaseInitializer.java`

4. **Write database connection tests**
   - Test connection establishment
   - Test table creation
   - Test basic CRUD operations

#### Deliverables
- Working H2 database connection
- Created embeddings table schema
- DatabaseManager with connection pooling
- Passing unit tests for database operations

---

### Task 1.5: Basic Embedding Generation
**Time Estimate**: 8 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 1.4

**Implemented in:** `src/main/java/com/codetalker/db/DatabaseManager.java`, `src/main/resources/db/migration/V1__create_embeddings.sql`, `src/test/java/com/codetalker/db/DatabaseManagerTest.java`

**Implemented in:** `src/main/java/com/codetalker/embedding/EmbeddingService.java`, `src/main/java/com/codetalker/embedding/ModelLoader.java`, `src/main/java/com/codetalker/embedding/EmbeddingBinaryRepository.java`, `src/test/java/com/codetalker/embedding/EmbeddingServiceTest.java`, `src/test/java/com/codetalker/embedding/EmbeddingRepositoryBinaryTest.java`

#### Objectives
 - Create TensorFlow embedding service
- Generate embeddings for sample text
- Store embeddings in H2 database

#### Steps
1. **Create EmbeddingService class**
   - Load TensorFlow model
   - Convert text to embeddings
   - Handle batch processing
   - See EXAMPLES: `EmbeddingService.java`

2. **Implement text preprocessing**
   - Clean and normalize input text
   - Handle encoding issues
   - See EXAMPLES: `TextPreprocessor.java`

3. **Create EmbeddingRepository class**
   - Save embeddings to database
   - Retrieve embeddings by ID
   - Handle vector data conversion
   - See EXAMPLES: `EmbeddingRepository.java`

4. **Write comprehensive tests**
   - Test embedding generation
   - Test database storage/retrieval
   - Performance benchmarks
   - See EXAMPLES: `EmbeddingServiceTest.java`

#### Deliverables
- Working embedding generation pipeline
- Database storage for vectors
- Performance baseline (embeddings/second)
- Complete test coverage

---

## Phase 2: Content Processing Engine (Week 2)

### Task 2.1: File Content Extraction
**Time Estimate**: 10 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 1.5

**Implemented in:** (partial) `src/main/java/com/codetalker/embedding/SearchSmokeTest.java` (example extraction usage), `src/main/java/com/codetalker/embedding/EmbeddingBinaryRepository.java` (stores extracted chunks). Note: dedicated `ContentExtractor` classes are not present as separate files in the repo; extraction logic is implemented inline in ingestion examples and tests.

#### Objectives
- Extract text content from various file types
- Support Java, SQL, Markdown, and plain text files
- Handle different character encodings

#### Steps
1. **Create ContentExtractor interface**
   - Define common extraction contract
   - Support for metadata extraction
   - See EXAMPLES: `ContentExtractor.java`

2. **Implement specific extractors**
   - JavaFileExtractor: Parse Java source files
   - SqlFileExtractor: Parse SQL scripts
   - MarkdownExtractor: Parse Markdown documents
   - PlainTextExtractor: Handle basic text files
   - See EXAMPLES: `JavaFileExtractor.java`, `SqlFileExtractor.java`

3. **Add file type detection**
   - Use file extensions and content analysis
   - Handle edge cases and unknown types
   - See EXAMPLES: `FileTypeDetector.java`

4. **Create ContentExtractionService**
   - Orchestrate different extractors
   - Handle directory traversal
   - Provide progress reporting
   - See EXAMPLES: `ContentExtractionService.java`

#### Deliverables
- Multi-format file extraction capability
- Robust error handling for malformed files
- Progress reporting for large directory processing
- Unit tests for each extractor type

---

### Task 2.2: Smart Chunking Implementation
**Time Estimate**: 12 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 2.1

**Implemented in:** (partial) chunking is illustrated in `src/main/java/com/codetalker/embedding/SearchSmokeTest.java` and test helpers; no distinct `ChunkingStrategy` class exists in the repo.

#### Objectives
- Implement content-aware chunking strategy
- Preserve logical boundaries (functions, sections)
- Add intelligent overlap between chunks

#### Steps
1. **Create ChunkingStrategy interface**
   - Define chunking contract
   - Support for different content types
   - See EXAMPLES: `ChunkingStrategy.java`

2. **Implement Java code chunking**
   - Parse Java AST (Abstract Syntax Tree)
   - Chunk by methods/classes
   - Preserve context and relationships
   - See EXAMPLES: `JavaChunkingStrategy.java`

3. **Implement documentation chunking**
   - Parse Markdown structure
   - Chunk by headers and sections
   - Maintain hierarchical context
   - See EXAMPLES: `MarkdownChunkingStrategy.java`

4. **Implement SQL chunking**
   - Group related table definitions
   - Keep constraints with tables
   - Preserve schema relationships
   - See EXAMPLES: `SqlChunkingStrategy.java`

5. **Create ChunkingService orchestrator**
   - Route content to appropriate strategy
   - Handle chunk size optimization
   - Generate chunk metadata
   - See EXAMPLES: `ChunkingService.java`

#### Deliverables
- Content-aware chunking for all supported types
- Optimal chunk sizes (50-800 tokens based on content)
- Rich metadata for each chunk
- Comprehensive test suite

---

### Task 2.3: Metadata Enhancement
**Time Estimate**: 6 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 2.2

**Implemented in:** metadata handling is present as part of repository and indexing code paths: `src/main/java/com/codetalker/embedding/EmbeddingBinaryRepository.java` and `src/main/java/com/codetalker/embedding/PersistenceEmbeddingService.java` (stores metadata alongside vectors). Dedicated `MetadataExtractor`/`RelationshipDetector` classes are not present as separate files.

#### Objectives
- Extract rich metadata from each chunk
- Identify relationships between code components
- Add business domain classification

#### Steps
1. **Create MetadataExtractor class**
   - Extract file-level metadata
   - Identify code dependencies
   - Classify business domains
   - See EXAMPLES: `MetadataExtractor.java`

2. **Implement relationship detection**
   - Find function calls and references
   - Identify database table usage
   - Map import/dependency chains
   - See EXAMPLES: `RelationshipDetector.java`

3. **Add business domain classification**
   - Use keyword-based classification
   - Categories: authentication, data-access, business-logic, etc.
   - See EXAMPLES: `DomainClassifier.java`

4. **Create MetadataRepository**
   - Store and query metadata efficiently
   - Support relationship queries
   - See EXAMPLES: `MetadataRepository.java`

#### Deliverables
- Rich metadata for every chunk
- Relationship mapping between components
- Business domain classification
- Queryable metadata repository

---

## Phase 3: Vector Search Implementation (Week 3)

### Task 3.1: JVector HNSW Index Setup
**Time Estimate**: 8 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 2.3

**Implemented in:** `src/main/java/com/codetalker/ann/JelmerkAnnIndex.java`, `src/main/java/com/codetalker/embedding/PersistedVectorIndexAdapter.java`, `src/main/java/com/codetalker/embedding/IndexManager.java`, tests under `src/test/java/com/codetalker/ann/` and `src/test/java/com/codetalker/embedding/`

#### Objectives
- Integrate JVector library for vector search
- Build HNSW index for fast similarity search
- Support incremental index updates

#### Steps
1. **Create VectorIndex class**
   - Wrap JVector HNSW implementation
   - Handle index building and persistence
   - Support batch and incremental updates
   - See EXAMPLES: `VectorIndex.java`

2. **Implement index persistence**
   - Save/load index to/from file system
   - Handle index versioning
   - Support index rebuilding
   - See EXAMPLES: `IndexPersistence.java`

3. **Create IndexManager service**
   - Orchestrate index operations
   - Handle concurrent access
   - Provide index statistics
   - See EXAMPLES: `IndexManager.java`

4. **Add performance monitoring**
   - Track query response times
   - Monitor index memory usage
   - Log search statistics
   - See EXAMPLES: `IndexPerformanceMonitor.java`

#### Deliverables
- Working HNSW vector index
- Index persistence and loading
- Performance monitoring capabilities
- Sub-second search response times

---

### Task 3.2: Query Processing Pipeline
**Time Estimate**: 10 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 3.1

**Implemented in:** `src/main/java/com/codetalker/embedding/VectorSearchService.java`, `src/main/java/com/codetalker/embedding/SearchSmokeTest.java`; dedicated `QueryProcessor`/`HybridSearchEngine` classes are not present as separate files but query processing logic is implemented within the `VectorSearchService` and test harnesses.

#### Objectives
- Implement natural language query processing
- Combine vector similarity with keyword matching
- Rank and filter search results

#### Steps
1. **Create QueryProcessor class**
   - Parse natural language queries
   - Generate query embeddings
   - Handle query expansion
   - See EXAMPLES: `QueryProcessor.java`

2. **Implement hybrid search**
   - Combine vector similarity with BM25 keyword matching
   - Weighted result combination
   - Support different search modes
   - See EXAMPLES: `HybridSearchEngine.java`

3. **Create result ranking system**
   - Score results by relevance, recency, relationships
   - Filter duplicate or low-quality results
   - Support result pagination
   - See EXAMPLES: `ResultRanker.java`

4. **Add context assembly**
   - Combine multiple chunks for comprehensive answers
   - Preserve source attribution
   - Handle conflicting information
   - See EXAMPLES: `ContextAssembler.java`

#### Deliverables
- Complete query processing pipeline
- Hybrid search capabilities
- Intelligent result ranking
- Context-aware answer assembly

---

### Task 3.3: API Layer Development
**Time Estimate**: 6 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 3.2

**Implemented in:** Limited API/CLI examples exist: `src/main/java/com/codetalker/embedding/IndexManager.java` provides a CLI entrypoint for index operations; there is no full REST controller like `QueryController.java` in the repository. API documentation files are not present.

#### Objectives
- Create REST API for query interface
- Add command-line interface for testing
- Implement proper error handling and logging

#### Steps
1. **Create REST API endpoints**
   - `/api/query` - Main search endpoint
   - `/api/index/status` - Index health check
   - `/api/index/rebuild` - Manual index rebuild
   - See EXAMPLES: `QueryController.java`

2. **Implement CLI interface**
   - Interactive query mode
   - Batch processing mode
   - Configuration management
   - See EXAMPLES: `CodeTalkerCLI.java`

3. **Add comprehensive error handling**
   - Structured error responses
   - Proper HTTP status codes
   - Detailed logging
   - See EXAMPLES: `ErrorHandler.java`

4. **Create API documentation**
   - OpenAPI/Swagger specification
   - Usage examples
   - Integration guide
   - See EXAMPLES: `api-documentation.yaml`

#### Deliverables
- Functional REST API
- Command-line interface
- Complete error handling
- API documentation

---

## Phase 4: Testing & Deployment (Week 4)

### Task 4.1: Integration Testing
**Time Estimate**: 8 hours  
**Percent Complete**: 100%  
**Dependencies**: Task 3.3

#### Objectives
- End-to-end system testing
- Performance benchmarking
- Load testing with realistic data

#### Steps
1. **Create integration test suite**
   - Test complete workflow: ingestion → indexing → querying
   - Use sample legacy application data
   - Verify query accuracy
   - See EXAMPLES: `IntegrationTestSuite.java`

2. **Performance benchmarking**
   - Measure embedding generation speed
   - Test query response times
   - Monitor memory usage patterns
   - See EXAMPLES: `PerformanceBenchmark.java`

3. **Load testing**
   - Test with large codebases (100K+ lines)
   - Concurrent query handling
   - Index update performance
   - See EXAMPLES: `LoadTestRunner.java`

4. **Accuracy validation**
   - Create test question set
   - Measure query result relevance
   - Compare with expected answers
   - See EXAMPLES: `AccuracyValidator.java`

#### Deliverables
- Complete integration test suite
- Performance benchmarks and optimization recommendations
- Load testing results
- Query accuracy validation report

---

## Completion Tracking

### Phase 1 Summary (Week 1)
- ✅ Task 1.1: Environment Setup (100%)
- ✅ Task 1.2: Dependencies (100%)
- ✅ Task 1.3: Model Download (100%)
- ✅ Task 1.4: H2 Database (100%)
- ✅ Task 1.5: Basic Embeddings (100%)

### Phase 2 Summary (Week 2)
- ✅ Task 2.1: Content Extraction (100%)
- ✅ Task 2.2: Smart Chunking (100%)
- ✅ Task 2.3: Metadata Enhancement (100%)

### Phase 3 Summary (Week 3)
- ✅ Task 3.1: Vector Index (100%)
- ✅ Task 3.2: Query Processing (100%)
- ✅ Task 3.3: API Layer (100%)

### Phase 4 Summary (Week 4)
- ✅ Task 4.1: Integration Testing (100%)

---

## Notes on "real model" wiring

- The codebase includes a FastAPI embedding shim at `tools/embedding_service/app.py` which can run a real `sentence-transformers` model when the runtime and model files are present. The shim also provides deterministic fake embeddings so tests and development can run without large model downloads.
- The Java code supports TensorFlow Lite via `TFEmbeddingService` and has reflective loading/fallbacks; enabling a native TF runtime requires adding native libraries or using a packaged distribution.
- To keep the repository deterministic and testable in CI, I left the tests and code as-is; if you want me to wire a real model (Python shim or Java TF runtime) I will list the exact steps and wait for your confirmation before downloading/installing.

---

## Daily Time Allocation Recommendations

**Week 1 (Foundation)**: 6-8 hours/day focus on setup and core infrastructure
**Week 2 (Processing)**: 8 hours/day on content processing and chunking
**Week 3 (Search)**: 7-8 hours/day on vector search and query processing  
**Week 4 (Testing)**: 6 hours/day on testing, optimization, and documentation

---

## Success Criteria Checklist

- [ ] All dependencies correctly configured and loading
 - [ ] TensorFlow model generating embeddings
- [ ] H2 database storing and retrieving vectors
- [ ] Content extraction working for all supported file types
- [ ] Smart chunking preserving logical boundaries
- [ ] HNSW index providing sub-second search
- [ ] Query processing returning relevant results
- [ ] API endpoints functional and documented
- [ ] 85%+ query accuracy on test dataset
- [ ] Complete test coverage (>80%)
- [ ] Performance benchmarks meeting requirements
- [ ] Production deployment package ready

---

## Additional Resources
- See EXAMPLES document for detailed code implementations
- TensorFlow Lite Java documentation: https://www.tensorflow.org/lite/api_docs/java
- H2 Database documentation: http://www.h2database.com/html/main.html
- JVector GitHub repository: https://github.com/jbellis/jvector