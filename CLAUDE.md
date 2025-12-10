# MyBible - Personal Bible Study Application

## Project Overview

**MyBible** is a personal Bible study application built using the JAC (Java Architects Companion) RAD framework, following the proven patterns from **AllowanceAlley**. The application provides comprehensive Bible study tools including passage lookup, multiple translations, personal notes, and accompanying study resources.

This project uses JAC's 6 generators (DDL, JEO, Service, Report, Frame, Dtable) and the MakeAll workflow to create a production-ready REST API backend deployed to Google Cloud Run.

---

## Reference Implementation: AllowanceAlley

MyBible follows the **AllowanceAlley** architecture patterns:
- **Location**: `jac2024/app/com/allowancealley/`
- **Documentation**: `jac2024/app/com/allowancealley/CLAUDE.md`
- **Patterns Guide**: `jac2024/app/com/allowancealley/jac-patterns.md`

### Key AllowanceAlley Patterns to Follow

1. **Monolithic Router** - Single `MyBibleRouter.script` (~10K+ lines) containing all REST endpoints
2. **JEO + CRUD Pattern** - Generated entity objects with CRUD operations
3. **JWT Authentication** - Stateless authentication with HS256 tokens
4. **ServiceJeo Pattern** - Request/response wrapper for database operations
5. **Row-Level Security** - All queries include user_id in WHERE clause
6. **HTML + JSON APIs** - Support both web forms and REST JSON responses
7. **6-Phase Build** - Multi-phase compilation with MakeAll orchestration

---

## Bible Data Sources (Available)

### Included Bible Translations

The project includes **10 Bible translation files** in the `bibles/` folder, loaded from `my-bible-app`:

| File | Translation | Size | Description |
|------|-------------|------|-------------|
| `kjv.json` | King James Version | ~4.5 MB | Public domain (1611/1769) |
| `asv.json` | American Standard Version | ~4.3 MB | Public domain (1901) |
| `web.json` | World English Bible | ~4.4 MB | Public domain |
| `bbe.json` | Bible in Basic English | ~3.8 MB | Public domain |
| `darby.json` | Darby Translation | ~4.2 MB | Public domain |
| `ylt.json` | Young's Literal Translation | ~4.3 MB | Public domain |
| `webbe.json` | WEB British Edition | ~4.4 MB | Public domain |
| `oeb-us.json` | Open English Bible (US) | ~2.1 MB | Public domain |
| `oeb-cw.json` | Open English Bible (Commonwealth) | ~2.1 MB | Public domain |
| `clementine.json` | Clementine Vulgate (Latin) | ~4.1 MB | Public domain |

### Bible JSON Format

Each JSON file contains a **flat array of verses**:

```json
[
  {
    "book_name": "Genesis",
    "chapter": 1,
    "verse": 1,
    "text": "In the beginning God created the heaven and the earth."
  },
  {
    "book_name": "Genesis",
    "chapter": 1,
    "verse": 2,
    "text": "And the earth was without form, and void; and darkness was upon the face of the deep..."
  }
]
```

### Field Definitions

| Field | Type | Description |
|-------|------|-------------|
| `book_name` | String | Full book name (e.g., "Genesis", "1 Corinthians") |
| `chapter` | Integer | Chapter number (1-based) |
| `verse` | Integer | Verse number (1-based) |
| `text` | String | Full verse text |

### Bible Statistics

- **66 Books** (39 OT + 27 NT)
- **1,189 Chapters**
- **31,102 Verses** (KJV count)
- **~4.5 MB** per translation (uncompressed JSON)

---

## Project Structure (AllowanceAlley Pattern)

```
app/com/mybible/
├── CLAUDE.md                      # This documentation file
├── jac-patterns.md                # JAC syntax patterns for this project
│
├── bin/                           # Build scripts (6-phase pipeline)
│   ├── allPhases.bat              # Master build orchestrator
│   ├── phase1.bat                 # Clean build
│   ├── phase2.bat                 # Generate data layer
│   ├── phase3.bat                 # Compile generators
│   ├── phase3.5.bat               # Execute generators
│   ├── phase3.55.bat              # Compile utilities
│   ├── phase3.6.bat               # Recompile server
│   └── SetMyBible.bat             # Environment setup
│
├── config/                        # Runtime configuration
│   └── properties/
│       ├── Properties.xml         # Main configuration
│       └── Properties-docker.xml  # Docker-specific config
│
├── data/                          # Data layer (JEO + CRUD generation)
│   ├── MyBibleMake.xml            # MakeAll component inventory
│   ├── MyBibleDdl.xml             # Database schema definitions
│   │
│   ├── AUTH_USERS.new             # Generated JEO for users
│   ├── AUTH_USERSCrud.new         # Generated CRUD operations
│   ├── TRANSLATIONS.new           # Bible translation metadata
│   ├── TRANSLATIONSCrud.new
│   ├── BOOKS.new                  # Bible book metadata
│   ├── BOOKSCrud.new
│   ├── VERSES.new                 # Bible verse text
│   ├── VERSESCrud.new
│   ├── NOTES.new                  # User study notes
│   ├── NOTESCrud.new
│   ├── BOOKMARKS.new              # User bookmarks
│   ├── BOOKMARKSCrud.new
│   ├── HIGHLIGHTS.new             # User highlights
│   ├── HIGHLIGHTSCrud.new
│   ├── READING_PLANS.new          # Reading plan definitions
│   ├── READING_PLANSCrud.new
│   ├── USER_READING_PROGRESS.new  # User's plan progress
│   ├── USER_READING_PROGRESSCrud.new
│   ├── TAGS.new                   # Note/bookmark tags
│   ├── TAGSCrud.new
│   └── ACTIVITY_LEDGER.new        # Audit trail
│
├── server/                        # REST API server
│   └── MyBibleRouter.script       # Main Jetty router (all endpoints)
│
├── util/                          # Utility classes (Java)
│   ├── HashUtil.java              # SHA-256 password hashing
│   ├── JWTUtil.java               # JWT token generation/validation
│   ├── JsonUtil.java              # JSON parsing/generation
│   ├── RequestContext.java        # Authentication context extraction
│   ├── RequestLogger.java         # Request/response logging
│   ├── BibleLoader.java           # Load Bible JSON files
│   └── PassageParser.java         # Parse passage references
│
├── bibles/                        # Bible translation JSON files
│   ├── kjv.json                   # King James Version
│   ├── asv.json                   # American Standard Version
│   ├── web.json                   # World English Bible
│   ├── bbe.json                   # Bible in Basic English
│   ├── darby.json                 # Darby Translation
│   ├── ylt.json                   # Young's Literal Translation
│   ├── webbe.json                 # WEB British Edition
│   ├── oeb-us.json                # Open English Bible (US)
│   ├── oeb-cw.json                # Open English Bible (Commonwealth)
│   └── clementine.json            # Clementine Vulgate (Latin)
│
├── docker/                        # Docker deployment
│   ├── Dockerfile                 # Container build
│   ├── docker-compose.yml         # Local dev orchestration
│   ├── docker-entrypoint.sh       # Container startup
│   └── .env.template              # Environment variables template
│
└── deploy/                        # Google Cloud deployment
    ├── cloudbuild.yaml            # Cloud Build config
    └── cloud-run-deploy.md        # Deployment instructions
```

---

## Database Schema (11 Tables)

### Schema Design (MyBibleDdl.xml)

```xml
<schemas>
  <!-- User Authentication -->
  <table name="AUTH_USERS">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="EMAIL" type="VARCHAR" size="255" required="true" unique="true"/>
    <column name="PASSWORD_HASH" type="VARCHAR" size="64" required="true"/>
    <column name="NAME" type="VARCHAR" size="255"/>
    <column name="CREATED_AT" type="TIMESTAMP" default="NOW()"/>
    <column name="LAST_LOGIN" type="TIMESTAMP"/>
    <column name="EMAIL_VERIFIED" type="BOOLEAN" default="false"/>
  </table>

  <!-- Bible Translation Metadata -->
  <table name="TRANSLATIONS">
    <column name="ID" type="UUID" primary="true"/>
    <column name="CODE" type="VARCHAR" size="20" required="true" unique="true"/>
    <column name="NAME" type="VARCHAR" size="255" required="true"/>
    <column name="LANGUAGE" type="VARCHAR" size="50"/>
    <column name="COPYRIGHT" type="TEXT"/>
    <column name="VERSE_COUNT" type="INTEGER"/>
  </table>

  <!-- Bible Book Metadata -->
  <table name="BOOKS">
    <column name="ID" type="UUID" primary="true"/>
    <column name="TRANSLATION_ID" type="UUID" foreignKey="TRANSLATIONS(ID)"/>
    <column name="NAME" type="VARCHAR" size="100" required="true"/>
    <column name="ABBREVIATION" type="VARCHAR" size="10"/>
    <column name="TESTAMENT" type="VARCHAR" size="10"/> <!-- OT or NT -->
    <column name="ORDER_NUM" type="INTEGER"/>
    <column name="CHAPTER_COUNT" type="INTEGER"/>
  </table>

  <!-- Bible Verses (Optional - can load from JSON) -->
  <table name="VERSES">
    <column name="ID" type="UUID" primary="true"/>
    <column name="TRANSLATION_ID" type="UUID" foreignKey="TRANSLATIONS(ID)"/>
    <column name="BOOK_NAME" type="VARCHAR" size="100"/>
    <column name="CHAPTER" type="INTEGER"/>
    <column name="VERSE" type="INTEGER"/>
    <column name="TEXT" type="TEXT"/>
    <index name="idx_verses_lookup" columns="TRANSLATION_ID,BOOK_NAME,CHAPTER,VERSE"/>
  </table>

  <!-- User Study Notes -->
  <table name="NOTES">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID" foreignKey="AUTH_USERS(ID)" onDelete="CASCADE"/>
    <column name="PASSAGE_REF" type="VARCHAR" size="100"/> <!-- e.g., "John.3.16" -->
    <column name="TITLE" type="VARCHAR" size="255"/>
    <column name="CONTENT" type="TEXT"/>
    <column name="CREATED_AT" type="TIMESTAMP" default="NOW()"/>
    <column name="UPDATED_AT" type="TIMESTAMP"/>
    <index name="idx_notes_user" columns="USER_ID"/>
    <index name="idx_notes_passage" columns="PASSAGE_REF"/>
  </table>

  <!-- User Bookmarks -->
  <table name="BOOKMARKS">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID" foreignKey="AUTH_USERS(ID)" onDelete="CASCADE"/>
    <column name="PASSAGE_REF" type="VARCHAR" size="100"/>
    <column name="LABEL" type="VARCHAR" size="255"/>
    <column name="CREATED_AT" type="TIMESTAMP" default="NOW()"/>
    <index name="idx_bookmarks_user" columns="USER_ID"/>
  </table>

  <!-- User Highlights -->
  <table name="HIGHLIGHTS">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID" foreignKey="AUTH_USERS(ID)" onDelete="CASCADE"/>
    <column name="PASSAGE_REF" type="VARCHAR" size="100"/>
    <column name="COLOR" type="VARCHAR" size="20"/> <!-- yellow, green, blue, pink -->
    <column name="CREATED_AT" type="TIMESTAMP" default="NOW()"/>
    <index name="idx_highlights_user" columns="USER_ID"/>
  </table>

  <!-- Reading Plan Definitions -->
  <table name="READING_PLANS">
    <column name="ID" type="UUID" primary="true"/>
    <column name="NAME" type="VARCHAR" size="255" required="true"/>
    <column name="DESCRIPTION" type="TEXT"/>
    <column name="DURATION_DAYS" type="INTEGER"/>
    <column name="IS_PUBLIC" type="BOOLEAN" default="true"/>
  </table>

  <!-- Reading Plan Entries (Daily Readings) -->
  <table name="READING_PLAN_ENTRIES">
    <column name="ID" type="UUID" primary="true"/>
    <column name="PLAN_ID" type="UUID" foreignKey="READING_PLANS(ID)" onDelete="CASCADE"/>
    <column name="DAY_NUM" type="INTEGER"/>
    <column name="PASSAGE_REF" type="VARCHAR" size="100"/>
    <column name="TITLE" type="VARCHAR" size="255"/>
  </table>

  <!-- User Reading Progress -->
  <table name="USER_READING_PROGRESS">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID" foreignKey="AUTH_USERS(ID)" onDelete="CASCADE"/>
    <column name="PLAN_ID" type="UUID" foreignKey="READING_PLANS(ID)"/>
    <column name="CURRENT_DAY" type="INTEGER" default="1"/>
    <column name="STARTED_AT" type="TIMESTAMP" default="NOW()"/>
    <column name="LAST_READ_DATE" type="DATE"/>
  </table>

  <!-- Tags for Notes/Bookmarks -->
  <table name="TAGS">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID" foreignKey="AUTH_USERS(ID)" onDelete="CASCADE"/>
    <column name="NAME" type="VARCHAR" size="100"/>
    <unique columns="USER_ID,NAME"/>
  </table>

  <!-- Activity Ledger (Audit Trail) -->
  <table name="ACTIVITY_LEDGER">
    <column name="ID" type="UUID" primary="true" default="gen_random_uuid()"/>
    <column name="USER_ID" type="UUID"/>
    <column name="EVENT_TYPE" type="VARCHAR" size="50"/>
    <column name="EVENT_DATE" type="TIMESTAMP" default="NOW()"/>
    <column name="REFERENCE_TYPE" type="VARCHAR" size="50"/>
    <column name="REFERENCE_ID" type="UUID"/>
    <column name="NOTES" type="TEXT"/>
  </table>
</schemas>
```

---

## REST API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Login with email/password |
| POST | `/auth/logout` | Logout (invalidate token) |
| GET | `/auth/verify-email` | Email verification |
| POST | `/auth/forgot-password` | Password reset request |
| GET | `/health` | Health check |

### Bible Passage Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/translations` | List available translations |
| GET | `/api/books` | List books in a translation |
| GET | `/api/passages/{ref}` | Get passage (e.g., `/api/passages/John.3.16`) |
| GET | `/api/passages/{ref}/parallel` | Get passage in multiple translations |
| GET | `/api/chapters/{book}/{chapter}` | Get entire chapter |
| GET | `/api/search?q={query}&t={translation}` | Full-text search |

### User Content Endpoints (Authenticated)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notes` | List user's notes |
| POST | `/api/notes` | Create note |
| GET | `/api/notes/{id}` | Get note by ID |
| PUT | `/api/notes/{id}` | Update note |
| DELETE | `/api/notes/{id}` | Delete note |
| GET | `/api/bookmarks` | List bookmarks |
| POST | `/api/bookmarks` | Create bookmark |
| DELETE | `/api/bookmarks/{id}` | Delete bookmark |
| GET | `/api/highlights` | List highlights |
| POST | `/api/highlights` | Create highlight |
| DELETE | `/api/highlights/{id}` | Delete highlight |

### Reading Plan Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/plans` | List available reading plans |
| GET | `/api/plans/{id}` | Get plan details |
| POST | `/api/plans/{id}/start` | Start a reading plan |
| GET | `/api/plans/{id}/progress` | Get user's progress |
| POST | `/api/plans/{id}/complete-day` | Mark day as complete |

---

## JAC Script Patterns

### Router Endpoint Pattern (from AllowanceAlley)

```java
// GET /api/passages/{ref}
lContextHandler.addServlet(new ServletHolder(
  new jakarta.servlet.http.HttpServlet() {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

      // Extract passage reference from path
      String pathInfo = request.getPathInfo();
      String passageRef = pathInfo.substring(1); // Remove leading /

      // Parse reference (e.g., "John.3.16" -> book=John, chapter=3, verse=16)
      com.mybible.util.PassageParser parser = new com.mybible.util.PassageParser();
      parser.parse(passageRef);

      // Load from Bible JSON or database
      String translation = request.getParameter("t");
      if (translation == null) translation = "kjv";

      // Get verse(s)
      com.esarks.arm.model.jeo.ServiceJeo jeo = new com.esarks.arm.model.jeo.ServiceJeo();
      jeo.getRequest().setWhereClause(
        "BOOK_NAME = '" + parser.getBookName().replace("'", "''") + "' " +
        "AND CHAPTER = " + parser.getChapter() + " " +
        "AND VERSE = " + parser.getVerse()
      );
      versesCrud.readVERSES(jeo);

      // Return JSON response
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      com.mybible.util.JsonUtil.writeResponse(response.getWriter(), jeo);
    }
  }
), "/api/passages/*");
```

### CRUD Service Pattern

```java
// Read operation
com.esarks.arm.model.jeo.ServiceJeo jeo = new com.esarks.arm.model.jeo.ServiceJeo();
jeo.getRequest().setWhereClause("USER_ID = '" + userId + "'");
notesCrud.readNOTES(jeo);

// Check for errors
if (jeo.getReply().getSeverity() > 0) {
  // Handle error
  response.setStatus(500);
  return;
}

// Get results
ArrayList results = jeo.getReply().getJeoByInstanceName("com.mybible.data.NOTES");

// Insert operation
com.mybible.data.NOTES newNote = new com.mybible.data.NOTES("note");
newNote.setUSER_ID(userId);
newNote.setPASSAGE_REF(passageRef);
newNote.setCONTENT(content);
notesCrud.batchCreateNOTES(jeo, new NOTES[]{newNote});
```

### JWT Authentication Pattern

```java
// Extract JWT from request
com.mybible.util.RequestContext ctx = com.mybible.util.RequestContext.fromRequest(request);

if (!ctx.isAuthenticated()) {
  response.setStatus(401);
  response.getWriter().write("{\"error\":\"Unauthorized\"}");
  return;
}

String userId = ctx.getUserId();
// All queries now include: WHERE USER_ID = 'userId'
```

---

## Configuration (Properties.xml)

```xml
<?xml version="1.0"?>
<properties>
  <database>
    <connection id="mybible">
      <driver>org.postgresql.Driver</driver>
      <url>jdbc:postgresql://localhost:5432/mybible_db</url>
      <username>mybible_user</username>
      <password>secure_password</password>
      <pool>
        <minConnections>5</minConnections>
        <maxConnections>20</maxConnections>
        <idleTimeoutMs>300000</idleTimeoutMs>
      </pool>
    </connection>
  </database>

  <server>
    <http>
      <port>8080</port>
      <host>0.0.0.0</host>
    </http>
    <cors>
      <allowedOrigins>*</allowedOrigins>
      <allowedMethods>GET,POST,PUT,DELETE,OPTIONS</allowedMethods>
      <allowedHeaders>Content-Type,Authorization</allowedHeaders>
    </cors>
  </server>

  <auth>
    <jwt>
      <algorithm>HS256</algorithm>
      <expirationMs>86400000</expirationMs> <!-- 24 hours -->
    </jwt>
    <password>
      <algorithm>SHA-256</algorithm>
    </password>
  </auth>

  <bible>
    <defaultTranslation>kjv</defaultTranslation>
    <dataDir>bibles</dataDir>
    <cacheEnabled>true</cacheEnabled>
  </bible>

  <logging>
    <level>INFO</level>
    <sqlStatements>true</sqlStatements>
    <slowQueryThresholdMs>1000</slowQueryThresholdMs>
  </logging>
</properties>
```

---

## Build System (6-Phase Pipeline)

Following AllowanceAlley's build pattern:

### Phase 0: Clean Build
- Remove old class files and generated artifacts

### Phase 1: Generate Data Layer
- JAC processes MyBibleDdl.xml
- Generates JEO classes and CRUD methods
- Produces .new files and compiled classes

### Phase 2: Generate Application Layer
- Generate form/frame templates (if any)

### Phase 3: Compile Generator Scripts
- Compile all .script files to Java

### Phase 3.5: Execute Generators
- Run compiled generators
- Produce final Java source files

### Phase 3.55: Compile Utility Classes
```batch
javac HashUtil.java
javac JsonUtil.java
javac JWTUtil.java
javac BibleLoader.java
javac PassageParser.java
javac RequestContext.java
javac RequestLogger.java
```

### Phase 3.6: Recompile Server Scripts
- Recompile MyBibleRouter after all classes exist

### Phase 4: Verify Build
- Check all artifacts were created

### Build Command
```batch
cd C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app\com\mybible
bin\allPhases.bat
```

---

## Quick Start

### 1. Set Environment
```batch
cd C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app\com\mybible
bin\SetMyBible.bat
```

### 2. Create Database
```sql
CREATE DATABASE mybible_db;
CREATE USER mybible_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE mybible_db TO mybible_user;
```

### 3. Build Project
```batch
bin\allPhases.bat
```

### 4. Start Server
```batch
..\..\..\jacBuild24\bin\JrunDirect.bat server\StartServer.jrun
```

### 5. Test API
```powershell
# Health check
Invoke-RestMethod -Uri "http://localhost:8080/health"

# Get passage
Invoke-RestMethod -Uri "http://localhost:8080/api/passages/John.3.16"

# Search
Invoke-RestMethod -Uri "http://localhost:8080/api/search?q=love&t=kjv"
```

---

## Development Status

### Phase 1: Foundation (Current)
- [x] CLAUDE.md created with AllowanceAlley patterns
- [x] Bible JSON files copied to bibles/ folder
- [ ] Create MyBibleDdl.xml schema definitions
- [ ] Generate JEO and CRUD classes
- [ ] Create Properties.xml configuration
- [ ] Implement utility classes (HashUtil, JWTUtil, etc.)

### Phase 2: Core API
- [ ] Create MyBibleRouter.script
- [ ] Implement Bible passage endpoints
- [ ] Implement search functionality
- [ ] Add translation support

### Phase 3: User Features
- [ ] Implement authentication endpoints
- [ ] Notes CRUD
- [ ] Bookmarks CRUD
- [ ] Highlights CRUD
- [ ] Reading plans

### Phase 4: Deployment
- [ ] Create Dockerfile
- [ ] Configure docker-compose.yml
- [ ] Deploy to Google Cloud Run
- [ ] Set up Cloud SQL

---

## Resources

### JAC Documentation
- **Wiki Home**: `ArchitectsCompanion.wiki/Home.md`
- **How to Write Scripts**: `ArchitectsCompanion.wiki/HowToWriteScript.md`
- **AllowanceAlley Patterns**: `ArchitectsCompanion.wiki/AllowanceAlley.md`
- **Generator Reference**: `ArchitectsCompanion.wiki/AllPhases.md`
- **Database Setup**: `ArchitectsCompanion.wiki/Database-Setup.md`

### AllowanceAlley Reference
- **Main Docs**: `app/com/allowancealley/CLAUDE.md`
- **Patterns**: `app/com/allowancealley/jac-patterns.md`
- **Router**: `app/com/allowancealley/server/AllowanceAlleyRouter.script`
- **Data Layer**: `app/com/allowancealley/data/`

### Bible Data Resources
- **Local Data**: `bibles/` folder (10 translations included)
- **API.Bible**: https://scripture.api.bible/
- **Open Scriptures**: https://github.com/openscriptures

---

## License

This project is part of the Architects Companion suite, licensed to Architects of Software Design, Corp.

---

**Document Version:** 2.0
**Last Updated:** 2025-12-10
**Status:** Phase 1 - Foundation Setup
**Reference Implementation:** AllowanceAlley
