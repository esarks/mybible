# MyBible - Personal Bible Study Application

## Project Overview

**MyBible** is a personal Bible study application being built as a Google Cloud-based application using the JAC (Java Architects Companion) RAD framework. The application provides comprehensive Bible study tools including passage lookup, multiple translations, personal notes, and accompanying study resources.

This project leverages JAC's powerful template engine, dynamic compilation, and extensive library support to create a production-ready REST API backend with Google Cloud services integration.

## Application Scope

### Business Domain
Personal Bible study and reference system with:
- **Passage Lookup**: Search and navigate Bible passages by book, chapter, and verse
- **Multiple Translations**: Support for various Bible translations (KJV, NIV, ESV, NASB, etc.)
- **Personal Notes**: Create, edit, and organize study notes linked to passages
- **Cross-References**: Navigate related passages and cross-references
- **Search**: Full-text search across translations and notes
- **Bookmarks & Highlights**: Mark favorite passages and highlight text
- **Study Resources**: Commentaries, concordance, and supplemental materials
- **Reading Plans**: Track daily reading progress

### Target Architecture (3-Tier)
```
┌────────────────────────────────────────────┐
│  PRESENTATION TIER (Clients)               │
│  - Web App (HTML/JavaScript) [PRIMARY]     │
│  - Progressive Web App (PWA)               │
│  - Mobile App (future)                     │
└──────────────────┬─────────────────────────┘
                   │ REST API
┌──────────────────▼─────────────────────────┐
│  APPLICATION TIER (JAC/Google Cloud Run)   │
│  ┌──────────────────────────────────────┐  │
│  │ REST API Endpoints                   │  │
│  │  - Authentication (Firebase/JWT)     │  │
│  │  - Passage Retrieval                 │  │
│  │  - Notes CRUD                        │  │
│  │  - Search                            │  │
│  │  - Bookmarks & Highlights            │  │
│  │  - Reading Plans                     │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │ Business Logic Layer                 │  │
│  │  - User Authorization                │  │
│  │  - Search Indexing                   │  │
│  │  - Note Organization                 │  │
│  └──────────────────────────────────────┘  │
└──────────────────┬─────────────────────────┘
                   │ JDBC / Google APIs
┌──────────────────▼─────────────────────────┐
│  DATA TIER (Google Cloud)                  │
│  - Cloud SQL (PostgreSQL) - User data      │
│  - Cloud Storage - Bible text files        │
│  - Cloud Firestore - Real-time sync (opt)  │
└────────────────────────────────────────────┘
```

## Database Schema Summary

The application uses **core tables** in PostgreSQL (Cloud SQL):

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `users` | User accounts | id, email, name, created_at |
| `translations` | Bible translation metadata | id, code, name, language, copyright |
| `books` | Bible book metadata | id, translation_id, name, abbreviation, testament, order_num |
| `chapters` | Chapter metadata | id, book_id, chapter_num, verse_count |
| `verses` | Bible verse text | id, chapter_id, verse_num, text |
| `notes` | User study notes | id, user_id, passage_ref, content, created_at, updated_at |
| `bookmarks` | User bookmarks | id, user_id, passage_ref, label, created_at |
| `highlights` | Text highlights | id, user_id, passage_ref, color, created_at |
| `reading_plans` | Reading plan definitions | id, name, description, duration_days |
| `reading_plan_entries` | Daily readings | id, plan_id, day_num, passage_ref |
| `user_reading_progress` | User's plan progress | id, user_id, plan_id, current_day, last_read_date |
| `cross_references` | Cross-reference links | id, source_ref, target_ref |
| `tags` | Note/bookmark tags | id, user_id, name |
| `note_tags` | Note-tag associations | note_id, tag_id |

### Passage Reference Format
```
{book_abbrev}.{chapter}.{verse}[-{end_verse}]
Examples:
  - Gen.1.1       (Genesis 1:1)
  - John.3.16     (John 3:16)
  - Ps.23.1-6     (Psalm 23:1-6)
  - Rom.8.28-39   (Romans 8:28-39)
```

## Google Cloud Services

### Cloud Run
- Hosts the JAC application as a containerized service
- Auto-scaling based on traffic
- HTTPS endpoint with custom domain support

### Cloud SQL (PostgreSQL)
- User data: notes, bookmarks, highlights, reading progress
- Bible metadata and potentially full text (or use Cloud Storage)
- Connection via Cloud SQL Proxy or private IP

### Cloud Storage
- Bible translation files (if not in database)
- User uploads (if needed)
- Static assets for web client

### Firebase Authentication (Optional)
- Google Sign-In
- Email/Password authentication
- JWT token validation

### Cloud Firestore (Optional)
- Real-time sync across devices
- Offline-first capability for PWA

## JAC Implementation Approach

### Why JAC?

JAC provides ideal capabilities for this project:

1. **Database Integration**: Native PostgreSQL support via JDBC
2. **REST API Development**: Jetty embedded server with servlet support
3. **JSON Processing**: Built-in XML/JSON parsing and generation
4. **Template Engine**: Mix Java code with JAC directives for clean code generation
5. **Dynamic Compilation**: Rapid development cycle with automatic compilation
6. **Google Cloud Compatible**: Containerized deployment to Cloud Run

### JAC Script Structure

MyBible will use the following JAC component structure:

```
app/com/mybible/
├── CLAUDE.md                    # This file
├── specs/
│   └── requirements.md          # Detailed requirements
├── server/
│   ├── MyBibleServer.script     # Main Jetty server bootstrap
│   └── Config.script            # Configuration loader
├── api/
│   ├── auth/
│   │   ├── LoginEndpoint.script
│   │   └── TokenValidation.script
│   ├── passages/
│   │   ├── GetPassageEndpoint.script
│   │   ├── SearchEndpoint.script
│   │   └── CrossRefEndpoint.script
│   ├── notes/
│   │   ├── NotesEndpoint.script
│   │   └── TagsEndpoint.script
│   ├── bookmarks/
│   │   ├── BookmarksEndpoint.script
│   │   └── HighlightsEndpoint.script
│   └── reading/
│       ├── PlansEndpoint.script
│       └── ProgressEndpoint.script
├── services/
│   ├── PassageService.script
│   ├── SearchService.script
│   ├── NotesService.script
│   ├── BookmarkService.script
│   └── ReadingPlanService.script
├── db/
│   ├── DatabaseInit.script      # Schema creation
│   ├── PassageRepo.script
│   ├── NotesRepo.script
│   ├── BookmarkRepo.script
│   └── ReadingPlanRepo.script
├── util/
│   ├── JWTUtil.script
│   ├── PassageRefParser.script
│   └── JsonUtil.script
├── data/
│   ├── translations/            # Bible translation data
│   ├── crossrefs/               # Cross-reference data
│   └── test/                    # Sample data for testing
├── config/
│   ├── Properties.xml           # Database/server config
│   └── Properties-dev.xml       # Development config
├── docker/
│   ├── Dockerfile               # Container build
│   └── docker-compose.yml       # Local development
└── deploy/
    ├── cloudbuild.yaml          # Cloud Build config
    └── cloud-run-deploy.md      # Deployment instructions
```

## API Design

### Endpoint Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/passages/{ref}` | Get passage text |
| GET | `/api/passages/{ref}/translations` | Get passage in multiple translations |
| GET | `/api/search?q={query}&translation={code}` | Search Bible text |
| GET | `/api/crossrefs/{ref}` | Get cross-references |
| GET | `/api/notes` | List user's notes |
| POST | `/api/notes` | Create note |
| PUT | `/api/notes/{id}` | Update note |
| DELETE | `/api/notes/{id}` | Delete note |
| GET | `/api/bookmarks` | List bookmarks |
| POST | `/api/bookmarks` | Create bookmark |
| DELETE | `/api/bookmarks/{id}` | Delete bookmark |
| GET | `/api/highlights` | List highlights |
| POST | `/api/highlights` | Create highlight |
| DELETE | `/api/highlights/{id}` | Delete highlight |
| GET | `/api/plans` | List reading plans |
| GET | `/api/plans/{id}/progress` | Get user's progress |
| POST | `/api/plans/{id}/progress` | Update progress |

### Request/Response Format

**Success Response:**
```json
{
  "success": true,
  "data": { /* response payload */ },
  "timestamp": "2025-12-10T10:00:00Z"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "ERR_NOT_FOUND",
    "message": "Passage not found",
    "details": "Book 'Hezekiah' does not exist"
  },
  "timestamp": "2025-12-10T10:00:00Z"
}
```

### Authentication Header
```
Authorization: Bearer <JWT_TOKEN>
```

## Configuration Management

### Properties.xml Structure
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
      </pool>
    </connection>
  </database>

  <server>
    <port>8080</port>
    <host>0.0.0.0</host>
    <contextPath>/api</contextPath>
  </server>

  <jwt>
    <secret>your-256-bit-secret-key-here</secret>
    <expirationMs>86400000</expirationMs> <!-- 24 hours -->
  </jwt>

  <translations>
    <default>KJV</default>
    <available>KJV,NIV,ESV,NASB,NLT</available>
  </translations>
</properties>
```

### Google Cloud Configuration
```xml
<gcloud>
  <project>mybible-project-id</project>
  <region>us-central1</region>
  <cloudsql>
    <instance>mybible-project-id:us-central1:mybible-db</instance>
  </cloudsql>
  <storage>
    <bucket>mybible-data</bucket>
  </storage>
</gcloud>
```

## Development Workflow

### Phase 1: Foundation
1. **Database Setup**
   - Create Cloud SQL instance or local PostgreSQL
   - Design and implement schema
   - Load Bible translation data

2. **Server Bootstrap**
   - Create `MyBibleServer.script` with Jetty
   - Configure routing and CORS
   - Add request logging

3. **Core Passage API**
   - Implement passage retrieval
   - Add translation support
   - Build search functionality

### Phase 2: User Features
1. **Notes System**
   - Notes CRUD operations
   - Tag management
   - Search within notes

2. **Bookmarks & Highlights**
   - Bookmark management
   - Highlight colors and categories

3. **Reading Plans**
   - Plan definitions
   - Progress tracking
   - Daily reminders

### Phase 3: Production Deployment
1. **Containerization**
   - Create Dockerfile
   - Test locally with docker-compose

2. **Cloud Deployment**
   - Configure Cloud Build
   - Deploy to Cloud Run
   - Set up Cloud SQL connection

3. **Web Client**
   - Build responsive web interface
   - Implement PWA features
   - Test offline capability

## Bible Data Sources

### Public Domain Translations
- **KJV** (King James Version) - Public domain
- **ASV** (American Standard Version) - Public domain
- **WEB** (World English Bible) - Public domain

### Licensed Translations
Note: Ensure proper licensing for non-public domain translations:
- NIV, ESV, NASB, NLT require licensing agreements
- Consider API services (e.g., API.Bible, Bible Gateway API)

### Data Format Options
1. **Database Storage**: Store verses directly in PostgreSQL
2. **File-Based**: Load from JSON/XML files in Cloud Storage
3. **External API**: Query third-party Bible APIs

## Quick Start Commands

### 1. Setup Local Database
```powershell
# Create PostgreSQL database
psql -U postgres
CREATE DATABASE mybible_db;
CREATE USER mybible_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE mybible_db TO mybible_user;
\q
```

### 2. Initialize Schema
```powershell
cd C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app\com\mybible
..\..\..\jacBuild24\bin\jac.bat db\DatabaseInit.script
```

### 3. Start Server
```powershell
..\..\..\jacBuild24\bin\jac.bat server\MyBibleServer.script
```

### 4. Test API
```powershell
# Get passage
Invoke-RestMethod -Uri "http://localhost:8080/api/passages/John.3.16"

# Search
Invoke-RestMethod -Uri "http://localhost:8080/api/search?q=love&translation=KJV"
```

## Google Cloud Deployment

### Cloud Run Deployment
```bash
# Build and push container
gcloud builds submit --config=deploy/cloudbuild.yaml

# Deploy to Cloud Run
gcloud run deploy mybible-api \
  --image gcr.io/PROJECT_ID/mybible-api \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --add-cloudsql-instances PROJECT_ID:us-central1:mybible-db
```

### Cloud SQL Setup
```bash
# Create instance
gcloud sql instances create mybible-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1

# Create database
gcloud sql databases create mybible_db --instance=mybible-db

# Create user
gcloud sql users create mybible_user \
  --instance=mybible-db \
  --password=secure_password
```

## Resources

### JAC Documentation
- Main JAC CLAUDE.md: `C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\CLAUDE.md`
- Example scripts: `app/com/esarks/examples/`
- Database demo: `app/com/esarks/examples/databaseintegration/`
- REST API demo: `app/com/esarks/examples/restapi/`
- AllowanceAlley reference: `app/com/allowancealley/`

### Bible Data Resources
- **SWORD Project**: https://crosswire.org/sword/
- **Open Scriptures**: https://github.com/openscriptures
- **API.Bible**: https://scripture.api.bible/
- **Bible Gateway API**: https://www.biblegateway.com/
- **Digital Bible Library**: https://thedigitalbiblelibrary.org/

### Google Cloud Documentation
- Cloud Run: https://cloud.google.com/run/docs
- Cloud SQL: https://cloud.google.com/sql/docs
- Cloud Storage: https://cloud.google.com/storage/docs
- Firebase Auth: https://firebase.google.com/docs/auth

## Development Status

### Current Phase: Project Setup
- [x] CLAUDE.md created
- [ ] Database schema designed
- [ ] Bible data source selected
- [ ] Server bootstrap implemented
- [ ] Core passage API implemented
- [ ] Notes system implemented
- [ ] Google Cloud deployment configured

### Next Steps
1. Design detailed database schema
2. Select and acquire Bible translation data
3. Create `DatabaseInit.script` with schema
4. Implement `MyBibleServer.script` with Jetty
5. Build passage retrieval endpoints
6. Test end-to-end passage lookup

## Notes for Claude Code

When working on this project:

1. **Reference AllowanceAlley patterns** for proven JAC implementations
2. **Use JAC examples** as templates (especially database and REST API demos)
3. **Follow JAC syntax** - mix Java code with `%>...<%` output blocks and `<!%var!>` interpolation
4. **Database-first approach** - create schema first, then build repositories, services, and endpoints
5. **Consider licensing** - be careful with Bible translation copyright requirements
6. **Google Cloud integration** - plan for Cloud Run deployment from the start
7. **Test incrementally** - create test scripts for each component
8. **Keep it modular** - one script per endpoint/service/repository
9. **Use prepared statements** - prevent SQL injection in all database queries
10. **Log extensively** - use Log4j for debugging and monitoring

## License

This project is part of the Architects Companion suite, licensed to Architects of Software Design, Corp.

---

**Document Version:** 1.0
**Last Updated:** 2025-12-10
**Status:** Project Setup - Ready for Development
