package com.kadi_aon.mon_salon.salon.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.mapper.SalonDTOResponseMapper;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreSalonService {

    private final SalonRepository salonRepository;
    private final SalonDTOResponseMapper salonDTOResponseMapper;

    @Transactional(readOnly = true)
    public Page<SalonDTOResponse> listerSalonsActifs(Pageable pageable) {
        return salonRepository.findByStatutTrue(pageable)
                .map(salonDTOResponseMapper::mapPublic);
    }

    @Transactional(readOnly = true)
    public List<SalonDTOResponse> rechercherSalons(String query) {
        if (query == null || query.isBlank()) {
            return salonRepository.findByStatutTrueOrderByNomAsc().stream()
                    .map(salonDTOResponseMapper::mapPublic)
                    .toList();
        }
        return salonRepository.searchSalons(query.trim()).stream()
                .map(salonDTOResponseMapper::mapPublic)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalonDTOResponse> rechercherSalonsNearby(double lat, double lng, double rayonKm) {
        List<Salon> activeSalons = salonRepository.findByStatutTrueOrderByNomAsc();

        return activeSalons.stream()
                .filter(salon -> salon.getLatitude() != null && salon.getLongitude() != null)
                .filter(salon -> calculerDistanceKm(lat, lng, salon.getLatitude(), salon.getLongitude()) <= rayonKm)
                .map(salonDTOResponseMapper::mapPublic)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalonDTOResponse> rechercherSalonsParService(Long serviceId) {
        return salonRepository.findSalonByServiceId(serviceId)
                .map(s -> List.of(salonDTOResponseMapper.mapPublic(s)))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public SalonDTOResponse getSalonDetail(String slugSalon) {
        Salon salon = salonRepository.findBySlugAndStatutTrue(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon actif introuvable avec le slug : " + slugSalon));
        return salonDTOResponseMapper.mapPublic(salon);
    }

    private double calculerDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en kilomètres
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
