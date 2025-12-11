package com.mybible.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BibleService - Loads and serves Bible translation data from JSON files
 *
 * JSON Structure:
 * {
 *   "metadata": { "name": "...", "shortname": "KJV", ... },
 *   "verses": [
 *     { "book_name": "Genesis", "chapter": 1, "verse": 1, "text": "..." },
 *     ...
 *   ]
 * }
 */
public class BibleService {

    private static BibleService instance;
    private final Map<String, BibleTranslation> translations = new ConcurrentHashMap<>();
    private final Map<String, TranslationMetadata> metadata = new ConcurrentHashMap<>();
    private String biblesPath;
    private boolean loaded = false;

    // Standard book order (66 books)
    public static final String[] BOOK_ORDER = {
        // Old Testament (39)
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles",
        "Ezra", "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
        "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk",
        "Zephaniah", "Haggai", "Zechariah", "Malachi",
        // New Testament (27)
        "Matthew", "Mark", "Luke", "John", "Acts",
        "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
        "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    };

    // Standard chapter counts for each book (used as fallback)
    public static final int[] CHAPTER_COUNTS = {
        // Old Testament (39)
        50, 40, 27, 36, 34,  // Genesis, Exodus, Leviticus, Numbers, Deuteronomy
        24, 21, 4, 31, 24,   // Joshua, Judges, Ruth, 1 Samuel, 2 Samuel
        22, 25, 29, 36,      // 1 Kings, 2 Kings, 1 Chronicles, 2 Chronicles
        10, 13, 10, 42, 150, 31, // Ezra, Nehemiah, Esther, Job, Psalms, Proverbs
        12, 8, 66, 52, 5,    // Ecclesiastes, Song of Solomon, Isaiah, Jeremiah, Lamentations
        48, 12, 14, 3, 9,    // Ezekiel, Daniel, Hosea, Joel, Amos
        1, 4, 7, 3, 3,       // Obadiah, Jonah, Micah, Nahum, Habakkuk
        3, 2, 14, 4,         // Zephaniah, Haggai, Zechariah, Malachi
        // New Testament (27)
        28, 16, 24, 21, 28,  // Matthew, Mark, Luke, John, Acts
        16, 16, 13, 6, 6,    // Romans, 1 Corinthians, 2 Corinthians, Galatians, Ephesians
        4, 4, 5, 3,          // Philippians, Colossians, 1 Thessalonians, 2 Thessalonians
        6, 4, 3, 1, 13,      // 1 Timothy, 2 Timothy, Titus, Philemon, Hebrews
        5, 5, 3, 5, 1, 1,    // James, 1 Peter, 2 Peter, 1 John, 2 John, 3 John
        1, 22                 // Jude, Revelation
    };

    // Static book to chapter count map for quick lookup
    private static final Map<String, Integer> STANDARD_CHAPTERS = new LinkedHashMap<>();
    static {
        for (int i = 0; i < BOOK_ORDER.length; i++) {
            STANDARD_CHAPTERS.put(BOOK_ORDER[i], CHAPTER_COUNTS[i]);
        }
    }

    private BibleService() {}

    public static synchronized BibleService getInstance() {
        if (instance == null) {
            instance = new BibleService();
        }
        return instance;
    }

    /**
     * Initialize the service with the path to Bible JSON files
     */
    public void initialize(String path) {
        this.biblesPath = path;
        System.out.println("[BibleService] Initialized with path: " + path);
    }

    /**
     * Load all available translations from the bibles directory
     */
    public void loadAllTranslations() {
        if (biblesPath == null) {
            System.err.println("[BibleService] ERROR: biblesPath not set. Call initialize() first.");
            return;
        }

        File dir = new File(biblesPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("[BibleService] ERROR: Bibles directory not found: " + biblesPath);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.err.println("[BibleService] WARNING: No JSON files found in " + biblesPath);
            return;
        }

        System.out.println("[BibleService] Loading " + files.length + " translation files...");

        for (File file : files) {
            String code = file.getName().replace(".json", "");
            try {
                loadTranslation(code, file);
            } catch (Exception e) {
                System.err.println("[BibleService] ERROR loading " + code + ": " + e.getMessage());
            }
        }

        loaded = true;
        System.out.println("[BibleService] Loaded " + translations.size() + " translations");
    }

    /**
     * Load a single translation from file
     */
    private void loadTranslation(String code, File file) throws IOException {
        System.out.println("[BibleService] Loading " + code + "...");
        long start = System.currentTimeMillis();

        String content = new String(Files.readAllBytes(file.toPath()), "UTF-8");

        BibleTranslation translation = new BibleTranslation(code);

        // Parse metadata
        TranslationMetadata meta = parseMetadata(content, code);
        metadata.put(code, meta);
        translation.setMetadata(meta);

        // Parse verses - simple parsing for flat array structure
        int versesStart = content.indexOf("\"verses\":[");
        if (versesStart == -1) {
            throw new IOException("No verses array found in " + code);
        }

        // Parse each verse object
        int pos = versesStart + 10;
        while (pos < content.length()) {
            int objStart = content.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = content.indexOf("}", objStart);
            if (objEnd == -1) break;

            String verseJson = content.substring(objStart, objEnd + 1);

            String bookName = extractJsonString(verseJson, "book_name");
            int chapter = extractJsonInt(verseJson, "chapter");
            int verse = extractJsonInt(verseJson, "verse");
            String text = extractJsonString(verseJson, "text");

            if (bookName != null && chapter > 0 && verse > 0 && text != null) {
                translation.addVerse(bookName, chapter, verse, text);
            }

            pos = objEnd + 1;
        }

        translations.put(code, translation);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[BibleService] Loaded " + code + " (" + translation.getVerseCount() + " verses) in " + elapsed + "ms");
    }

    private TranslationMetadata parseMetadata(String content, String code) {
        int metaStart = content.indexOf("\"metadata\":{");
        if (metaStart == -1) {
            return new TranslationMetadata(code, code.toUpperCase(), code, "Unknown");
        }

        int metaEnd = content.indexOf("}", metaStart);
        String metaJson = content.substring(metaStart, metaEnd + 1);

        String name = extractJsonString(metaJson, "name");
        String shortname = extractJsonString(metaJson, "shortname");
        String year = extractJsonString(metaJson, "year");

        return new TranslationMetadata(
            code,
            name != null ? name : code.toUpperCase(),
            shortname != null ? shortname : code.toUpperCase(),
            year != null ? year : "Unknown"
        );
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;

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
            // Unescape common escape sequences
            value = value.replace("\\\"", "\"")
                        .replace("\\/", "/")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
            return value;
        }
        return null;
    }

    private int extractJsonInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return -1;

        start += pattern.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }

        if (end > start) {
            try {
                return Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    // ========================================================================
    // Public API Methods
    // ========================================================================

    /**
     * Get list of available translations
     */
    public List<TranslationMetadata> getTranslations() {
        return new ArrayList<>(metadata.values());
    }

    /**
     * Get list of books for a translation
     */
    public List<String> getBooks(String translationCode) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t == null) return Arrays.asList(BOOK_ORDER);
        return t.getBooks();
    }

    /**
     * Get chapter count for a book
     */
    public int getChapterCount(String translationCode, String book) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t != null) {
            int count = t.getChapterCount(book);
            if (count > 0) return count;
        }
        // Fallback to standard chapter counts
        Integer standard = STANDARD_CHAPTERS.get(book);
        return standard != null ? standard : 0;
    }

    /**
     * Get standard chapter count for a book (static data)
     */
    public static int getStandardChapterCount(String book) {
        Integer count = STANDARD_CHAPTERS.get(book);
        return count != null ? count : 0;
    }

    /**
     * Get a single verse
     */
    public Verse getVerse(String translationCode, String book, int chapter, int verse) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t == null) return null;
        return t.getVerse(book, chapter, verse);
    }

    /**
     * Get all verses in a chapter
     */
    public List<Verse> getChapter(String translationCode, String book, int chapter) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t == null) return Collections.emptyList();
        return t.getChapter(book, chapter);
    }

    /**
     * Get a range of verses
     */
    public List<Verse> getVerseRange(String translationCode, String book, int chapter, int startVerse, int endVerse) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t == null) return Collections.emptyList();
        return t.getVerseRange(book, chapter, startVerse, endVerse);
    }

    /**
     * Search for text across a translation
     */
    public List<Verse> search(String translationCode, String query, int limit) {
        BibleTranslation t = translations.get(translationCode.toLowerCase());
        if (t == null) return Collections.emptyList();
        return t.search(query, limit);
    }

    public boolean isLoaded() {
        return loaded;
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    public static class TranslationMetadata {
        public final String code;
        public final String name;
        public final String shortName;
        public final String year;

        public TranslationMetadata(String code, String name, String shortName, String year) {
            this.code = code;
            this.name = name;
            this.shortName = shortName;
            this.year = year;
        }
    }

    public static class Verse {
        public final String book;
        public final int chapter;
        public final int verse;
        public final String text;

        public Verse(String book, int chapter, int verse, String text) {
            this.book = book;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
        }

        public String getReference() {
            return book + " " + chapter + ":" + verse;
        }
    }

    private static class BibleTranslation {
        private final String code;
        private TranslationMetadata metadata;
        // Map: book -> chapter -> verse -> text
        private final Map<String, Map<Integer, Map<Integer, String>>> data = new LinkedHashMap<>();
        private int verseCount = 0;

        public BibleTranslation(String code) {
            this.code = code;
        }

        public void setMetadata(TranslationMetadata meta) {
            this.metadata = meta;
        }

        public void addVerse(String book, int chapter, int verse, String text) {
            data.computeIfAbsent(book, k -> new TreeMap<>())
                .computeIfAbsent(chapter, k -> new TreeMap<>())
                .put(verse, text);
            verseCount++;
        }

        public int getVerseCount() {
            return verseCount;
        }

        public List<String> getBooks() {
            // Return in standard order, filtering to what's available
            List<String> result = new ArrayList<>();
            for (String book : BOOK_ORDER) {
                if (data.containsKey(book)) {
                    result.add(book);
                }
            }
            return result;
        }

        public int getChapterCount(String book) {
            Map<Integer, Map<Integer, String>> chapters = data.get(book);
            if (chapters == null) return 0;
            return chapters.size();
        }

        public Verse getVerse(String book, int chapter, int verse) {
            Map<Integer, Map<Integer, String>> chapters = data.get(book);
            if (chapters == null) return null;

            Map<Integer, String> verses = chapters.get(chapter);
            if (verses == null) return null;

            String text = verses.get(verse);
            if (text == null) return null;

            return new Verse(book, chapter, verse, text);
        }

        public List<Verse> getChapter(String book, int chapter) {
            List<Verse> result = new ArrayList<>();

            Map<Integer, Map<Integer, String>> chapters = data.get(book);
            if (chapters == null) return result;

            Map<Integer, String> verses = chapters.get(chapter);
            if (verses == null) return result;

            for (Map.Entry<Integer, String> entry : verses.entrySet()) {
                result.add(new Verse(book, chapter, entry.getKey(), entry.getValue()));
            }

            return result;
        }

        public List<Verse> getVerseRange(String book, int chapter, int startVerse, int endVerse) {
            List<Verse> result = new ArrayList<>();

            Map<Integer, Map<Integer, String>> chapters = data.get(book);
            if (chapters == null) return result;

            Map<Integer, String> verses = chapters.get(chapter);
            if (verses == null) return result;

            for (int v = startVerse; v <= endVerse; v++) {
                String text = verses.get(v);
                if (text != null) {
                    result.add(new Verse(book, chapter, v, text));
                }
            }

            return result;
        }

        public List<Verse> search(String query, int limit) {
            List<Verse> result = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            outer:
            for (Map.Entry<String, Map<Integer, Map<Integer, String>>> bookEntry : data.entrySet()) {
                String book = bookEntry.getKey();
                for (Map.Entry<Integer, Map<Integer, String>> chapterEntry : bookEntry.getValue().entrySet()) {
                    int chapter = chapterEntry.getKey();
                    for (Map.Entry<Integer, String> verseEntry : chapterEntry.getValue().entrySet()) {
                        int verse = verseEntry.getKey();
                        String text = verseEntry.getValue();

                        if (text.toLowerCase().contains(lowerQuery)) {
                            result.add(new Verse(book, chapter, verse, text));
                            if (result.size() >= limit) break outer;
                        }
                    }
                }
            }

            return result;
        }
    }
}
