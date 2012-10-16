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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import eu.scape_project.pw.idp.excpetions.CreateUserException;
import eu.scape_project.pw.idp.excpetions.UserNotFoundExeception;
import eu.scape_project.pw.idp.model.IdpRole;
import eu.scape_project.pw.idp.model.IdpUser;
import eu.scape_project.pw.idp.model.IdpUserState;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

public class UserManagerTest {
    private UserManager userManager;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public UserManagerTest() {
        this.userManager = new UserManager();

        Logger log = mock(Logger.class);
        this.userManager.setLog(log);
    }

    @Test
    public void addUser_standardRoleHasToBeGenerated() throws CreateUserException {
        // -- set up --
        EntityManager em = mock(EntityManager.class);
        TypedQuery<IdpRole> query = mock(TypedQuery.class);
        TypedQuery<IdpRole> parameterQuery = mock(TypedQuery.class);
        when(em.createQuery("SELECT r from IdpRole r WHERE rolename = :rolename", IdpRole.class)).thenReturn(query);
        when(query.setParameter(anyString(), anyObject())).thenReturn(parameterQuery);
        when(parameterQuery.getSingleResult()).thenThrow(new NoResultException());
        userManager.setEntityManager(em);

        // -- test --
        IdpUser submittedUser = new IdpUser();
        submittedUser.setFirstName("firstname");
        submittedUser.setFirstName("lastname");
        submittedUser.setEmail("email");
        submittedUser.setUsername("username");
        submittedUser.setPlainPassword("password");
        userManager.addUser(submittedUser);

        // -- assert --
        verify(em).persist(submittedUser);
        assertTrue(submittedUser.getActionToken().length() > 1);
        assertEquals("authenticated", submittedUser.getRoles().get(0).getRoleName());
    }

    @Test
    public void addUser_standardRoleExists() throws CreateUserException {
        // -- set up --
        EntityManager em = mock(EntityManager.class);
        TypedQuery<IdpRole> query = mock(TypedQuery.class);
        TypedQuery<IdpRole> parameterQuery = mock(TypedQuery.class);
        when(em.createQuery("SELECT r from IdpRole r WHERE rolename = :rolename", IdpRole.class)).thenReturn(query);
        when(query.setParameter(anyString(), anyObject())).thenReturn(parameterQuery);
        IdpRole authenticatedRole = new IdpRole();
        authenticatedRole.setRoleName("authenticated");
        when(parameterQuery.getSingleResult()).thenReturn(authenticatedRole);
        userManager.setEntityManager(em);

        // -- test --
        IdpUser submittedUser = new IdpUser();
        submittedUser.setFirstName("firstname");
        submittedUser.setFirstName("lastname");
        submittedUser.setEmail("email");
        submittedUser.setUsername("username");
        submittedUser.setPlainPassword("password");
        userManager.addUser(submittedUser);

        // -- assert --
        verify(em).persist(submittedUser);
        assertTrue(submittedUser.getActionToken().length() > 1);
        assertEquals("authenticated", submittedUser.getRoles().get(0).getRoleName());
    }

    @Test
    public void activateUser_actionTokenOK_userIsActivated_success() throws UserNotFoundExeception {
        // -- set up --
        EntityManager em = mock(EntityManager.class);
        IdpUser user = mock(IdpUser.class);
        when(em.find(IdpUser.class, user.getId())).thenReturn(user);
        userManager.setEntityManager(em);

        // -- test --
        userManager.activateUser(user);

        // -- assert --
        verify(user).setStatus(IdpUserState.ACTIVE);
        verify(user).setActionToken("");
        verify(em).persist(user);
    }
}
