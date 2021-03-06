/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.application;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import eu.scape_project.planning.model.Role;
import eu.scape_project.planning.model.User;
import eu.scape_project.planning.model.UserGroup;

/**
 * Factory class responsible for producing/injecting session-scoped objects.
 * 
 * @author Michael Kraxner, Markus Hamm
 */
@SessionScoped
@Stateful
public class SessionScopeProducer implements Serializable, IAuthenticatedUserProvider {
    private static final long serialVersionUID = -830549797293803656L;

    @Inject
    private Logger log;

    private User user;

    @PersistenceContext
    private EntityManager em;

    public SessionScopeProducer() {
        user = null;
    }

    /* (non-Javadoc)
     * @see eu.scape_project.planning.application.IAuthenticatedUserProvider#getUser()
     */
    @Override
    @Produces
    @Named("user")
    public User getUser() {
        // TODO: Replace this by correct code after login-functionality exists.

        if (user == null) {
            user = getUserFromSession();
        }

        if (user == null) {
            user = getUserAdminFromDB();
        }

        return user;
    }

    private User getUserFromSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // Get userprincipal
        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            return null;
        }

        // Read user from DB
        User user = getUserFromDB(principal.getName());
        // Create new user object
        if (user == null) {
            user = createUser(principal.getName());
        }

        // Get attributes
        HttpSession session = request.getSession();
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> attributes = (Map<String, List<Object>>) session
            .getAttribute("SESSION_ATTRIBUTE_MAP");

        String email = null;
        String firstName = null;
        String lastName = null;

        if (attributes != null) {
            // Set transient data from attributes
            List<Object> firstNameList = attributes.get("firstName");
            if (firstNameList != null) {
                if (firstNameList.size() > 0) {
                    firstName = (String) firstNameList.get(0);
                }
            }

            List<Object> lastNameList = attributes.get("lastName");
            if (lastNameList != null) {
                if (lastNameList.size() > 0) {
                    lastName = (String) lastNameList.get(0);
                }
            }

            List<Object> emailList = attributes.get("email");
            if (emailList != null) {
                if (emailList.size() > 0) {
                    email = (String) emailList.get(0);
                }
            }
        }

        ArrayList<Role> roles = new ArrayList<Role>();
        if (request.isUserInRole("authenticated")) {
            Role role = new Role();
            role.setName("authenticated");
            roles.add(role);

        }
        if (request.isUserInRole("admin")) {
            Role role = new Role();
            role.setName("admin");
            roles.add(role);

        }
        user.setRoles(roles);

        boolean update = false;
        if (email != null && !email.equals("") && !email.equals(user.getEmail())) {
            user.setEmail(email);
            update = true;
        }
        if (firstName != null && !firstName.equals("") && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            update = true;
        }
        if (lastName != null && !lastName.equals("") && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            update = true;
        }

        if (update) {
            em.merge(user);
            log.debug("Updating email address of user " + user.getUsername());
        }

        // try {
        // Subject caller = (Subject) PolicyContext
        // .getContext("javax.security.auth.Subject.container");
        //
        // Set<Principal> principals = caller.getPrincipals();
        // for (Principal p : principals) {
        // String result = p.getName();
        // }
        //
        // CallbackHandler cbh = (CallbackHandler) PolicyContext
        // .getContext("org.jboss.security.auth.spi.CallbackHandler");
        //
        // } catch (PolicyContextException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        return user;
    }

//    /**
//     * Reads the current logged in user from the ServletRequest and fetches the
//     * corresponding plato specific data.
//     * 
//     * @return The current user
//     */
//    private User getUserByServletRequest() {
//        // Get user principal
//        FacesContext context = FacesContext.getCurrentInstance();
//        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
//        Principal principal = request.getUserPrincipal();
//
//        if (principal == null) {
//            return null;
//        }
//
//        // Get user from DB
//        try {
//            User user = em.createQuery("SELECT u From User u WHERE u.username = :username", User.class)
//                .setParameter("username", principal.getName()).getSingleResult();
//
//            return user;
//        } catch (NoResultException e) {
//            return null;
//        }
//    }

    private User getUserFromDB(String username) {
        // Get user from DB
        try {
            User user = em.createQuery("SELECT u From User u WHERE u.username = :username", User.class)
                .setParameter("username", username).getSingleResult();

            return user;
        } catch (NoResultException e) {
            return null;
        }
    }

    private User getUserAdminFromDB() {
        Object dbResult;
        try {
            dbResult = em.createQuery("SELECT u From User u WHERE u.username = 'admin'").getSingleResult();
            return (User) dbResult;
        } catch (NoResultException e1) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        UserGroup userGroup = new UserGroup();
        userGroup.setName(username);
        user.setUserGroup(userGroup);
        em.persist(userGroup);
        em.persist(user);
        return user;
    }
}
