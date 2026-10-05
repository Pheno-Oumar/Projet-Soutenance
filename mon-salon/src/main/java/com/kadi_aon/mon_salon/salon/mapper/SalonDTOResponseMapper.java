package com.kadi_aon.mon_salon.salon.mapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.stock.repository.CommandeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SalonDTOResponseMapper implements Function<Salon, SalonDTOResponse> {

    private final AffectationSalonRepository affectationSalonRepository;
    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;

    @Override
    public SalonDTOResponse apply(Salon salon) {
        if (salon == null) {
            return null;
        }

        String emailProprietaire = null;
        Long nombreClients = 0L;
        Long nombreEmployes = 0L;
        BigDecimal chiffreAffaires = BigDecimal.ZERO;
        Long nombreCommandes = 0L;

        if (salon.getId() != null) {
            Optional<AffectationSalon> affectationProprio = affectationSalonRepository
                    .findBySalonIdAndStatutTrue(salon.getId())
                    .stream()
                    .filter(a -> a.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.PROPRIETAIRE))
                    .findFirst();

            if (affectationProprio.isPresent() && affectationProprio.get().getCompte() != null) {
                emailProprietaire = affectationProprio.get().getCompte().getEmail();
            }
        }

        if (salon.getSlug() != null) {
            nombreClients = affectationSalonRepository.countBySalonSlugAndRole(salon.getSlug(), TypeRoleSalon.CLIENT);
            nombreEmployes = affectationSalonRepository.countEmployesBySalonSlug(salon.getSlug());
            BigDecimal ca = paiementRepository.totalPaiementsSalon(salon.getSlug());
            if (ca != null) {
                chiffreAffaires = ca;
            }
            nombreCommandes = commandeRepository.countByClientAffectationSalonSlug(salon.getSlug());
        }

        return new SalonDTOResponse(
                salon.getId(),
                salon.getNom(),
                salon.getSlug(),
                salon.getLogoUrl(),
                salon.getDescription(),
                salon.getAdresse(),
                salon.getTelephone(),
                salon.getEmail(),
                salon.getLatitude(),
                salon.getLongitude(),
                salon.getStatut(),
                salon.getDateCreation(),
                emailProprietaire,
                nombreClients,
                nombreEmployes,
                chiffreAffaires,
                nombreCommandes
        );
    }

    public SalonDTOResponse mapPublic(Salon salon) {
        if (salon == null) {
            return null;
        }
        return new SalonDTOResponse(
                salon.getId(),
                salon.getNom(),
                salon.getSlug(),
                salon.getLogoUrl(),
                salon.getDescription(),
                salon.getAdresse(),
                salon.getTelephone(),
                salon.getEmail(),
                salon.getLatitude(),
                salon.getLongitude(),
                salon.getStatut(),
                salon.getDateCreation(),
                null
        );
    }
}

