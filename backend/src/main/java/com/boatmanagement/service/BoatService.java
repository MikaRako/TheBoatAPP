package com.boatmanagement.service;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.Boat;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoatService {

    private final BoatRepository boatRepository;
    private final BoatMapper boatMapper;

    public BoatDto.PageResponse findAll(String search, int page, int size, String sortBy, String sortDir) {
        log.debug("Finding all boats - search: '{}', page: {}, size: {} by '{}'", search, page, size, MDC.get("user"));

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Boat> boatPage = boatRepository.findBySearchTerm(search, pageable);

        return BoatDto.PageResponse.builder()
                .content(boatPage.getContent().stream().map(boatMapper::toResponse).toList())
                .page(boatPage.getNumber())
                .size(boatPage.getSize())
                .totalElements(boatPage.getTotalElements())
                .totalPages(boatPage.getTotalPages())
                .last(boatPage.isLast())
                .build();
    }

    public BoatDto.Response findById(Long id) {
        log.debug("Finding boat by id: {} by '{}'", id, MDC.get("user"));
        return boatRepository.findById(id)
                .map(boatMapper::toResponse)
                .orElseThrow(() -> new BoatNotFoundException(id));
    }

    @Transactional
    public BoatDto.Response create(BoatDto.Request request) {
        log.debug("Creating new boat: '{}' by '{}'", request.getName(), MDC.get("user"));
        Boat boat = boatMapper.toEntity(request);
        Boat saved = boatRepository.save(boat);
        log.info("Boat created with id: {} by '{}'", saved.getId(), MDC.get("user"));
        return boatMapper.toResponse(saved);
    }

    @Transactional
    public BoatDto.Response update(Long id, BoatDto.Request request) {
        log.debug("Updating boat with id: {} by '{}'", id, MDC.get("user"));
        Boat boat = boatRepository.findById(id)
                .orElseThrow(() -> new BoatNotFoundException(id));
        boatMapper.updateEntity(boat, request);
        Boat saved = boatRepository.save(boat);
        log.info("Boat updated with id: {} by '{}'", saved.getId(), MDC.get("user"));
        return boatMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting boat with id: {} by '{}'", id, MDC.get("user"));
        if (!boatRepository.existsById(id)) {
            throw new BoatNotFoundException(id);
        }
        boatRepository.deleteById(id);
        log.info("Boat deleted with id: {} by '{}'", id, MDC.get("user"));
    }
}
