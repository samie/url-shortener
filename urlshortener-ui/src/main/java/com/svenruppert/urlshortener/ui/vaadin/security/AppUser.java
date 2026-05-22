package com.svenruppert.urlshortener.ui.vaadin.security;



import java.util.Set;

public record AppUser(String name, Set<AppRole> roles) {
}