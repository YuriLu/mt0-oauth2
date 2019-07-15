package com.hw.service;

import com.hw.clazz.GrantedAuthorityImpl;
import com.hw.clazz.eenum.ResourceOwnerAuthorityEnum;
import com.hw.entity.ResourceOwner;
import com.hw.interfaze.TokenRevocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashSet;

@Component
public class ResourceOwnerTokenRevocationService implements TokenRevocationService<ResourceOwner> {

    @Autowired
    private OAuth2RestTemplate restTemplate;

    @Value("${url.zuul.resourceOwner}")
    private String url;

    @Value("${feature.token.revocation}")
    private Boolean enabled;

    /**
     * aspects: authority, lock
     * unlock a user should not revoke
     *
     * @param oldResourceOwner
     * @param newResourceOwner
     * @return
     */
    @Override
    public boolean shouldRevoke(ResourceOwner oldResourceOwner, ResourceOwner newResourceOwner) {
        if (!enabled) {
            return false;
        } else if (authorityChanged(oldResourceOwner, newResourceOwner)) {
            return true;
        } else if (lockUser(oldResourceOwner, newResourceOwner)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean lockUser(ResourceOwner oldResourceOwner, ResourceOwner newResourceOwner) {
        if (Boolean.TRUE.equals(newResourceOwner.getLocked())) {
            return true;
        } else {
            return false;
        }
    }

    private boolean authorityChanged(ResourceOwner oldResourceOwner, ResourceOwner newResourceOwner) {
        HashSet<GrantedAuthorityImpl<ResourceOwnerAuthorityEnum>> grantedAuthorities = new HashSet<>(oldResourceOwner.getGrantedAuthorities());
        HashSet<GrantedAuthorityImpl<ResourceOwnerAuthorityEnum>> grantedAuthorities1 = new HashSet<>(newResourceOwner.getGrantedAuthorities());
        return !grantedAuthorities.equals(grantedAuthorities1);
    }

    @Override
    public void blacklist(String name, boolean shouldRevoke) {
        if (shouldRevoke && enabled) {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("name", name);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> hashMapHttpEntity = new HttpEntity<>(map, headers);
            restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        }
    }


}
