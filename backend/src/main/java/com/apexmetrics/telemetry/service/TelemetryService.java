package com.apexmetrics.telemetry.service;

import com.apexmetrics.auth.entity.User;
import com.apexmetrics.auth.repository.UserRepository;
import com.apexmetrics.shared.exception.CsvInvalidSchemaException;
import com.apexmetrics.telemetry.dto.SessionSummaryDTO;
import com.apexmetrics.telemetry.entity.Category;
import com.apexmetrics.telemetry.entity.TelemetryPoint;
import com.apexmetrics.telemetry.entity.TelemetrySession;
import com.apexmetrics.telemetry.entity.Track;
import com.apexmetrics.telemetry.parser.CsvParser;
import com.apexmetrics.telemetry.repository.CategoryRepository;
import com.apexmetrics.telemetry.repository.TelemetryPointRepository;
import com.apexmetrics.telemetry.repository.TelemetrySessionRepository;
import com.apexmetrics.telemetry.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelemetryService implements ITelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final int MAX_POINTS = 10_000;

    private final List<CsvParser> csvParsers;
    private final TelemetrySessionRepository sessionRepository;
    private final TelemetryPointRepository pointRepository;
    private final TrackRepository trackRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SessionSummaryDTO uploadSession(MultipartFile file, Long trackId, Long categoryId,
                                           String simulatorType, String userEmail) {
        CsvParser parser = csvParsers.stream()
                .filter(p -> p.getSimulatorType().equalsIgnoreCase(simulatorType))
                .findFirst()
                .orElseThrow(() -> new CsvInvalidSchemaException(
                        "Unknown simulator type: " + simulatorType, "simulatorType"));

        List<TelemetryPoint> rawPoints = parser.parse(file);
        List<TelemetryPoint> points = downsample(rawPoints);

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        TelemetrySession session = TelemetrySession.builder()
                .user(user)
                .track(track)
                .category(category)
                .uploadedAt(LocalDateTime.now())
                .bestLapTime(null)
                .build();
        TelemetrySession saved = sessionRepository.save(session);

        points.forEach(p -> p.setSession(saved));
        pointRepository.saveAll(points);

        log.info("TelemetryService.uploadSession: persisted session {} with {} points for user {}",
                saved.getId(), points.size(), userEmail);

        return new SessionSummaryDTO(
                saved.getId(),
                track.getName(),
                category.getName(),
                saved.getUploadedAt(),
                null,
                points.size()
        );
    }

    List<TelemetryPoint> downsample(List<TelemetryPoint> points) {
        if (points.size() <= MAX_POINTS) return points;
        int step = points.size() / MAX_POINTS;
        List<TelemetryPoint> result = new ArrayList<>(MAX_POINTS);
        for (int i = 0; i < points.size() && result.size() < MAX_POINTS; i += step) {
            result.add(points.get(i));
        }
        log.info("TelemetryService.downsample: reduced {} → {} points", points.size(), result.size());
        return result;
    }
}
