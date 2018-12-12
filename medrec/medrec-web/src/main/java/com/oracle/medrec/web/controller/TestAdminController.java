package com.oracle.medrec.web.controller;

import com.oracle.medrec.web.login.Password;
import com.oracle.medrec.web.login.Username;

import javax.enterprise.inject.Model;

import javax.inject.Inject;

import javax.json.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@Model
public class TestAdminController extends BaseMedRecPageController {

    @Username
    @Inject
    private String username;

    @Password
    @Inject
    private String password;

    public TestAdminController() {
        super();
    }

    public String login() {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getFacesContext()
                                                                          .getExternalContext()
                                                                          .getRequest();
        HttpServletResponse response = (HttpServletResponse) getPageContext().getFacesContext()
                                                                             .getExternalContext()
                                                                             .getResponse();
        String message = callMs();
        AuthenticatingAdministratorController aac = new AuthenticatingAdministratorController();
        String path = aac.login(request, response, username, password);
        getPageContext().getSessionMap().put("adminName", username);
        return path;
    }
    
    public String logout() {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getFacesContext()
                                                                          .getExternalContext()
                                                                          .getRequest();
        HttpServletResponse response = (HttpServletResponse) getPageContext().getFacesContext()
                                                                             .getExternalContext()
                                                                             .getResponse();
        
        AuthenticatingAdministratorController aac = new AuthenticatingAdministratorController();
        String path = aac.logout(request, response, username, password);
        getPageContext().invalidateSession();
        return path;
    } 
    
    private String callMs() {
        Client client = ClientBuilder.newClient();
        JsonObject jsonObject = client
                .target("http://localhost:8080/greet")
                .request()
                .get(JsonObject.class);
        return jsonObject.getString("message");
    }
}
