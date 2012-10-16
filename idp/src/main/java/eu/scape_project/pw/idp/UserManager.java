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
package eu.scape_project.pw.idp;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;

import eu.scape_project.pw.idp.excpetions.CannotSendMailException;
import eu.scape_project.pw.idp.excpetions.CreateUserException;
import eu.scape_project.pw.idp.excpetions.UserNotFoundExeception;
import eu.scape_project.pw.idp.model.IdpRole;
import eu.scape_project.pw.idp.model.IdpUser;
import eu.scape_project.pw.idp.model.IdpUserState;
import eu.scape_project.pw.idp.utils.PropertiesLoader;

import org.slf4j.Logger;

/**
 * Class responsible for managing users in the identity provider.
 */
@Stateless
public class UserManager {

    /**
     * Name of the configuration file.
     */
    private static final String CONFIG_NAME = "mail.properties";

    /**
     * Entitymanager.
     */
    @Inject
    private EntityManager em;

    /**
     * Logger for this class.
     */
    @Inject
    private Logger log;

    /**
     * Standard rolename for a user.
     */
    private static final String STANDARD_ROLE_NAME = "authenticated";

    /**
     * Properties for activation mail.
     */
    private Properties mailProperties;

    /**
     * Init this class.
     */
    @PostConstruct
    public void init() {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        mailProperties = propertiesLoader.load(CONFIG_NAME);
    }

    /**
     * Method responsible for adding a new user.
     * 
     * @param user
     *            User to add.
     * @throws CreateUserException
     *             if the user could not be created
     */
    public void addUser(IdpUser user) throws CreateUserException {
        // Set standard role

        IdpRole role = null;
        List<IdpRole> standardRoles = em
            .createQuery("SELECT r from IdpRole r WHERE rolename = :rolename", IdpRole.class)
            .setParameter("rolename", STANDARD_ROLE_NAME).getResultList();
        if (standardRoles.size() == 1) {
            role = standardRoles.get(0);
        } else {
            role = new IdpRole();
            role.setRoleName(STANDARD_ROLE_NAME);
        }

        List<IdpRole> roles = user.getRoles();
        roles.add(role);

        // Create a user actionToken which is needed for activation
        user.setActionToken(UUID.randomUUID().toString());
        user.setStatus(IdpUserState.CREATED);

        // Check if data unique
        final List<IdpUser> usernameUsers = em
            .createQuery("SELECT u FROM IdpUser u WHERE u.username = :username", IdpUser.class)
            .setParameter("username", user.getUsername()).getResultList();
        if (!usernameUsers.isEmpty()) {
            log.info("Error creating user. Username exists {}", user.getUsername());
            throw new CreateUserException("Error creating user. Username exists " + user.getUsername());
        }

        // Check if data unique
        final List<IdpUser> emailUsers = em
            .createQuery("SELECT u FROM IdpUser u WHERE u.email = :email", IdpUser.class)
            .setParameter("email", user.getEmail()).getResultList();
        if (!emailUsers.isEmpty()) {
            log.info("Error creating user. Email exists {}", user.getUsername());
            throw new CreateUserException("Error creating user. Email exists " + user.getUsername());
        }

        em.persist(user);
        log.info("Added user with username " + user.getUsername());
    }

    /**
     * Method responsible for activating an already created user.
     * 
     * @param user
     *            the user to activate
     * @throws UserNotFoundExeception
     *             if the user could not be found
     */
    public void activateUser(IdpUser user) throws UserNotFoundExeception {
        IdpUser foundUser = em.find(IdpUser.class, user.getId());
        if (foundUser == null) {
            log.error("Error activating user. User not found {}.", user.getUsername());
            throw new UserNotFoundExeception("Error activating user. User not found " + user.getUsername());
        }
        foundUser.setStatus(IdpUserState.ACTIVE);
        foundUser.setActionToken("");
        em.persist(foundUser);

        log.info("Activated user with username " + foundUser.getUsername());
    }

    /**
     * Method responsible for sending a email to the user, including a link to
     * activate his user account.
     * 
     * @param user
     *            User the activation mail should be sent to
     * @param serverString
     *            Name and port of the server the user was added.
     * @throws CannotSendMailException
     *             if the mail could not be sent
     */
    public void sendActivationMail(IdpUser user, String serverString) throws CannotSendMailException {

        try {
            Properties props = System.getProperties();

            props.put("mail.smtp.host", mailProperties.getProperty("server.smtp"));
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailProperties.getProperty("mail.from")));
            message.setRecipient(RecipientType.TO, new InternetAddress(user.getEmail()));
            message.setSubject("Please confirm your Planningsuite user account");

            StringBuilder builder = new StringBuilder();
            builder.append("Dear " + user.getFirstName() + " " + user.getLastName() + ", \n\n");
            builder.append("Please use the following link to confirm your Planningsuite user account: \n");
            builder.append("http://" + serverString + "/idp/activateUser.jsf?uid=" + user.getActionToken());
            builder.append("\n\n--\n");
            builder.append("Your Planningsuite team");

            message.setText(builder.toString());
            message.saveChanges();

            Transport.send(message);
            log.debug("Activation mail sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error at sending activation mail to {}", user.getEmail());
            throw new CannotSendMailException("Error at sending activation mail to " + user.getEmail(), e);
        }
    }

    /**
     * Initiates password reset for the user.
     * 
     * @param userIdentifier
     *            the identifier of the user
     * @param serverString
     *            host and port of the server
     * @throws UserNotFoundExeception
     *             if the user could not be found
     * @throws CannotSendMailException
     *             if the password reset mail could not be sent
     */
    public void initiateResetPassword(String userIdentifier, String serverString) throws UserNotFoundExeception,
        CannotSendMailException {
        List<IdpUser> matchingUsers = em
            .createQuery("SELECT u FROM IdpUser u WHERE u.username = :username OR u.email = :email", IdpUser.class)
            .setParameter("username", userIdentifier).setParameter("email", userIdentifier).getResultList();

        if (matchingUsers.size() != 1) {
            log.error("{} users matching given identifier {}", matchingUsers.size(), userIdentifier);
            throw new UserNotFoundExeception(matchingUsers.size() + " users matching given identifier "
                + userIdentifier);
        }

        IdpUser user = matchingUsers.get(0);

        user.setActionToken(UUID.randomUUID().toString());
        em.persist(user);

        sendPasswordResetMail(user, serverString);

        log.info("Sent password reset mail for identifier {}", userIdentifier);
    }

    /**
     * Sends the user a link to reset the password.
     * 
     * @param user
     *            the user
     * @param serverString
     *            host and port of the server
     * @throws CannotSendMailException
     *             if the password reset mail could not be sent
     */
    private void sendPasswordResetMail(IdpUser user, String serverString) throws CannotSendMailException {
        try {
            Properties props = System.getProperties();

            props.put("mail.smtp.host", mailProperties.getProperty("server.smtp"));
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailProperties.getProperty("mail.from")));
            message.setRecipient(RecipientType.TO, new InternetAddress(user.getEmail()));
            message.setSubject("Planningsuite password recovery");

            StringBuilder builder = new StringBuilder();
            builder.append("Dear " + user.getFirstName() + " " + user.getLastName() + ", \n\n");
            builder.append("Please use the following link to reset your Planningsuite password: \n");
            builder.append("http://" + serverString + "/idp/resetPassword.jsf?uid=" + user.getActionToken());
            builder.append("\n\n--\n");
            builder.append("Your Planningsuite team");

            message.setText(builder.toString());
            message.saveChanges();

            Transport.send(message);
            log.debug("Sent password reset mail to " + user.getEmail());

        } catch (Exception e) {
            log.error("Error at sending password reset mail to {}", user.getEmail());
            throw new CannotSendMailException("Error at sending password reset mail to " + user.getEmail());
        }
    }

    /**
     * Resets the password of the user identified by the actionToken.
     * 
     * @param user
     *            the user
     * @throws UserNotFoundExeception
     *             if the user could not be found
     */
    public void resetPassword(IdpUser user) throws UserNotFoundExeception {
        IdpUser foundUser = em.find(IdpUser.class, user.getId());
        if (foundUser == null) {
            log.error("Error resetting password. User not found {}.", user.getUsername());
            throw new UserNotFoundExeception("Error resetting password. User not found " + user.getUsername());
        }
        foundUser.setPlainPassword(user.getPlainPassword());
        foundUser.setActionToken("");
        foundUser.setStatus(IdpUserState.ACTIVE);
        em.persist(foundUser);

        log.info("Reset password for user " + user.getUsername());
    }

    /**
     * Reads the user identified by the provided action token.
     * 
     * @param actionToken
     *            the action token identifying the user
     * @return the user
     * @throws UserNotFoundExeception
     *             if no user could be found
     */
    public IdpUser getUserByActionToken(String actionToken) throws UserNotFoundExeception {
        List<IdpUser> matchingUsers = em
            .createQuery("SELECT u FROM IdpUser u WHERE u.actionToken = :actionToken", IdpUser.class)
            .setParameter("actionToken", actionToken).getResultList();

        if (matchingUsers.size() != 1) {
            log.error("{} users matching given actionToken {}", matchingUsers.size(), actionToken);
            throw new UserNotFoundExeception(matchingUsers.size() + " users matching given actionToken " + actionToken);
        }

        return matchingUsers.get(0);
    }

    // ---------- getter/setter ----------

    // Method used to make this class Unit-testable
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    // Method used to make this class Unit-testable
    public void setLog(Logger log) {
        this.log = log;
    }
}
