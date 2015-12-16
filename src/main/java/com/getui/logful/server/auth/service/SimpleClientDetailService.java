package com.getui.logful.server.auth.service;

import com.getui.logful.server.auth.model.SimpleClientDetails;
import com.getui.logful.server.auth.repository.SimpleClientDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class SimpleClientDetailService implements ClientDetailsService, ClientRegistrationService {

    @Autowired
    SimpleClientDetailsRepository simpleClientDetailsRepository;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        SimpleClientDetails simpleClientDetails = simpleClientDetailsRepository.findByClientId(clientId);
        if (simpleClientDetails == null) {
            throw new ClientRegistrationException("Client: " + clientId + " not found!");
        }
        return getClientFromSimpleClientDetail(simpleClientDetails);
    }

    @Override
    public void addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException {
        SimpleClientDetails simpleClientDetails = new SimpleClientDetails(clientDetails);
        simpleClientDetailsRepository.save(simpleClientDetails);
    }

    @Override
    public void updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException {
        SimpleClientDetails simpleClientDetails = simpleClientDetailsRepository.findByClientId(clientDetails.getClientId());
        if (simpleClientDetails == null) {
            throw new NoSuchClientException("Client not found with ID: " + clientDetails.getClientId());
        }
        simpleClientDetails = new SimpleClientDetails(clientDetails);
        simpleClientDetailsRepository.save(simpleClientDetails);
    }

    @Override
    public void updateClientSecret(String clientId, String secret) throws NoSuchClientException {
        SimpleClientDetails simpleClientDetails = simpleClientDetailsRepository.findByClientId(clientId);
        if (simpleClientDetails == null) {
            throw new NoSuchClientException("Client not found with ID: " + clientId);
        }
        // TODO
        simpleClientDetails.setClientSecret(secret);
        simpleClientDetailsRepository.save(simpleClientDetails);
    }

    @Override
    public void removeClientDetails(String clientId) throws NoSuchClientException {
        SimpleClientDetails simpleClientDetails = simpleClientDetailsRepository.findByClientId(clientId);
        if (simpleClientDetails == null) {
            throw new NoSuchClientException("Client not found with ID: " + clientId);
        }
        simpleClientDetailsRepository.delete(simpleClientDetails);
    }

    @Override
    public List<ClientDetails> listClientDetails() {
        List<SimpleClientDetails> list = simpleClientDetailsRepository.findAll();
        List<ClientDetails> temp = new LinkedList<>();
        if (list != null && !list.isEmpty()) {
            for (SimpleClientDetails item : list) {
                temp.add(getClientFromSimpleClientDetail(item));
            }
        }
        return temp;
    }

    private BaseClientDetails getClientFromSimpleClientDetail(SimpleClientDetails simpleClientDetails) {
        BaseClientDetails baseClientDetails = new BaseClientDetails();
        baseClientDetails.setAccessTokenValiditySeconds(simpleClientDetails.getAccessTokenValiditySeconds());
        baseClientDetails.setAuthorizedGrantTypes(simpleClientDetails.getAuthorizedGrantTypes());
        baseClientDetails.setClientId(simpleClientDetails.getClientId());
        baseClientDetails.setClientSecret(simpleClientDetails.getClientSecret());
        baseClientDetails.setRefreshTokenValiditySeconds(simpleClientDetails.getRefreshTokenValiditySeconds());
        baseClientDetails.setRegisteredRedirectUri(simpleClientDetails.getRegisteredRedirectUri());
        baseClientDetails.setResourceIds(simpleClientDetails.getResourceIds());
        baseClientDetails.setScope(simpleClientDetails.getScope());
        return baseClientDetails;
    }
}
