package org.example.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.NetworkMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// Persists analysis session to disk so results can be restored on next launch
public class SessionPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(SessionPersistenceService.class);
    private static final Path DEFAULT_SESSION_DIR =
            Path.of(System.getProperty("user.home"), ".network-analysis-tool");
    private final Path sessionDir;
    private final Path sessionPath;
    private final Path sessionBackup;
    private final ObjectMapper mapper;
    public SessionPersistenceService() {
        this(DEFAULT_SESSION_DIR);
    }
    SessionPersistenceService(Path sessionDir) {
        this.sessionDir = sessionDir;
        this.sessionPath = sessionDir.resolve("session.json");
        this.sessionBackup = sessionDir.resolve("session.json.bak");
        mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }
    public static Path getSessionPath() {
        return DEFAULT_SESSION_DIR.resolve("session.json");
    }
    public boolean save(SessionData data) {
        if (data == null) return false;
        if (data.metrics == null) {
            data.metrics = new ArrayList<>();
        }
        SessionData toWrite = data;
        if (data.metrics.isEmpty()) {
            SessionData existing = readSessionFile(sessionPath);
            if (existing == null && Files.isRegularFile(sessionBackup)) {
                existing = readSessionFile(sessionBackup);
            }
            if (existing != null && existing.metrics != null && !existing.metrics.isEmpty()) {
                mergeUiState(existing, data);
                toWrite = existing;
                logger.debug("Merged UI state into existing session ({} networks preserved)",
                        existing.metrics.size());
            } else {
                logger.debug("Skipping session save: no metrics loaded");
                return false;
            }
        }
        return writeSession(toWrite);
    }
    private static void mergeUiState(SessionData target, SessionData incoming) {
        if (incoming.currentFolder != null) {
            target.currentFolder = incoming.currentFolder;
        }
        if (incoming.lastAnalysisText != null) {
            target.lastAnalysisText = incoming.lastAnalysisText;
        }
        if (incoming.chatHistory != null) {
            target.chatHistory = incoming.chatHistory;
        }
        if (incoming.analysisMode != null) {
            target.analysisMode = incoming.analysisMode;
        }
        target.savedAt = incoming.savedAt > 0 ? incoming.savedAt : System.currentTimeMillis();
    }
    private boolean writeSession(SessionData data) {
        try {
            Files.createDirectories(sessionDir);
            if (Files.isRegularFile(sessionPath)) {
                Files.copy(sessionPath, sessionBackup, StandardCopyOption.REPLACE_EXISTING);
            }
            Path temp = sessionDir.resolve("session.json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), data);
            try {
                Files.move(temp, sessionPath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, sessionPath, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!Files.isRegularFile(sessionPath) || Files.size(sessionPath) < 4) {
                logger.error("Session file missing or empty after save");
                return false;
            }
            logger.info("Session saved ({} networks) - {}", data.metrics.size(), sessionPath);
            return true;
        } catch (IOException e) {
            logger.error("Could not save session: {}", e.getMessage(), e);
            return false;
        }
    }
    public SessionData load() {
        SessionData fromMain = readSessionFile(sessionPath);
        SessionData fromBackup = Files.isRegularFile(sessionBackup) ? readSessionFile(sessionBackup) : null;
        boolean mainHasMetrics = fromMain != null && !fromMain.metrics.isEmpty();
        boolean backupHasMetrics = fromBackup != null && !fromBackup.metrics.isEmpty();
        if (mainHasMetrics) {
            return fromMain;
        }
        if (backupHasMetrics) {
            logger.warn("Primary session has no networks: restoring from backup ({} networks)",
                    fromBackup.metrics.size());
            return fromBackup;
        }
        if (fromMain != null) {
            return fromMain;
        }
        return fromBackup;
    }
    private SessionData readSessionFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            SessionData data = mapper.readValue(path.toFile(), SessionData.class);
            if (data.metrics == null) {
                data.metrics = new ArrayList<>();
            }
            logger.info("Read session from {} ({} networks)", path.getFileName(), data.metrics.size());
            return data;
        } catch (IOException e) {
            logger.error("Could not load session from {}: {}", path, e.getMessage(), e);
            return null;
        }
    }
    public void clear() {
        try {
            Files.deleteIfExists(sessionPath);
            Files.deleteIfExists(sessionBackup);
        } catch (IOException e) {
            logger.warn("Could not delete session file: {}", e.getMessage());
        }
    }
    public static class SessionData {
        public List<NetworkMetrics> metrics = new ArrayList<>();
        public String currentFolder;
        public String lastAnalysisText;
        public String chatHistory;
        public String analysisMode;
        public long savedAt;
    }
}
