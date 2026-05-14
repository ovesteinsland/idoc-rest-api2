
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
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@SuppressWarnings("LanguageDetectionInspection")
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.user")
@RolesAllowed({"ApplicationRole"})
public class UserFacadeREST extends AbstractFacade<User> {

    private static UserFacadeREST instance;


    public UserFacadeREST() {
        super(User.class);
        instance = this;
    }

    public static UserFacadeREST getInstance() {
        if (instance == null) {
            instance = new UserFacadeREST();
        }
        return instance;
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sql = """
                SELECT COUNT(*) FROM user
                WHERE login_name = ?1
                """;
            Number count = (Number) em.createNativeQuery(sql)
                    .setParameter(1, loginName)
                    .getSingleResult();

            long counter = count.longValue();
            if (counter > 0) {
                counter = 1L;
            }
            return String.valueOf(counter);
        } catch (Exception e) {
            System.out.println("Feil ved telling av brukere med login navn: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke telle brukere med login navn", e);
        }
    }

    @POST
    @Path("signupUser")
    @Consumes({MediaType.APPLICATION_JSON})
    @PermitAll
    @RateLimit(requests = 10, seconds = 60)
    public User signupUser(CustomerData entity) {

        Company authority = new Company();
        authority.setCompanyId(UUID.randomUUID().toString());
        authority.setName(entity.getCompany());
        authority.setCompanyType("AUTHORITY");
        authority.setIsLiteAccount(true);
        authority.setDefaultLanguageCode(entity.getLanguageCode());
        CompanyFacadeREST.getInstance().create(authority);

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

        linkToCompany(authority.getCompanyId(), user.getUserId());

        Company customer = new Company();
        customer.setCompanyId(UUID.randomUUID().toString());
        customer.setName("Generic Customer");
        customer.setCompanyType("OWNER");
        customer.setIsLiteAccount(true);
        authority.setDefaultLanguageCode(entity.getLanguageCode());
        CompanyFacadeREST.getInstance().create(customer);

        Company softwareControl = CompanyFacadeREST.getInstance().find("a84bdb6d-6f4b-4116-985b-6418ade5e957");
        Contract authorityContract = new Contract();
        authorityContract.setContractId(UUID.randomUUID().toString());
        authorityContract.setPartner(authority);
        authorityContract.setContractType("CUSTOMER");
        authorityContract.setCompany(softwareControl);
        ContractFacadeREST.getInstance().create(authorityContract);
        System.out.println("--------------------------------------------");
        System.out.println("Authority: ContractId = " + authorityContract.getContractId());

        Contract contract = new Contract();
        contract.setContractId(UUID.randomUUID().toString());
        contract.setPartner(customer);
        contract.setContractType("CUSTOMER");
        contract.setCompany(authority);
        ContractFacadeREST.getInstance().create(contract);

        // Link company to disipline
        DisiplineFacadeREST.getInstance().linkToCompany(authority.getCompanyId(), "7e445a7a-ce06-474c-93ff-d718a8c8cfe70"); //

        // Link company to language
        List<Language> languages = LanguageFacadeRest.getInstance().findAll();
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
        LanguageFacadeRest.getInstance().linkToCompany(authority.getCompanyId(), language.getLanguageId());

        AssetGroup assetGroup = new AssetGroup();
        assetGroup.setAssetGroupId(UUID.randomUUID().toString());
        assetGroup.setName("Asset Group");
        AssetGroupFacadeREST.getInstance().create(assetGroup);
        AssetGroupFacadeREST.getInstance().linkToCompany(authority.getCompanyId(), assetGroup);
        AssetGroupFacadeREST.getInstance().linkToCompany(customer.getCompanyId(), assetGroup);

        Asset asset = new Asset();
        asset.setAssetId(UUID.randomUUID().toString());
        asset.setAssetGroup(assetGroup);
        asset.setName("Default Asset");
        AssetFacadeREST.getInstance().create(asset);
        AssetFacadeREST.getInstance().linkCompany(authority.getCompanyId(), asset);
        AssetFacadeREST.getInstance().linkCompany(customer.getCompanyId(), asset);

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
            user.setPreferenceCompose(entity.getPreferenceCompose());
            user.setSub(entity.getSub());
            if (!entity.getIntegrationList().isEmpty()) {
                user.setIntegrationList(entity.getIntegrationList());
            }
            super.edit(user);

            for (Certificate certificate : user.getCertificateList()) {
                CertificateFacadeREST.getInstance().edit(certificate.getCertificateId(), certificate);
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

    private User optimizeLoginUser(User user) {
        user.setProjectList(new ArrayList<>());
        user.setObservationList(new ArrayList<>());

        if (!user.getCompanyList().isEmpty()) {
            List<Company> employerList = new ArrayList<>();
            for(Company employer: user.getCompanyList()) {
                List<User> cleanUserList = new ArrayList<>();
                for(User employerUser: employer.getUserList()) {
                    User cleanUser = new User();
                    cleanUser.setUserId(employerUser.getUserId());
                    cleanUser.setFirstname(employerUser.getFirstname());
                    cleanUser.setLastname(employerUser.getLastname());
                    cleanUser.setEmail(employerUser.getEmail());
                    cleanUser.setPhone(employerUser.getPhone());
                    cleanUser.setMobile(employerUser.getMobile());
                    cleanUser.setJobTitle(employerUser.getJobTitle());
                    cleanUser.setImageList(employerUser.getImageList());
                    cleanUser.setLoginName(employerUser.getLoginName());
                    // Alle lister er null/tomme på det nye objektet — ingen sirkel
                    cleanUserList.add(cleanUser);
                }
                employer.setUserList(cleanUserList);
                //employer.setDisiplineList(new ArrayList<>());
                for(Disipline disipline: employer.getDisiplineList()) {
                    disipline.setEquipmentTypeList(new ArrayList<>());
                }
                employer.setReportList(new ArrayList<>());
                employer.setProjectList(new ArrayList<>());
                employer.setAssetList(new ArrayList<>());
                employer.setAssetGroupList(new ArrayList<>());
                employer.setContractList(new ArrayList<>());
                employer.setInvoiceList(new ArrayList<>());
                //employer.setLanguageList(new ArrayList<>());
                employer.setUserRoleList(new ArrayList<>());
                employerList.add(employer);
            }
            user.setCompanyList(new ArrayList<>());
            user.setEmployerList(employerList);
        }
        return user;
    }

    @GET
    @Path("loadWithEmployers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public User loadWithEmployers(@PathParam("id") String id) {
        User user = super.find(id);
        return optimizeLoginUser(user);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Query query = em.createNamedQuery("User.findByLoginName");
            query.setParameter("loginName", id);
            List<User> users = query.getResultList();
            return users;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter bruker med login navn: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke finne bruker med login navn", e);
        }
    }



    @GET
    @Path("findContacts/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserContact> findContacts(@PathParam("id") String id) {
        User user = find(id);
        List<UserContact> contacts = new ArrayList<>(user.getContactList());
        return contacts;
    }

    @Deprecated // Can be replaced with authenticateUser for clients that expect one user ONLY
    @POST
    @Path("authenticateWithCredentials")
    @PermitAll
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> authenticateWithCredentials(IDocCredentials iDocCredentials) {
        String username = iDocCredentials.getUsername();
        String password = iDocCredentials.getPassword();
        List<User> users = authenticate(username, password);

        for(User user: users) {
            user.setEmployerList(new ArrayList<>());
        }
        return users;
    }

    @POST
    @Path("authenticateUser")
    @PermitAll
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public User authenticateUser(IDocCredentials iDocCredentials) {
        String username = iDocCredentials.getUsername();
        String password = iDocCredentials.getPassword();
        List<User> users =  authenticate(username, password);
        if(!users.isEmpty()) {
            User user = users.get(0);
            return optimizeLoginUser(user);
        } else {
            return null;
        }
    }

    @GET
    @Path("authenticate/{username}/{password}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> authenticate(@PathParam("username") String username, @PathParam("password") String password) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sqlString = """
                SELECT * FROM user
                WHERE login_name = ?1
                """;

            List<User> results = (List<User>) em.createNativeQuery(sqlString, User.class)
                    .setParameter(1, username)
                    .getResultList();

            if (!results.isEmpty()) {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
                String hashedPassword = results.get(0).getPassword();
                if (hashedPassword != null) {
                    if (!passwordAuthentication.authenticate(password.toCharArray(), hashedPassword)) {
                        results = new ArrayList<>();
                    }
                } else {
                    results = new ArrayList<>();
                }
            }
            return results;
        } catch (Exception e) {
            System.out.println("Feil ved autentisering av bruker: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke autentisere bruker", e);
        }
    }



    @GET
    @Path("authenticateWithEncoding/{loginName}/{password}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> authenticateWithEncoding(@PathParam("loginName") String loginName, @PathParam("password") String password) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String decodedLoginName = URLDecoder.decode(loginName, StandardCharsets.UTF_8.toString());
            List<User> results = new ArrayList<>();
            User user = findByLoginNameNative(decodedLoginName);

            if (user != null) {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
                String hashedPassword = user.getPassword();
                System.out.println("hashedPassword = " + hashedPassword);
                String decodedPassword = URLDecoder.decode(password, StandardCharsets.UTF_8.toString());

                if (passwordAuthentication.authenticate(decodedPassword.toCharArray(), hashedPassword)) {
                    results.add(user);
                }
            } else {
                System.out.println("results = EMPTY");
            }
            return results;
        } catch (UnsupportedEncodingException e) {
            System.out.println("Feil ved dekoding av innloggingsinformasjon: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke dekode innloggingsinformasjon", e);
        } catch (Exception e) {
            System.out.println("Feil ved autentisering med encoding: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke autentisere bruker med encoding", e);
        }
    }

    public User findByLoginNameNative(String login) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user u
                WHERE u.login_name = ?1
                """, User.class)
                    .setParameter(1, login)
                    .getResultList();

            if (resultList.isEmpty()) {
                return null;
            } else {
                return resultList.get(0);
            }
        } catch (Exception e) {
            System.out.println("Feil ved søk etter bruker med login navn (native): " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke finne bruker med login navn (native)", e);
        }
    }

    @GET
    @Path("searchUsers/{searchString}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> searchUsers(@PathParam("searchString") String searchString) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            searchString = "%" + searchString + "%";
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user
                WHERE (CONCAT(firstname, ' ', lastname) LIKE ?1
                   OR CONCAT(lastname, ' ', firstname) LIKE ?1)
                  AND deleted = 0
                ORDER BY lastname, firstname ASC
                """, User.class)
                    .setParameter(1, searchString)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter brukere: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke etter brukere", e);
        }
    }

    @GET
    @Path("usercompanies/{userId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Company> findUserCompanies(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = (List<Company>) em.createNativeQuery("""
                SELECT c.* FROM company c
                JOIN company_has_user chu ON chu.company = c.company_id
                JOIN user u ON chu.user = u.user_id
                WHERE u.user_id = ?1
                """, Company.class)
                    .setParameter(1, userId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved henting av brukerens selskaper: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke hente brukerens selskaper", e);
        }
    }

    @Deprecated
    @GET
    @Path("loadUserCompaniesOptimized/{userId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Company> loadUserCompaniesOptimized(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = (List<Company>) em.createNativeQuery("""
                SELECT c.* FROM company c
                JOIN company_has_user chu ON chu.company = c.company_id
                JOIN user u ON chu.user = u.user_id
                WHERE u.user_id = ?1
                """, Company.class)
                    .setParameter(1, userId)
                    .getResultList();

            for (Company company : resultList) {
                company.setProjectList(new ArrayList<>());
                company.setAssetList(new ArrayList<>());
                company.setUserList(new ArrayList<>());
                for (Disipline disipline : company.getDisiplineList()) {
                    disipline.setCompanyList(new ArrayList<>());
                }
                for (AssetGroup assetGroup : company.getAssetGroupList()) {
                    assetGroup.setAssetList(new ArrayList<>());
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved henting av brukerens selskaper (optimalisert): " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke hente brukerens selskaper (optimalisert)", e);
        }
    }

    @GET
    @Path("loadPartnerUsers/{authorityId}/{customerId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadPartnerUsers(@PathParam("authorityId") String authorityId, @PathParam("customerId") String customerId) {
        String sqlQuery = """
            SELECT
                u.*,
                concat(partner.name,partner.lastname,' ',partner.firstname),
                partner.company_id as companyId,
                authCon.contract_type as contractType
            FROM company authority
            JOIN contract authCon ON authority.company_id = authCon.company
            JOIN company partner ON authCon.partner = partner.company_id
            LEFT JOIN contract customerCon ON partner.company_id = customerCon.company and customerCon.contract_type = 'SUBSIDIARY'
            LEFT JOIN company subsidiary ON customerCon.partner = subsidiary.company_id
            LEFT JOIN company_has_user shu
                   ON subsidiary.company_id = shu.company
                   OR partner.company_id = shu.company
            LEFT JOIN user u ON shu.user = u.user_id
            WHERE
                authority.company_id = ?1
              AND (partner.company_id = ?2
                   OR authCon.contract_type = 'ENTREPRENEUR')
            GROUP BY shu.user
            """;

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Object[]> results = em.createNativeQuery(sqlQuery)
                    .setParameter(1, authorityId)
                    .setParameter(2, customerId)
                    .getResultList();

            List<User> userList = new ArrayList<>();
            for (Object[] result : results) {
                User user = new User();
                user.setUserId((String) result[0]);
                if (result[1] != null) {
                    user.setUserToken((String) result[1]);
                }
                if (result[2] != null) {
                    user.setFirstname((String) result[2]);
                }
                if (result[3] != null) {
                    user.setLastname((String) result[3]);
                }
                if (result[4] != null) {
                    user.setPhone((String) result[4]);
                }
                if (result[5] != null) {
                    user.setEmail((String) result[5]);
                }
                if (result[8] != null) {
                    user.setMobile((String) result[8]);
                }
                if (result[12] != null) {
                    user.setJobTitle((String) result[12]);
                }
                if (result[23] != null) {
                    user.setPartnerName((String) result[23]);
                }
                if (result[24] != null) {
                    user.setPartnerId((String) result[24]);
                }
                if (result[25] != null) {
                    user.setPartnerType((String) result[25]);
                }

                userList.add(user);
            }
            return userList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av partnerbrukere: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste partnerbrukere", e);
        }
    }

    @GET
    @Path("loadUserCompaniesOptimized2/{userId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Company> loadUserCompaniesOptimized2(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = (List<Company>) em.createNativeQuery("""
                SELECT DISTINCT c.* FROM company c
                    JOIN company_has_user chu ON chu.company = c.company_id
                    JOIN user u ON chu.user = u.user_id
                    LEFT JOIN company_has_disipline chd on chd.company_company_id = c.company_id
                    LEFT JOIN disipline d on chd.disipline_disipline_id = d.disipline_id
                    LEFT JOIN disipline_has_deviation_grade dhdg ON dhdg.disipline_disipline_id = d.disipline_id
                    LEFT JOIN deviation_grade dg ON dhdg.deviation_grade_deviation_grade_id = dg.deviation_grade_id
                    LEFT JOIN deviation_grade_language dgl ON dgl.deviation_grade = dg.deviation_grade_id
                WHERE u.user_id = ?1
                """, Company.class)
                    .setParameter(1, userId)
                    .getResultList();

            for (Company company : resultList) {
                // Hent gyldige språkkoder fra selskapets språkliste
                Set<String> validLanguageCodes = company.getLanguageList().stream()
                        .map(Language::getLanguageCode)
                        .collect(Collectors.toSet());

                company.setProjectList(new ArrayList<>());
                company.setAssetList(new ArrayList<>());
                company.setContractList(new ArrayList<>());
                company.setInvoiceList(new ArrayList<>());
                company.setUserRoleList(new ArrayList<>());
                for (Disipline disipline : company.getDisiplineList()) {
                    disipline.setCompanyList(new ArrayList<>());

                    // MÅ OPTIMALISERES
                    //disipline.setEquipmentTypeList(new ArrayList<>()); // Added 29/3-2026 for optimizing purpose

                    List<DisiplineLanguage> filteredDisiplineLanguages = disipline.getDisiplineLanguageList().stream()
                            .filter(dl -> validLanguageCodes.contains(dl.getLanguageCode()))
                            .toList();
                    disipline.setDisiplineLanguageList(filteredDisiplineLanguages);
                    // Filtrer deviation_grade_language for hver deviation_grade
                    for (DeviationGrade deviationGrade : disipline.getDeviationGradeList()) {
                        List<DeviationGradeLanguage> filteredLanguages = deviationGrade.getDeviationGradeLanguageList().stream()
                                .filter(dgl -> validLanguageCodes.contains(dgl.getLanguageCode()))
                                .collect(Collectors.toList());
                        deviationGrade.setDeviationGradeLanguageList(filteredLanguages);
                    }
                }
                company.setReportList(new ArrayList<>());
                for (AssetGroup assetGroup : company.getAssetGroupList()) {
                    assetGroup.setAssetList(new ArrayList<>());
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved henting av brukerens selskaper (optimalisert 2): " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke hente brukerens selskaper (optimalisert 2)", e);
        }
    }


    @GET
    @Path("loadByCompanyAll/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadByCompanyAll(@PathParam("companyId") String companyId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user u
                JOIN company_has_user chu ON chu.user = u.user_id
                WHERE chu.company = ?1
                """, User.class)
                    .setParameter(1, companyId)
                    .getResultList();

            for (User user : resultList) {
                user.defaultCompanyName = user.getCompanyName();
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av alle brukere for selskap: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste alle brukere for selskap", e);
        }
    }

    @GET
    @Path("loadByCompanyWithSubsidiaries/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadByCompanyWithSubsidiaries(@PathParam("companyId") String companyId) {
        try {
            List<User> companyUsers = loadByCompanyAll(companyId);
            Collections.sort(companyUsers, Comparator.comparing(User::getLastNameFirst));
            for (User user : companyUsers) {
                user.defaultCompanyName = user.getCompanyName();
            }

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<User> resultList = (List<User>) em.createNativeQuery("""
                    SELECT u.* FROM contract con
                    JOIN company c ON c.company_id = con.partner
                    JOIN company_has_user chu ON chu.company = c.company_id
                    JOIN user u ON u.user_id = chu.user
                    WHERE con.company = ?1 AND con.contract_type = 'SUBSIDIARY'
                    """, User.class)
                        .setParameter(1, companyId)
                        .getResultList();

                for (User user : resultList) {
                    user.defaultCompanyName = user.getCompanyName();
                    user.setPartnerName(user.getCompanyName());
                }
                resultList.sort(Comparator.comparing(User::getLastNameFirst));
                companyUsers.addAll(resultList);
                return companyUsers;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av brukere for selskap med datterselskaper: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste brukere for selskap med datterselskaper", e);
        }
    }

    @GET
    @Path("loadByCompany/{companyId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> loadByCompany(@PathParam("companyId") String companyId, @PathParam("authorityId") String authorityId) {
        List<User> resultList = loadByCompanyAll(companyId);
        return resultList;
    }

    @GET
    @Path("loadByMobile/{mobile}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadByMobile(@PathParam("mobile") String mobile) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user u
                WHERE u.mobile = ?1
                """, User.class)
                    .setParameter(1, mobile)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av brukere etter mobilnummer: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste brukere etter mobilnummer", e);
        }
    }

    @GET
    @Path("loadByMobileAndEmail/{mobile}/{email}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadByMobile(@PathParam("mobile") String mobile, @PathParam("email") String email) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user u
                WHERE u.mobile = ?1 AND u.email = ?2
                """, User.class)
                    .setParameter(1, mobile)
                    .setParameter(2, email)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av brukere etter mobilnummer og e-post: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste brukere etter mobilnummer og e-post", e);
        }
    }

    @GET
    @Path("loadByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<User> loadByProject(@PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<User> resultList = (List<User>) em.createNativeQuery("""
                SELECT * FROM user u
                JOIN project_has_user phu ON phu.user_user_id = u.user_id
                JOIN project p ON phu.project_project_id = p.project_id
                WHERE p.project_id = ?1
                """, User.class)
                    .setParameter(1, projectId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av brukere for prosjekt: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste brukere for prosjekt", e);
        }
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                    SELECT COUNT(*) FROM company_has_user
                    WHERE company = ?1 AND user = ?2
                    """)
                        .setParameter(1, companyId)
                        .setParameter(2, userId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    em.createNativeQuery("""
                        INSERT INTO company_has_user (company, user)
                        VALUES (?, ?)
                        """)
                            .setParameter(1, companyId)
                            .setParameter(2, userId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Advarsel ved linking av bruker til selskap (kan allerede eksistere): " + exp.getMessage());
                // Aksepterer at linken kan eksistere fra før - ingen exception kastes
            }
        } catch (Exception e) {
            System.out.println("Feil ved opprettelse av EntityManager: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke linke bruker til selskap", e);
        }
    }

    @PUT
    @Path("linkToCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId, User entity) {
        User user = this.find(entity.getUserId());
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        if (user != null && company != null) {
            if (!user.getCompanyList().contains(company)) {
                user.getCompanyList().add(company);
                this.edit(user);
            }
            if (!company.getUserList().contains(user)) {
                company.getUserList().add(user);
                CompanyFacadeREST.getInstance().edit(company);
            }
        }
    }

    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("companyId") String companyId, User entity) {
        User user = this.find(entity.getUserId());
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        if (user != null && company != null) {
            CompanyFacadeREST.getInstance().removeUser(company, user);
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
        Media media = ImageFacadeREST.getInstance().find(mediaId);
        if (user != null && media != null) {

            if (user.getImageList().contains(media)) {
                user.getCompanyList().remove(media);
                //this.edit(user);
            }
            ImageFacadeREST.getInstance().remove(media.getMediaId());
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
