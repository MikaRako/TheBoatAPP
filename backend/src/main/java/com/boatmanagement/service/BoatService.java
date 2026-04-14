package com.boatmanagement.service;

import com.boatmanagement.audit.Auditable;
import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.Boat;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import com.boatmanagement.exception.BoatNotFoundException;
import com.boatmanagement.mapper.BoatMapper;
import com.boatmanagement.repository.BoatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoatService {

    private final BoatRepository boatRepository;
    private final BoatMapper boatMapper;

    // BOAT_LIST: no resource id — the aspect skips id extraction for list
    // responses.
    @Auditable(action = AuditAction.BOAT_LIST, resourceType = "BOAT")
    public BoatDto.PageResponse findAll(@Nullable String search, @Nullable BoatStatus status, @Nullable BoatType type,
            int page, int size, String sortBy, String sortDir) {
        // Normalise optional inputs so the repository and logs never see null strings.
        // Escape LIKE wildcard characters so user-supplied % and _ are treated as literals.
        String effectiveSearch = escapeLike((search != null) ? search.trim() : "");
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "createdAt";
        String effectiveSortDir = (sortDir != null && !sortDir.isBlank()) ? sortDir.trim() : "desc";

        log.debug("Finding all boats - search: '{}', status: {}, type: {}, page: {}, size: {} by '{}'",
                effectiveSearch, status, type, page, size, MDC.get("user"));

        Sort sort = effectiveSortDir.equalsIgnoreCase("desc")
                ? Sort.by(effectiveSortBy).descending()
                : Sort.by(effectiveSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Boat> boatPage = boatRepository.findByFilters(effectiveSearch, status, type, pageable);

        return BoatDto.PageResponse.builder()
                .content(boatPage.getContent().stream().map(boatMapper::toResponse).toList())
                .page(boatPage.getNumber())
                .size(boatPage.getSize())
                .totalElements(boatPage.getTotalElements())
                .totalPages(boatPage.getTotalPages())
                .last(boatPage.isLast())
                .build();
    }

    /**
     * Escapes SQL LIKE wildcard characters so user-supplied % and _ are matched
     * literally. The repository query uses ESCAPE '\' to honour these escapes.
     */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // BOAT_READ: id is arg[0]; the aspect reads it directly from the parameter.
    @Auditable(action = AuditAction.BOAT_READ, resourceType = "BOAT", resourceIdArgIndex = 0)
    public BoatDto.Response findById(@NonNull Long id) {
        log.debug("Finding boat by id: {} by '{}'", id, MDC.get("user"));
        return boatRepository.findById(id)
                .map(boatMapper::toResponse)
                .orElseThrow(() -> new BoatNotFoundException(id));
    }

    // BOAT_CREATE: no id arg (id is assigned by DB); the aspect reads it from the
    // returned DTO via getId().
    @Auditable(action = AuditAction.BOAT_CREATE, resourceType = "BOAT")
    @Transactional
    public BoatDto.Response create(BoatDto.Request request) {
        log.debug("Creating new boat: '{}' by '{}'", request.getName(), MDC.get("user"));
        Boat boat = boatMapper.toEntity(request);
        Boat saved = boatRepository.save(boat);
        log.info("Boat created with id: {} by '{}'", saved.getId(), MDC.get("user"));
        return boatMapper.toResponse(saved);
    }

    // BOAT_UPDATE: id is arg[0].
    @Auditable(action = AuditAction.BOAT_UPDATE, resourceType = "BOAT", resourceIdArgIndex = 0)
    @Transactional
    public BoatDto.Response update(@NonNull Long id, BoatDto.Request request) {
        log.debug("Updating boat with id: {} by '{}'", id, MDC.get("user"));
        Boat boat = boatRepository.findById(id)
                .orElseThrow(() -> new BoatNotFoundException(id));
        boatMapper.updateEntity(boat, request);
        Boat saved = boatRepository.save(boat);
        log.info("Boat updated with id: {} by '{}'", saved.getId(), MDC.get("user"));
        return boatMapper.toResponse(saved);
    }

    // BOAT_DELETE: id is arg[0].
    @Auditable(action = AuditAction.BOAT_DELETE, resourceType = "BOAT", resourceIdArgIndex = 0)
    @Transactional
    public void delete(@NonNull Long id) {
        log.debug("Deleting boat with id: {} by '{}'", id, MDC.get("user"));
        if (!boatRepository.existsById(id)) {
            throw new BoatNotFoundException(id);
        }
        boatRepository.deleteById(id);
        log.info("Boat deleted with id: {} by '{}'", id, MDC.get("user"));
    }
}
