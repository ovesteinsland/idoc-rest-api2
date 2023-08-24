
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.authentication.PasswordAuthentication;
import no.softwarecontrol.idoc.data.entityhelper.IDocCredentials;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.opplysningen1881.ContactPoints;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Contacts;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Opplysningen1881Client;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Opplysningen1881Result;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.user")
//@RolesAllowed({"ApplicationRole"})
public class UserFacadeREST extends AbstractFacade<User> {

    @EJB
    private CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();

    public UserFacadeREST() {
        super(User.class);
    }


    @Override
    protected String getSelectAllQuery() {
        return "User.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(User entity) {
        User existing = find(entity.getUserId());
        if (existing == null) {
            super.create(entity);
        }
    }

    @POST
    @Path("createWithCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, User entity) {
        User existing = find(entity.getUserId());
        if (existing == null) {
            super.create(entity);
            linkToCompany(companyId,entity.getUserId());
        }
    }


    @PUT
    @Path("editPassword2/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editPassword2(@PathParam("id") String id, User entity) {
        PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
        String passwordString = entity.getPassword();
        String hashedPassword = passwordAuthentication.hash(passwordString.toCharArray());

        User user = this.find(entity.getUserId());
        if (user != null) {
            user.setLoginName(entity.getLoginName());
            user.setPassword(hashedPassword);
            user.setPinCode(entity.getPinCode());
            user.setActivationCode(entity.getActivationCode());
            super.edit(user);
        }
    }


    @PUT
    @Path("editPassword/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editPassword(@PathParam("id") String id, User entity) {
        User user = this.find(entity.getUserId());
        if (user != null) {
            user.setLoginName(entity.getLoginName());
            user.setPassword(entity.getPassword());
            user.setPinCode(entity.getPinCode());
            user.setActivationCode(entity.getActivationCode());
            super.edit(user);
        }
    }

    public void editInternal(User entity) {
        User user = this.find(entity.getUserId());
        if (user != null) {
            user.setLoginName(entity.getLoginName());
            user.setPassword(entity.getPassword());
            user.setPinCode(entity.getPinCode());
            user.setActivationCode(entity.getActivationCode());
            super.edit(user);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, User entity) {
        User user = this.find(entity.getUserId());
        if (user != null) {
            /*for(Certificate certificate : entity.getCertificateList()) {
                if(!certificate.getUserList().contains(user)) {
                    certificate.getUserList().add(user);
                }
            }*/
            //user.getCertificateList().clear();
            //user.setCertificateList(entity.getCertificateList());
            user.setDeleted(entity.isDeleted());
            user.setEmail(entity.getEmail());
            user.setFirstname(entity.getFirstname());
            user.setLastname(entity.getLastname());
            user.setLoginName(entity.getLoginName());
            user.setMobile(entity.getMobile());
            user.setPhone(entity.getPhone());
            user.setUserToken(entity.getUserToken());
            //user.setPassword(entity.getPassword());
            user.setPinCode(entity.getPinCode());
            user.setActivationCode(entity.getActivationCode());
            user.setJobTitle(entity.getJobTitle());
            user.setNotificationByEmail(entity.getNotificationByEmail());
            user.setNotificationByIDoc(entity.getNotificationByIDoc());
            user.setNotificationBySms(entity.getNotificationBySms());
            user.setAuthority(entity.getAuthority());
            user.setLicence(entity.getLicence());
            if (!entity.getIntegrationList().isEmpty()) {
                user.setIntegrationList(entity.getIntegrationList());
            }
            super.edit(user);

            CertificateFacadeREST certificateFacadeREST = new CertificateFacadeREST();
            for (Certificate certificate : user.getCertificateList()) {
                certificateFacadeREST.edit(certificate.getCertificateId(), certificate);
            }
        }
    }

    @PUT
    @Path("editWithRoles/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editWithRoles(@PathParam("id") String id, User entity) {
        User user = this.find(entity.getUserId());
        if (user != null) {
            user.setDeleted(entity.isDeleted());
            user.setEmail(entity.getEmail());
            user.setFirstname(entity.getFirstname());
            user.setLastname(entity.getLastname());
            user.setLoginName(entity.getLoginName());
            user.setMobile(entity.getMobile());
            user.setPhone(entity.getPhone());
            user.setUserToken(entity.getUserToken());
            //user.setUserRoleList(entity.getUserRoleList());
            super.edit(user);
        }
        //super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public User find(@PathParam("id") String id) {
        User user = super.find(id);
        return user;
    }

    @GET
    @Path("loadOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public User loadOptimized(@PathParam("id") String id) {
        User user = super.find(id);
        user.setProjectList(new ArrayList<>());
        user.setObservationList(new ArrayList<>());
        return user;
    }

    @GET
    @Path("findConversations/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Conversation> findConversations(@PathParam("userId") String userId) {
        List<Conversation> conversations = new ArrayList<>();
        User user = find(userId);
        if (user != null) {
            conversations.addAll(user.getConversationList());
        }
        return conversations;
    }

    @GET
    @Path("findByLoginName/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findByLoginName(@PathParam("id") String id) {
        //TypedQuery<User> query = em.createNamedQuery("User.findByLoginName", User.class);
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        Query query = em.createNamedQuery("User.findByLoginName");
        query.setParameter("loginName", id);
        List<User> users = query.getResultList();

        em.close();
        return users;
    }

    @GET
    @Path("countByLoginName/{loginName}")
    @Produces({MediaType.APPLICATION_JSON})
    public String countByLoginName(@PathParam("loginName") String loginName) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        Query query = em.createNamedQuery("User.findByLoginName");
        query.setParameter("loginName", loginName);
        List<User> users = query.getResultList();

        em.close();
        return String.valueOf(users.size());
    }

    @GET
    @Path("findContacts/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserContact> findContacts(@PathParam("id") String id) {
        User user = find(id);
        List<UserContact> contacts = new ArrayList<>();
        for (UserContact userContact : user.getContactList()) {
            contacts.add(userContact);
        }
        return contacts;
    }

    @POST
    @Path("authenticateWithCredentials")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> authenticateWithCredentials(IDocCredentials iDocCredentials) {
        String username = iDocCredentials.getUsername();
        String password = iDocCredentials.getPassword();
        return authenticate(username, password);
    }

    @GET
    @Path("authenticate/{username}/{password}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> authenticate(@PathParam("username") String username, @PathParam("password") String password) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query query = em.createNamedQuery("User.findByLoginName");
        query.setParameter("loginName", username);
        List<User> results = query.getResultList();
        if (!results.isEmpty()) {
            PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
            String hashedPassword = results.get(0).getPassword();
            if (passwordAuthentication.authenticate(password.toCharArray(), hashedPassword)) {
            } else {
                System.out.println("Authentication FAILED");
                results = new ArrayList<>();
            }
        } else {
            System.out.println("results = EMPTY");
        }
        em.close();
        return results;
    }

    @GET
    @Path("authenticateWithEncoding/{loginName}/{password}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> authenticateWithEncoding(@PathParam("loginName") String loginName, @PathParam("password") String password) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            String decodedLoginName = URLDecoder.decode(loginName, StandardCharsets.UTF_8.toString());
            Query query = em.createNamedQuery("User.findByLoginName");
            query.setParameter("loginName", decodedLoginName);
            //List<User> results = query.getResultList();
            List<User> results = new ArrayList<>();
            User user = findByLoginNameNative(decodedLoginName);
            if (user != null) {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
                String hashedPassword = user.getPassword();
                System.out.println("hashedPassword = " + hashedPassword);
                String decodedPassword = URLDecoder.decode(password, StandardCharsets.UTF_8.toString());
                if (passwordAuthentication.authenticate(decodedPassword.toCharArray(), hashedPassword)) {
                    System.out.println("Authentication SUCCEEDED");
                    results.add(user);
                } else {
                    System.out.println("Authentication FAILED");
                    results = new ArrayList<>();
                }
            } else {
                System.out.println("results = EMPTY");
            }

            /*if (!results.isEmpty()) {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
                String hashedPassword = results.get(0).getPassword();
                System.out.println("hashedPassword = " + hashedPassword);
                String decodedPassword = URLDecoder.decode(password, StandardCharsets.UTF_8.toString());
                if (passwordAuthentication.authenticate(decodedPassword.toCharArray(), hashedPassword)) {
                    System.out.println("Authentication SUCCEEDED");
                } else {
                    System.out.println("Authentication FAILED");
                    results = new ArrayList<>();
                }
            } else {
                System.out.println("results = EMPTY");
            }*/
            //em.close();
            return results;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<User>();
        } finally {
            em.close();
        }
    }


    public User findByLoginNameNative(String login) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<User> resultList = (List<User>) em.createNativeQuery("SELECT "
                                + "* FROM user u\n"
                                + "WHERE u.login_name = ?1",
                        User.class)
                .setParameter(1, login)
                .getResultList();
        em.close();
        if (resultList.isEmpty()) {
            return null;
        } else {
            User user = resultList.get(0);

            return user;
            //return temp;
        }
    }

    @GET
    @Path("searchUsers/{searchString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> searchUsers(@PathParam("searchString") String searchString) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        searchString = "%" + searchString + "%";
        List<User> resultList = (List<User>) em.createNativeQuery("SELECT * FROM user\n"
                                + "WHERE (CONCAT(firstname, ' ', lastname) LIKE ?1 OR \n"
                                + "	CONCAT(lastname, ' ', firstname) LIKE ?1) AND deleted = 0\n"
                                + "ORDER BY lastname, firstname ASC",
                        User.class)
                .setParameter(1, searchString)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("usercompanies/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> findUserCompanies(@PathParam("userId") String userId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT " +
                                "c.* FROM company c " +
                                "JOIN company_has_user chu ON chu.company = c.company_id " +
                                "JOIN user u ON chu.user = u.user_id " +
                                "WHERE u.user_id = ?1",
                        Company.class)
                .setParameter(1, userId)
                .getResultList();
        em.close();
        return resultList;
    }

    @Deprecated
    @GET
    @Path("loadUserCompaniesOptimized/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadUserCompaniesOptimized(@PathParam("userId") String userId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT " +
                                "c.* FROM company c " +
                                "JOIN company_has_user chu ON chu.company = c.company_id " +
                                "JOIN user u ON chu.user = u.user_id " +
                                "WHERE u.user_id = ?1",
                        Company.class)
                .setParameter(1, userId)
                .getResultList();
        em.close();
        for (Company company : resultList) {
            //company.getProjectList().clear();
            //company.getAssetList().clear();
            company.setProjectList(new ArrayList<>());
            company.setAssetList(new ArrayList<>());
            company.setUserList(new ArrayList<>());
            company.setContractList(new ArrayList<>());
            for (Disipline disipline : company.getDisiplineList()) {
                disipline.setCompanyList(new ArrayList<>());
            }
            for (AssetGroup assetGroup : company.getAssetGroupList()) {
                assetGroup.setAssetList(new ArrayList<>());
            }
        }
        return resultList;
    }

    @GET
    @Path("loadUserCompaniesOptimized2/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadUserCompaniesOptimized2(@PathParam("userId") String userId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT " +
                                "c.* FROM company c " +
                                "JOIN company_has_user chu ON chu.company = c.company_id " +
                                "JOIN user u ON chu.user = u.user_id " +
                                "WHERE u.user_id = ?1",
                        Company.class)
                .setParameter(1, userId)
                .getResultList();
        em.close();
        for (Company company : resultList) {
            //company.getProjectList().clear();
            //company.getAssetList().clear();
            company.setProjectList(new ArrayList<>());
            company.setAssetList(new ArrayList<>());
            company.setUserList(new ArrayList<>());
            company.setContractList(new ArrayList<>());
            //company.getDisiplineList().clear();
            for (Disipline disipline : company.getDisiplineList()) {
                disipline.getCompanyList().clear();
                //disipline.getEquipmentTypeList().clear();
            }
            company.getReportList().clear();
            for (AssetGroup assetGroup : company.getAssetGroupList()) {
                assetGroup.setAssetList(new ArrayList<>());
            }
        }
        return resultList;
    }


    @GET
    @Path("loadByCompanyAll/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByCompanyAll(@PathParam("companyId") String companyId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<User> resultList = (List<User>) em.createNativeQuery("SELECT " +
                                "* FROM user u " +
                                "JOIN company_has_user chu ON chu.user = u.user_id " +
                                "WHERE chu.company = ?1",
                        User.class)
                .setParameter(1, companyId)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadByCompany/{companyId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByCompany(@PathParam("companyId") String companyId, @PathParam("authorityId") String authorityId) {
//        EntityManager em = LocalEntityManagerFactory.createEntityManager();
//        List<User> resultList = (List<User>) em.createNativeQuery("SELECT " +
//                "* FROM user u " +
//                "JOIN company_has_user chu ON chu.user = u.user_id " +
//                "JOIN company_has_user ahu ON ahu.user = u.user_id " +
//                "WHERE chu.company = ?1 AND ahu.company = ?2",
//                User.class)
//                .setParameter(1, companyId)
//                .setParameter(2, authorityId)
//                .getResultList();
//        em.close();

        List<User> resultList = loadByCompanyAll(companyId);
        return resultList;
    }

    @GET
    @Path("loadByMobile/{mobile}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByMobile(@PathParam("mobile") String mobile) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<User> resultList = (List<User>) em.createNativeQuery("SELECT " +
                                "* FROM user u " +
                                "WHERE u.mobile = ?1",
                        User.class)
                .setParameter(1, mobile)
                .getResultList();
        em.close();

        return resultList;
    }

    @GET
    @Path("loadByMobileAndEmail/{mobile}/{email}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByMobile(@PathParam("mobile") String mobile, @PathParam("email") String email) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<User> resultList = (List<User>) em.createNativeQuery("SELECT " +
                                "* FROM user u " +
                                "WHERE u.mobile = ?1 AND u.email = ?2",
                        User.class)
                .setParameter(1, mobile)
                .setParameter(2, email)
                .getResultList();
        em.close();

        return resultList;
    }

    @GET
    @Path("loadByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByProject(@PathParam("projectId") String projectId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        List<User> resultList = (List<User>) em.createNativeQuery("select * from user u "
                        + "join project_has_user phu on phu.user_user_id = u.user_id "
                        + "join project p on phu.project_project_id = p.project_id "
                        + "where p.project_id = ?1", User.class)
                .setParameter(1, projectId).getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    private void linkToCompany(String companyId, String userId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_user \n " +
                            " WHERE company = ?1 AND user = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, userId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO company_has_user (company, user)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, companyId)
                        .setParameter(2, userId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: company_has_project already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_project: " + exp.getMessage());
        } finally {
            em.close();
        }
    }
    @PUT
    @Path("linkToCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId, User entity) {
        User user = this.find(entity.getUserId());
        Company company = companyFacadeREST.find(companyId);
        if (user != null && company != null) {
            if (!user.getCompanyList().contains(company)) {
                user.getCompanyList().add(company);
                this.edit(user);
            }
            if (!company.getUserList().contains(user)) {
                company.getUserList().add(user);
                companyFacadeREST.edit(company);
            }
        }
    }

    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("companyId") String companyId, User entity) {
        User user = this.find(entity.getUserId());
        Company company = companyFacadeREST.find(companyId);
        if (user != null && company != null) {
            companyFacadeREST.removeUser(company, user);
            if (user.getCompanyList().contains(company)) {
                user.getCompanyList().remove(company);
                this.edit(user);
            }
        }
    }

    @PUT
    @Path("unlinkImage/{userId}/{mediaId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkImage(@PathParam("userId") String userId, @PathParam("mediaId") String mediaId) {
        User user = this.find(userId);

        ImageFacadeREST imageFacadeREST = new ImageFacadeREST();
        Media media = imageFacadeREST.find(mediaId);
        if (user != null && media != null) {

            if (user.getImageList().contains(media)) {
                user.getCompanyList().remove(media);
                //this.edit(user);
            }
            imageFacadeREST.remove(media.getMediaId());
        }
    }

    @PUT
    @Path("linkUserContact/{contactId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkUserContact(@PathParam("contactId") String contactId, User entity) {
        User user = this.find(entity.getUserId());
        User contact = this.find(contactId);
        if (user != null && contact != null) {
            UserContact userContact = new UserContact(UUID.randomUUID().toString());
            userContact.setAcceptedByUser(true);
            userContact.setAcceptedByContact(false);
            userContact.setContact(contact);
            userContact.setUser(user);
            user.getContactList().add(userContact);
            contact.getUserContactList1().add(userContact);

            UserContact contactContact = new UserContact(UUID.randomUUID().toString());
            contactContact.setAcceptedByUser(false);
            contactContact.setAcceptedByContact(true);
            contactContact.setContact(user);
            contactContact.setUser(contact);
            contact.getContactList().add(contactContact);
            user.getUserContactList1().add(contactContact);

            this.edit(user);
            this.edit(contact);
        }
    }

    public static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int i = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @GET
    @Path("search1881/{queryString}/{searchLevel}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> search1881(@PathParam("queryString") String queryString, @PathParam("searchLevel") String searchLevel) {

        Opplysningen1881Client client = new Opplysningen1881Client();
        boolean isSearching = false;
        Opplysningen1881Result opplysningen1881Result = null;
        if (isInteger(queryString)) {
            opplysningen1881Result = client.lookupPhoneNumber(queryString);
        } else {
            String decodedMessage = null;
            try {
                decodedMessage = URLDecoder.decode(queryString, "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
            isSearching = true;
            opplysningen1881Result = client.search(decodedMessage);
        }
        List<User> resultList = new ArrayList<>();
        if (opplysningen1881Result != null) {
            for (Contacts resultItem : opplysningen1881Result.getContacts()) {
                User user = new User();
                user.setUserId(UUID.randomUUID().toString());
                if (isSearching) {
                    String formattedAddress = resultItem.getPostCode() + " " + resultItem.getPostArea();
                    user.setFormattedAddress(formattedAddress);

                    String[] names = resultItem.getName().split(" ");
                    if (names.length > 1) {
                        String firstname = "";
                        String lastname = "";
                        for (int i = 0; i < names.length - 1; i++) {
                            firstname = firstname + names[i];
                            if (i < names.length - 2) {
                                firstname = firstname + " ";
                            }
                        }
                        lastname = names[names.length - 1];
                        user.setFirstname(firstname);
                        user.setLastname(lastname);
                        user.setUserToken(resultItem.id);
                    }
                } else {
                    user.setFirstname(resultItem.getFirstName());
                    user.setLastname(resultItem.getLastName());
                }
                if (!resultItem.getContactPoints().isEmpty()) {
                    ContactPoints contactPoints = resultItem.getContactPoints().get(0);
                    user.setMobile(contactPoints.getValue());
                }
                user.setCreateRequested(true);
                user.setSearchService("Search1881");
                if (resultItem.getGeography() != null) {
                    user.setFormattedAddress(resultItem.getGeography().getAddress().getAddressString());
                }
                if (!user.getFirstname().isEmpty() || !user.getLastname().isEmpty()) {
                    resultList.add(user);
                }
            }
        }
        return resultList;
    }


    @GET
    @Path("lookup1881/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> lookup1881(@PathParam("id") String id) {

        Opplysningen1881Client client = new Opplysningen1881Client();
        Opplysningen1881Result opplysningen1881Result = client.lookupId(id);

        List<User> resultList = new ArrayList<>();
        if (opplysningen1881Result != null) {
            for (Contacts resultItem : opplysningen1881Result.getContacts()) {
                User user = new User();
                user.setUserId(UUID.randomUUID().toString());

                user.setFirstname(resultItem.getFirstName());
                user.setLastname(resultItem.getLastName());

                if (!resultItem.getContactPoints().isEmpty()) {
                    ContactPoints contactPoints = resultItem.getContactPoints().get(0);
                    user.setMobile(contactPoints.getValue());
                }
                user.setCreateRequested(true);
                user.setSearchService("Search1881");
                if (resultItem.getGeography() != null) {
                    user.setFormattedAddress(resultItem.getGeography().getAddress().getAddressString());
                }
                if (!user.getFirstname().isEmpty() || !user.getLastname().isEmpty()) {
                    resultList.add(user);
                }
            }
        }
        return resultList;
    }


//    @GET
//    @Path("findContacts/{id}")
//    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
//    public List<User> findContacts(@PathParam("id") String id) {
//
//        User user
//        return users;
//    }

}
