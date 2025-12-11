package com.mybible.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/**
 * ApiBibleService - Integration with api.bible for additional translations
 *
 * API Documentation: https://docs.api.bible/
 * Base URL: https://api.scripture.api.bible/v1
 *
 * Popular Bible IDs:
 * - de4e12af7f28f599-02: KJV (King James Version)
 * - 06125adad2d5898a-01: ASV (American Standard Version)
 * - 9879dbb7cfe39e4d-04: WEB (World English Bible)
 * - 592420522e16049f-01: NIV (New International Version)
 * - 01b29f4b342acc35-01: ESV (English Standard Version)
 * - 65eec8e0b60e656b-01: NLT (New Living Translation)
 * - bba9f40183526463-01: NASB (New American Standard Bible)
 */
public class ApiBibleService {

    private static ApiBibleService instance;
    private String apiKey;
    private boolean configured = false;

    private static final String BASE_URL = "https://api.scripture.api.bible/v1";

    // Cache for Bible metadata
    private final Map<String, BibleInfo> biblesCache = new LinkedHashMap<>();
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL = 3600000; // 1 hour

    // Popular translations to feature
    public static final Map<String, String> POPULAR_BIBLES = new LinkedHashMap<>();
    static {
        POPULAR_BIBLES.put("NIV", "592420522e16049f-01");
        POPULAR_BIBLES.put("ESV", "01b29f4b342acc35-01");
        POPULAR_BIBLES.put("NLT", "65eec8e0b60e656b-01");
        POPULAR_BIBLES.put("NASB", "bba9f40183526463-01");
        POPULAR_BIBLES.put("KJV-API", "de4e12af7f28f599-02");
        POPULAR_BIBLES.put("ASV-API", "06125adad2d5898a-01");
        POPULAR_BIBLES.put("WEB-API", "9879dbb7cfe39e4d-04");
    }

    private ApiBibleService() {}

    public static synchronized ApiBibleService getInstance() {
        if (instance == null) {
            instance = new ApiBibleService();
        }
        return instance;
    }

    /**
     * Configure the service with API key
     */
    public void configure(String apiKey) {
        this.apiKey = apiKey;
        this.configured = apiKey != null && !apiKey.isEmpty();
        System.out.println("[ApiBibleService] Configured (enabled=" + configured + ")");
    }

    public boolean isConfigured() {
        return configured;
    }

    // ========================================================================
    // API Methods
    // ========================================================================

    /**
     * Get list of available Bibles
     * GET /v1/bibles
     */
    public List<BibleInfo> getBibles() throws IOException {
        if (!configured) return Collections.emptyList();

        // Check cache
        if (!biblesCache.isEmpty() && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL) {
            return new ArrayList<>(biblesCache.values());
        }

        String response = makeRequest("/bibles?language=eng");
        if (response == null) return Collections.emptyList();

        // Parse JSON response
        List<BibleInfo> bibles = parseBiblesResponse(response);

        // Update cache
        biblesCache.clear();
        for (BibleInfo bible : bibles) {
            biblesCache.put(bible.id, bible);
        }
        cacheTimestamp = System.currentTimeMillis();

        return bibles;
    }

    /**
     * Get books for a Bible
     * GET /v1/bibles/{bibleId}/books
     */
    public List<BookInfo> getBooks(String bibleId) throws IOException {
        if (!configured) return Collections.emptyList();

        String response = makeRequest("/bibles/" + bibleId + "/books");
        if (response == null) return Collections.emptyList();

        return parseBooksResponse(response);
    }

    /**
     * Get chapters for a book
     * GET /v1/bibles/{bibleId}/books/{bookId}/chapters
     */
    public List<ChapterInfo> getChapters(String bibleId, String bookId) throws IOException {
        if (!configured) return Collections.emptyList();

        String response = makeRequest("/bibles/" + bibleId + "/books/" + bookId + "/chapters");
        if (response == null) return Collections.emptyList();

        return parseChaptersResponse(response);
    }

    /**
     * Get a passage (chapter or verse range)
     * GET /v1/bibles/{bibleId}/passages/{passageId}
     *
     * passageId examples:
     * - "GEN.1" = Genesis chapter 1
     * - "JHN.3.16" = John 3:16
     * - "ROM.8.28-ROM.8.30" = Romans 8:28-30
     */
    public PassageContent getPassage(String bibleId, String passageId) throws IOException {
        if (!configured) return null;

        // content-type can be: html, json, text
        String response = makeRequest("/bibles/" + bibleId + "/passages/" + passageId +
            "?content-type=text&include-notes=false&include-titles=true&include-chapter-numbers=false&include-verse-numbers=true");
        if (response == null) return null;

        return parsePassageResponse(response);
    }

    /**
     * Search for text
     * GET /v1/bibles/{bibleId}/search
     */
    public SearchResult search(String bibleId, String query, int limit) throws IOException {
        if (!configured) return null;

        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String response = makeRequest("/bibles/" + bibleId + "/search?query=" + encodedQuery + "&limit=" + limit);
        if (response == null) return null;

        return parseSearchResponse(response);
    }

    /**
     * Convert standard book name to API book ID
     * e.g., "Genesis" -> "GEN", "1 Corinthians" -> "1CO"
     */
    public static String bookNameToId(String bookName) {
        // Standard abbreviations used by api.bible
        Map<String, String> bookIds = new LinkedHashMap<>();
        bookIds.put("Genesis", "GEN");
        bookIds.put("Exodus", "EXO");
        bookIds.put("Leviticus", "LEV");
        bookIds.put("Numbers", "NUM");
        bookIds.put("Deuteronomy", "DEU");
        bookIds.put("Joshua", "JOS");
        bookIds.put("Judges", "JDG");
        bookIds.put("Ruth", "RUT");
        bookIds.put("1 Samuel", "1SA");
        bookIds.put("2 Samuel", "2SA");
        bookIds.put("1 Kings", "1KI");
        bookIds.put("2 Kings", "2KI");
        bookIds.put("1 Chronicles", "1CH");
        bookIds.put("2 Chronicles", "2CH");
        bookIds.put("Ezra", "EZR");
        bookIds.put("Nehemiah", "NEH");
        bookIds.put("Esther", "EST");
        bookIds.put("Job", "JOB");
        bookIds.put("Psalms", "PSA");
        bookIds.put("Proverbs", "PRO");
        bookIds.put("Ecclesiastes", "ECC");
        bookIds.put("Song of Solomon", "SNG");
        bookIds.put("Isaiah", "ISA");
        bookIds.put("Jeremiah", "JER");
        bookIds.put("Lamentations", "LAM");
        bookIds.put("Ezekiel", "EZK");
        bookIds.put("Daniel", "DAN");
        bookIds.put("Hosea", "HOS");
        bookIds.put("Joel", "JOL");
        bookIds.put("Amos", "AMO");
        bookIds.put("Obadiah", "OBA");
        bookIds.put("Jonah", "JON");
        bookIds.put("Micah", "MIC");
        bookIds.put("Nahum", "NAM");
        bookIds.put("Habakkuk", "HAB");
        bookIds.put("Zephaniah", "ZEP");
        bookIds.put("Haggai", "HAG");
        bookIds.put("Zechariah", "ZEC");
        bookIds.put("Malachi", "MAL");
        bookIds.put("Matthew", "MAT");
        bookIds.put("Mark", "MRK");
        bookIds.put("Luke", "LUK");
        bookIds.put("John", "JHN");
        bookIds.put("Acts", "ACT");
        bookIds.put("Romans", "ROM");
        bookIds.put("1 Corinthians", "1CO");
        bookIds.put("2 Corinthians", "2CO");
        bookIds.put("Galatians", "GAL");
        bookIds.put("Ephesians", "EPH");
        bookIds.put("Philippians", "PHP");
        bookIds.put("Colossians", "COL");
        bookIds.put("1 Thessalonians", "1TH");
        bookIds.put("2 Thessalonians", "2TH");
        bookIds.put("1 Timothy", "1TI");
        bookIds.put("2 Timothy", "2TI");
        bookIds.put("Titus", "TIT");
        bookIds.put("Philemon", "PHM");
        bookIds.put("Hebrews", "HEB");
        bookIds.put("James", "JAS");
        bookIds.put("1 Peter", "1PE");
        bookIds.put("2 Peter", "2PE");
        bookIds.put("1 John", "1JN");
        bookIds.put("2 John", "2JN");
        bookIds.put("3 John", "3JN");
        bookIds.put("Jude", "JUD");
        bookIds.put("Revelation", "REV");

        return bookIds.getOrDefault(bookName, bookName);
    }

    // ========================================================================
    // HTTP Request
    // ========================================================================

    private String makeRequest(String endpoint) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("[ApiBibleService] API error: " + responseCode + " for " + endpoint);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        } finally {
            conn.disconnect();
        }
    }

    // ========================================================================
    // JSON Parsing (simple manual parsing to avoid external dependencies)
    // ========================================================================

    private List<BibleInfo> parseBiblesResponse(String json) {
        List<BibleInfo> bibles = new ArrayList<>();

        // Find data array
        int dataStart = json.indexOf("\"data\":[");
        if (dataStart == -1) return bibles;

        // Parse each bible object
        int pos = dataStart + 8;
        while (pos < json.length()) {
            int objStart = json.indexOf("{", pos);
            if (objStart == -1) break;

            // Find matching closing brace (handle nested objects)
            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd == -1) break;

            String objJson = json.substring(objStart, objEnd + 1);

            String id = extractJsonString(objJson, "id");
            String name = extractJsonString(objJson, "name");
            String abbr = extractJsonString(objJson, "abbreviation");
            String lang = extractJsonString(objJson, "language");

            if (id != null && name != null) {
                bibles.add(new BibleInfo(id, name, abbr != null ? abbr : id, lang));
            }

            pos = objEnd + 1;
        }

        return bibles;
    }

    private List<BookInfo> parseBooksResponse(String json) {
        List<BookInfo> books = new ArrayList<>();

        int dataStart = json.indexOf("\"data\":[");
        if (dataStart == -1) return books;

        int pos = dataStart + 8;
        while (pos < json.length()) {
            int objStart = json.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd == -1) break;

            String objJson = json.substring(objStart, objEnd + 1);

            String id = extractJsonString(objJson, "id");
            String name = extractJsonString(objJson, "name");
            String abbr = extractJsonString(objJson, "abbreviation");

            if (id != null && name != null) {
                books.add(new BookInfo(id, name, abbr));
            }

            pos = objEnd + 1;
        }

        return books;
    }

    private List<ChapterInfo> parseChaptersResponse(String json) {
        List<ChapterInfo> chapters = new ArrayList<>();

        int dataStart = json.indexOf("\"data\":[");
        if (dataStart == -1) return chapters;

        int pos = dataStart + 8;
        while (pos < json.length()) {
            int objStart = json.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd == -1) break;

            String objJson = json.substring(objStart, objEnd + 1);

            String id = extractJsonString(objJson, "id");
            String number = extractJsonString(objJson, "number");

            if (id != null) {
                chapters.add(new ChapterInfo(id, number != null ? number : ""));
            }

            pos = objEnd + 1;
        }

        return chapters;
    }

    private PassageContent parsePassageResponse(String json) {
        int dataStart = json.indexOf("\"data\":{");
        if (dataStart == -1) return null;

        int dataEnd = findMatchingBrace(json, dataStart + 7);
        if (dataEnd == -1) return null;

        String dataJson = json.substring(dataStart + 7, dataEnd + 1);

        String id = extractJsonString(dataJson, "id");
        String reference = extractJsonString(dataJson, "reference");
        String content = extractJsonString(dataJson, "content");

        if (content == null) return null;

        return new PassageContent(id, reference, content);
    }

    private SearchResult parseSearchResponse(String json) {
        SearchResult result = new SearchResult();
        result.verses = new ArrayList<>();

        // Get total from the data object
        String total = extractJsonString(json, "total");
        result.total = total != null ? Integer.parseInt(total) : 0;

        // Find verses array inside data
        int versesStart = json.indexOf("\"verses\":[");
        if (versesStart == -1) return result;

        int pos = versesStart + 10;
        while (pos < json.length()) {
            int objStart = json.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd == -1) break;

            String objJson = json.substring(objStart, objEnd + 1);

            String id = extractJsonString(objJson, "id");
            String reference = extractJsonString(objJson, "reference");
            String text = extractJsonString(objJson, "text");

            if (text != null) {
                result.verses.add(new SearchVerse(id, reference, text));
            }

            pos = objEnd + 1;
        }

        return result;
    }

    private int findMatchingBrace(String json, int start) {
        if (json.charAt(start) != '{') return -1;

        int depth = 0;
        boolean inString = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // Try without quotes (for numbers)
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start == -1) return null;

            start += pattern.length();
            // Skip whitespace
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

            if (start >= json.length()) return null;

            // If it's a number
            if (Character.isDigit(json.charAt(start)) || json.charAt(start) == '-') {
                int end = start;
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-' || json.charAt(end) == '.')) {
                    end++;
                }
                return json.substring(start, end);
            }
            return null;
        }

        start += pattern.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && json.charAt(end - 1) != '\\') {
                break;
            }
            end++;
        }

        if (end > start) {
            String value = json.substring(start, end);
            value = value.replace("\\\"", "\"")
                        .replace("\\/", "/")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
            return value;
        }
        return null;
    }

    // ========================================================================
    // Data Classes
    // ========================================================================

    public static class BibleInfo {
        public final String id;
        public final String name;
        public final String abbreviation;
        public final String language;

        public BibleInfo(String id, String name, String abbreviation, String language) {
            this.id = id;
            this.name = name;
            this.abbreviation = abbreviation;
            this.language = language;
        }
    }

    public static class BookInfo {
        public final String id;
        public final String name;
        public final String abbreviation;

        public BookInfo(String id, String name, String abbreviation) {
            this.id = id;
            this.name = name;
            this.abbreviation = abbreviation;
        }
    }

    public static class ChapterInfo {
        public final String id;
        public final String number;

        public ChapterInfo(String id, String number) {
            this.id = id;
            this.number = number;
        }
    }

    public static class PassageContent {
        public final String id;
        public final String reference;
        public final String content;

        public PassageContent(String id, String reference, String content) {
            this.id = id;
            this.reference = reference;
            this.content = content;
        }
    }

    public static class SearchResult {
        public int total;
        public List<SearchVerse> verses;
    }

    public static class SearchVerse {
        public final String id;
        public final String reference;
        public final String text;

        public SearchVerse(String id, String reference, String text) {
            this.id = id;
            this.reference = reference;
            this.text = text;
        }
    }
}
