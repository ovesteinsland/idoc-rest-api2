
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.authentication.PasswordAuthentication;
import no.softwarecontrol.idoc.data.entityhelper.CustomerData;
import no.softwarecontrol.idoc.data.entityhelper.IDocCredentials;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.opplysningen1881.ContactPoints;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Contacts;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Opplysningen1881Client;
import no.softwarecontrol.idoc.webservices.opplysningen1881.Opplysningen1881Result;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
import no.softwarecontrol.idoc.webservices.restapi.ratelimit.RateLimit;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author ovesteinsland
 */
@SuppressWarnings("LanguageDetectionInspection")
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.user")
@RolesAllowed({"ApplicationRole"})
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

    @Deprecated()
    @POST
    @Path("signup")
    @Consumes({MediaType.APPLICATION_JSON})
    public String signup(CustomerData entity) {
        User user = signupUser(entity);
        return user.getUserId();
    }

    @GET
    @Path("countByLoginName/{loginName}")
    @Produces({MediaType.APPLICATION_JSON})
    @PermitAll
    @RateLimit(requests = 10, seconds = 60)
    public String countByLoginName(@PathParam("loginName") String loginName) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            String sql = "SELECT COUNT(*) FROM user WHERE login_name = ?1";
            Number count = (Number) em.createNativeQuery(sql)
                    .setParameter(1, loginName)
                    .getSingleResult();

            long counter = count.longValue();
            if(counter > 0){ counter = 1L; }
            return String.valueOf(counter);
        } finally {
            em.close();
        }
    }

    @POST
    @Path("signupUser")
    @Consumes({MediaType.APPLICATION_JSON})
    @PermitAll
    @RateLimit(requests = 10, seconds = 60)
    public User signupUser(CustomerData entity) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        ContractFacadeREST contractFacadeREST = new ContractFacadeREST();
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        LanguageFacadeRest languageFacadeREST = new LanguageFacadeRest();

        Company authority = new Company();
        authority.setCompanyId(UUID.randomUUID().toString());
        authority.setName(entity.getCompany());
        authority.setCompanyType("AUTHORITY");
        authority.setIsLiteAccount(true);
        authority.setDefaultLanguageCode(entity.getLanguageCode());
        companyFacadeREST.create(authority);

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFirstname(entity.getFirstname());
        user.setLastname(entity.getLastname());
        user.setEmail(entity.getEmail());
        user.setLoginName(entity.getEmail());
        user.setPassword(entity.getPassword());
        user.setAuthority(authority.getCompanyId());
        super.create(user);

        this.editPassword2(user.getUserId(), user);

        System.out.println("--------------------------------------------");
        System.out.println("UserID = " + user.getUserId());

        linkToCompany(authority.getCompanyId(), user.getUserId());
        System.out.println("--------------------------------------------");
        System.out.println("AuthorityId = " + authority.getCompanyId());

        Company customer = new Company();
        customer.setCompanyId(UUID.randomUUID().toString());
        customer.setName("Generic Customer");
        customer.setCompanyType("OWNER");
        customer.setIsLiteAccount(true);
        authority.setDefaultLanguageCode(entity.getLanguageCode());
        companyFacadeREST.create(customer);

        Company softwareControl = companyFacadeREST.find("a84bdb6d-6f4b-4116-985b-6418ade5e957");
        Contract authorityContract = new Contract();
        authorityContract.setContractId(UUID.randomUUID().toString());
        authorityContract.setPartner(authority);
        authorityContract.setContractType("CUSTOMER");
        authorityContract.setCompany(softwareControl);
        contractFacadeREST.create(authorityContract);
        System.out.println("--------------------------------------------");
        System.out.println("Authority: ContractId = " + authorityContract.getContractId());

        Contract contract = new Contract();
        contract.setContractId(UUID.randomUUID().toString());
        contract.setPartner(customer);
        contract.setContractType("CUSTOMER");
        contract.setCompany(authority);
        contractFacadeREST.create(contract);

        // Link company to disipline
        disiplineFacadeREST.linkToCompany(authority.getCompanyId(), "7e445a7a-ce06-474c-93ff-d718a8c8cfe70"); // NEK 405-1 Termografering

        // Link company to language
        List<Language> languages = languageFacadeREST.findAll();
        Language language = languages.stream()
                .filter(l -> (entity.getLanguageCode()).equals(l.getLanguageCode()))
                .findFirst()
                .orElse(null);
        if (language == null) {
            language = languages.stream()
                    .filter(l -> ("en").equals(l.getLanguageCode()))
                    .findFirst()
                    .orElse(null);
            if (language == null) {
                language = languages.get(0);
            }
        }
        languageFacadeREST.linkToCompany(authority.getCompanyId(), language.getLanguageId());

        AssetGroupFacadeREST assetGroupFacadeREST = new AssetGroupFacadeREST();
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.setAssetGroupId(UUID.randomUUID().toString());
        assetGroup.setName("Asset Group");
        assetGroupFacadeREST.create(assetGroup);
        assetGroupFacadeREST.linkToCompany(authority.getCompanyId(), assetGroup);
        assetGroupFacadeREST.linkToCompany(customer.getCompanyId(), assetGroup);
        System.out.println("--------------------------------------------");
        System.out.println("AssetGroupId = " + assetGroup.getAssetGroupId());


        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Asset asset = new Asset();
        asset.setAssetId(UUID.randomUUID().toString());
        asset.setAssetGroup(assetGroup);
        asset.setName("Default Asset");
        assetFacadeREST.create(asset);
        assetFacadeREST.linkCompany(authority.getCompanyId(), asset);
        assetFacadeREST.linkCompany(customer.getCompanyId(), asset);
        System.out.println("--------------------------------------------");
        System.out.println("AssetId = " + asset.getAssetId());
        System.out.println("-------------     Signup   -----------------");
        System.out.println("-------------   Completed! -----------------");
        System.out.println();
        return user;
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
            linkToCompany(companyId, entity.getUserId());
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
            user.setPreferenceJson(entity.getPreferenceJson());
            user.setSub(entity.getSub());
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
        try {
            String sqlString = """
                    SELECT *
                    FROM user 
                    WHERE login_name= ?1 
                    """;

            List<User> results = (List<User>) em.createNativeQuery(sqlString, User.class)
                    .setParameter(1, username)
                    .getResultList();

            //List<User> results = query.getResultList();
            if (!results.isEmpty()) {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
                String hashedPassword = results.get(0).getPassword();
                if (hashedPassword != null) {
                    if (passwordAuthentication.authenticate(password.toCharArray(), hashedPassword)) {

                    } else {
                        results = new ArrayList<>();
                    }
                } else {
                    results = new ArrayList<>();
                }
            } else {
                results = new ArrayList<>();
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            em.close();
        }
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
                    results.add(user);
                } else {
                    results = new ArrayList<>();
                }
            } else {
                System.out.println("results = EMPTY");
            }
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
            //company.setContractList(new ArrayList<>());
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
    @Path("loadPartnerUsers/{authorityId}/{customerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadPartnerUsers(@PathParam("authorityId") String authorityId, @PathParam("customerId") String customerId) {
        String sqlQuery = """
                SELECT u.*, concat(cust.name,cust.lastname,' ',cust.firstname) as companyName, cust.company_id as companyId, con.contract_type as contractType \s
                FROM user u
                         JOIN company_has_user chu ON u.user_id = chu.user
                         JOIN contract con ON chu.company = con.partner
                         JOIN company cust on con.partner = cust.company_id
                WHERE con.company = ?1
                  AND (con.contract_type = 'ENTREPRENEUR' or con.partner = ?2)
                """;

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Object[]> results = em.createNativeQuery(sqlQuery /*, "UserResultMapping"*/)
                .setParameter(1, authorityId)
                .setParameter(2, customerId)
                .getResultList();

        List<User> userList = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            User user = new User();
            user.setUserId((String) results.get(i)[0]);
            if (results.get(i)[1] != null) {
                user.setUserToken((String) results.get(i)[1]);
            }
            if (results.get(i)[2] != null) {
                user.setFirstname((String) results.get(i)[2]);
            }
            if (results.get(i)[3] != null) {
                user.setLastname((String) results.get(i)[3]);
            }
            if (results.get(i)[4] != null) {
                user.setPhone((String) results.get(i)[4]);
            }
            if (results.get(i)[5] != null) {
                user.setEmail((String) results.get(i)[5]);
            }
            if (results.get(i)[8] != null) {
                user.setMobile((String) results.get(i)[8]);
            }
            if (results.get(i)[12] != null) {
                user.setJobTitle((String) results.get(i)[12]);
            }
            if (results.get(i)[22] != null) {
                user.setPartnerName((String) results.get(i)[22]);
            }
            if (results.get(i)[23] != null) {
                user.setPartnerId((String) results.get(i)[23]);
            }
            if (results.get(i)[24] != null) {
                user.setPartnerType((String) results.get(i)[24]);
            }
            userList.add(user);
        }
        return userList;
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
            //company.setUserList(new ArrayList<>());
            company.setContractList(new ArrayList<>());
            company.setInvoiceList(new ArrayList<>());
            company.setUserRoleList(new ArrayList<>());
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
        for (User user : resultList) {
            user.defaultCompanyName = user.getCompanyName();
        }
        return resultList;
    }

    @GET
    @Path("loadByCompanyWithSubsidiaries/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByCompanyWithSubsidiaries(@PathParam("companyId") String companyId) {
        List<User> companyUsers = loadByCompanyAll(companyId);
        Collections.sort(companyUsers, Comparator.comparing(User::getLastNameFirst));
        for (User user : companyUsers) {
            user.defaultCompanyName = user.getCompanyName();
        }
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<User> resultList = (List<User>) em.createNativeQuery(
                        "SELECT u.* FROM contract con\n" +
                                "JOIN company c on c.company_id = con.partner\n" +
                                "JOIN company_has_user chu on chu.company = c.company_id\n" +
                                "JOIN user u on u.user_id = chu.user\n" +
                                "WHERE con.company = ?1 AND con.contract_type = 'SUBSIDIARY'",
                        User.class)
                .setParameter(1, companyId)
                .getResultList();
        em.close();
        for (User user : resultList) {
            user.defaultCompanyName = user.getCompanyName();
        }
        Collections.sort(resultList, Comparator.comparing(User::getLastNameFirst));
        companyUsers.addAll(resultList);
        return companyUsers;
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
            if(opplysningen1881Result.getContacts() != null) {
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
                        if (resultItem.getGeography().getAddress() != null) {
                            user.setFormattedAddress(resultItem.getGeography().getAddress().getAddressString());
                        }
                    }
                    if (!user.getFirstname().isEmpty() || !user.getLastname().isEmpty()) {
                        resultList.add(user);
                    }
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
