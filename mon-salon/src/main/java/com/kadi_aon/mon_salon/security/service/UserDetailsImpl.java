package com.kadi_aon.mon_salon.security.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kadi_aon.mon_salon.account.entity.Compte;

import lombok.Getter;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final String nom;
    private final String prenom;
    private final String password;
    private final Boolean statut;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Compte compte;

    public UserDetailsImpl(Compte compte) {
        this.compte = compte;
        this.id = compte.getId();
        this.email = compte.getEmail();
        this.nom = compte.getNom();
        this.prenom = compte.getPrenom();
        this.password = compte.getPassword();
        this.statut = compte.getStatut() != null && compte.getStatut();

        if (compte.getRolePlateforme() != null) {
            this.authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + compte.getRolePlateforme().getRole().name())
            );
        } else {
            this.authorities = Collections.emptyList();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.statut);
    }
}
