package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Admin.Dto.*;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Enums.Permission;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Fasllar va qismlar.
 *
 * Kontent ichida joylashgani uchun manzil ham ichma-ich:
 * {@code /content/{contentId}/seasons} va {@code /content/{contentId}/episodes}.
 * Bu qaysi qism qaysi kontentga tegishli ekanini manzilning o'zida ko'rsatadi
 * va servis har safar tekshiradi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/content/{contentId}")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;
    private final EpisodeRepo episodeRepo;
    private final PermissionService permissionService;

    private void require(Permission permission) {
        if (!permissionService.hasPermission(CurrentUser.get(), permission)) {
            throw BusinessException.accessDenied("Ruxsat yo'q: " + permission);
        }
    }

    /** Nashr qilish alohida ruxsat — yaratish huquqi uni o'z ichiga olmaydi. */
    private void requirePublishRights(PublicationStatus status) {
        if (status != null && status.isVisibleToUsers()) {
            require(Permission.CONTENT_PUBLISH);
        }
    }

    // ---------------------------------------------------------------- fasllar

    @GetMapping("/seasons")
    public ResponseEntity<List<SeasonDto>> seasons(@PathVariable Long contentId) {
        require(Permission.CONTENT_VIEW);
        List<SeasonDto> result = episodeService.seasonsOf(contentId).stream()
                .map(s -> SeasonDto.from(s,
                        episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(s.getId()).size()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seasons")
    @RequirePermission(Permission.CONTENT_CREATE)
    public ResponseEntity<SeasonDto> createSeason(@PathVariable Long contentId,
                                                  @Valid @RequestBody SeasonSaveRequest request) {
        require(Permission.CONTENT_CREATE);
        requirePublishRights(request.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SeasonDto.from(episodeService.saveSeason(CurrentUser.get(), contentId, null, request), 0));
    }

    @PutMapping("/seasons/{seasonId}")
    @RequirePermission(Permission.CONTENT_EDIT)
    public ResponseEntity<SeasonDto> updateSeason(@PathVariable Long contentId,
                                                  @PathVariable Long seasonId,
                                                  @Valid @RequestBody SeasonSaveRequest request) {
        require(Permission.CONTENT_EDIT);
        requirePublishRights(request.getStatus());
        var saved = episodeService.saveSeason(CurrentUser.get(), contentId, seasonId, request);
        return ResponseEntity.ok(SeasonDto.from(saved,
                episodeRepo.findAllBySeasonIdOrderByEpisodeNumberAsc(seasonId).size()));
    }

    @DeleteMapping("/seasons/{seasonId}")
    public ResponseEntity<Void> deleteSeason(@PathVariable Long contentId, @PathVariable Long seasonId) {
        require(Permission.CONTENT_DELETE);
        episodeService.deleteSeason(CurrentUser.get(), contentId, seasonId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- qismlar

    @GetMapping("/episodes")
    public ResponseEntity<List<EpisodeDto>> episodes(@PathVariable Long contentId) {
        require(Permission.CONTENT_VIEW);
        List<Episode> episodes = episodeService.episodesOf(contentId);
        return ResponseEntity.ok(episodes.stream().map(EpisodeDto::from).toList());
    }

    @PostMapping("/episodes")
    @RequirePermission(Permission.CONTENT_CREATE)
    public ResponseEntity<EpisodeDto> createEpisode(@PathVariable Long contentId,
                                                    @Valid @RequestBody EpisodeSaveRequest request) {
        require(Permission.CONTENT_CREATE);
        requirePublishRights(request.getStatus());
        Episode saved = episodeService.saveEpisode(CurrentUser.get(), contentId, null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EpisodeDto.from(saved));
    }

    @PutMapping("/episodes/{episodeId}")
    @RequirePermission(Permission.CONTENT_EDIT)
    public ResponseEntity<EpisodeDto> updateEpisode(@PathVariable Long contentId,
                                                    @PathVariable Long episodeId,
                                                    @Valid @RequestBody EpisodeSaveRequest request) {
        require(Permission.CONTENT_EDIT);
        requirePublishRights(request.getStatus());
        Episode saved = episodeService.saveEpisode(CurrentUser.get(), contentId, episodeId, request);
        return ResponseEntity.ok(EpisodeDto.from(saved));
    }

    @DeleteMapping("/episodes/{episodeId}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable Long contentId, @PathVariable Long episodeId) {
        require(Permission.CONTENT_DELETE);
        episodeService.deleteEpisode(CurrentUser.get(), contentId, episodeId);
        return ResponseEntity.noContent().build();
    }
}
