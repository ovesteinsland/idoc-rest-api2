/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityhelper.*;
import no.softwarecontrol.idoc.data.entityjson.CompanyLite;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.entityobject.query.ResultItem;
import no.softwarecontrol.idoc.data.requestparams.ProjectRequestParameters;
import no.softwarecontrol.idoc.restclient.brreg.BrregResult;
import no.softwarecontrol.idoc.restclient.brreg.Enhet;
import no.softwarecontrol.idoc.web.signup.SignupTask;
import no.softwarecontrol.idoc.webservices.brreg.BrregJsonClient;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.company")
@RolesAllowed({"ApplicationRole"})
public class CompanyFacadeREST extends AbstractFacade<Company> {

    public static CompanyFacadeREST instance;

    public CompanyFacadeREST() {
        super(Company.class);
        instance = this;
    }

    public static CompanyFacadeREST getInstance() {
        if (instance == null) {
            instance = new CompanyFacadeREST();
        }
        return instance;
    }

    private static final String LICENCE = "1.0";

    @Override
    protected String getSelectAllQuery() {
        return "Company.findAll";
    }

    @POST
    @Path("signupCustomer")
    @Consumes({MediaType.APPLICATION_JSON})
    public void signupCustomer(CustomerData customerData) {
        SignupTask signupTask = new SignupTask(customerData);
        signupTask.execute();
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Company entity) {
        if (entity.getIsLiteAccount() == null) {
            entity.setIsLiteAccount(false);
        }
        super.create(entity);
    }

    @Deprecated
    @POST
    @Path("createWithContract/{companyId}/{contractType}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithContract(@PathParam("companyId") String companyId, @PathParam("contractType") String contractType, Company partner) {
        // Check if partner exist in iDoc already
        /*List<Company> existingCompanies = queryByOrganizationNumber(partner.getOrganizationNumber());
        if (existingCompanies.isEmpty()) {
            super.create(partner);
        } else {
            partner = existingCompanies.get(0);
        }*/

        super.create(partner);
        Company company = this.find(companyId);
        Contract contract = new Contract(UUID.randomUUID().toString());
        contract.setCompany(company);
        contract.setPartner(partner);
        contract.setContractType(contractType);
        ContractFacadeREST.getInstance().create(contract);
        partner.getPartnerContracts().add(contract);
        company.getContractList().add(contract);
        edit(company);
        edit(partner);
        ContractFacadeREST.getInstance().edit(contract);
    }

    @POST
    @Path("createWithContract2/{companyId}/{contractType}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Company createWithContract2(@PathParam("companyId") String companyId, @PathParam("contractType") String contractType, Company partner) {

        // Check if partner exist in iDoc already
        boolean isPartnerAlready = false;
        if (partner.getIsLiteAccount() == null) {
            partner.setIsLiteAccount(false);
        }
        List<Company> existingCompanies = new ArrayList<>();
        if (!partner.getOrganizationNumber().isEmpty()) {
            existingCompanies = queryByOrganizationNumber(partner.getOrganizationNumber());
        }

        Company company = this.find(companyId);
        if (existingCompanies.isEmpty()) {
            String groupName = "Bygninger";
            AssetGroup assetGroup = new AssetGroup();
            assetGroup.setAssetGroupId(UUID.randomUUID().toString());
            assetGroup.setName(groupName);
            partner.setCreatedDate(new Date());
            super.create(partner);

            AssetGroupFacadeREST.getInstance().create(assetGroup);
            partner.getAssetGroupList().add(assetGroup);
            AssetGroupFacadeREST.getInstance().linkToCompany(partner.getCompanyId(), assetGroup);

        } else {
            List<Contract> partnerContracts = existingCompanies.get(0).getPartnerContracts();
            for (Contract existingContract : partnerContracts) {
                if (existingContract.getCompany().equals(company)) {
                    isPartnerAlready = true;
                }
            }
            existingCompanies.get(0).getUserList().addAll(partner.getUserList());
            partner = existingCompanies.get(0);
        }
        if (!isPartnerAlready) {
            Contract contract = new Contract(UUID.randomUUID().toString());
            contract.setCompany(company);
            contract.setPartner(partner);
            contract.setContractType(contractType);
            ContractFacadeREST.getInstance().create(contract);
            partner.getPartnerContracts().add(contract);
            company.getContractList().add(contract);
            ContractFacadeREST.getInstance().edit(contract);
        }
        return partner;
    }


    private boolean requiresAssetGroup() {
        return true;
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Company entity) {
        Company company = this.find(id);
        if (company != null) {
            company.setAddressList(entity.getAddressList());
            for (Address address : company.getAddressList()) {
                if (!address.getCompanyList().contains(company)) {
                    address.getCompanyList().add(company);
                }
            }
            company.setIntegrationList(entity.getIntegrationList());
            company.setDeleted(entity.isDeleted());
            company.setOrganizationNumber(entity.getOrganizationNumber());
            company.setCompanyType(entity.getCompanyType());
            company.setName(entity.getName());
            company.setFirstname(entity.getFirstname());
            company.setLastname(entity.getLastname());
            company.setPhone(entity.getPhone());
            company.setEmail(entity.getEmail());
            company.setWebSite(entity.getWebSite());
            if (entity.getProjectCounter() > 0) {
                company.setProjectCounter(entity.getProjectCounter());
            }
            company.setIsPersonCustomer(entity.getIsPersonCustomer());
        }
        super.edit(company);
    }


    public void removeUser(Company company, User user) {
        if (company.getUserList().contains(user)) {
            company.getUserList().remove(user);
            super.edit(company);
        }
    }

    public void editInternal(Company entity) {
        //super.edit(entity);
        Company company = this.find(entity.getCompanyId());
        if (company != null) {
            company.setAddressList(entity.getAddressList());
            for (Address address : company.getAddressList()) {
                if (!address.getCompanyList().contains(company)) {
                    address.getCompanyList().add(company);
                }
            }
            for (Invoice invoice : entity.getInvoiceList()) {
                if (!company.getInvoiceList().contains(invoice)) {
                    company.getInvoiceList().add(invoice);
                }
            }
            company.setDeleted(entity.isDeleted());
            company.setOrganizationNumber(entity.getOrganizationNumber());
            company.setCompanyType(entity.getCompanyType());
            company.setName(entity.getName());
            company.setFirstname(entity.getFirstname());
            company.setLastname(entity.getLastname());
            company.setPhone(entity.getPhone());
            if (entity.getProjectCounter() > 0) {
                company.setProjectCounter(entity.getProjectCounter());
            }
            company.setIsPersonCustomer(entity.getIsPersonCustomer());
        }
        super.edit(company);
    }

    @PUT
    @Path("linkToDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToDisipline(@PathParam("disiplineId") String disiplineId, Company entity) {
        Company company = this.find(entity.getCompanyId());
        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
        if (disipline != null && company != null) {
            if (!disipline.getCompanyList().contains(company)) {
                disipline.getCompanyList().add(company);
                DisiplineFacadeREST.getInstance().edit(disipline);
            }
            if (!company.getDisiplineList().contains(disipline)) {
                company.getDisiplineList().add(disipline);
                this.edit(company);
            }
        }
    }

    @PUT
    @Path("unlinkDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkDisipline(@PathParam("disiplineId") String disiplineId, Company entity) {
        Company company = this.find(entity.getCompanyId());
        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
        if (disipline != null && company != null) {
            if (disipline.getCompanyList().contains(company)) {
                disipline.getCompanyList().remove(company);
                DisiplineFacadeREST.getInstance().edit(disipline);
            }
            if (company.getDisiplineList().contains(disipline)) {
                company.getDisiplineList().remove(disipline);
                this.edit(company);
            }
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Company find(@PathParam("id") String id) {
        return findNative(id);
    }

    @GET
    @Path("findOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Company findOptimized(@PathParam("id") String id) {
        Company company = findNative(id);
        //Company temp = company.duplicate();

        List<AssetGroup> tempAssetGroups = new ArrayList<>();
        for (AssetGroup assetGroup : company.getAssetGroupList()) {
            AssetGroup tempAssetGroup = assetGroup.duplicate();
            List<Asset> tempList = assetGroup.getAssetList().subList(0, 0);
            tempAssetGroup.setAssetList(tempList);
            tempAssetGroups.add(tempAssetGroup);
        }
        company.setAssetGroupList(tempAssetGroups);
        company.setAssetList(new ArrayList<>());
        company.setUserList(new ArrayList<>());
        company.setProjectList(new ArrayList<>());
        //company.setReportList(new ArrayList<>());
        company.setContractList(new ArrayList<>());
        company.setInvoiceList(new ArrayList<>());
        for (Disipline disipline : company.getDisiplineList()) {
            disipline.setEquipmentTypeList(new ArrayList<>());
            disipline.setParameterList(new ArrayList<>());

        }
        //temp.getAssetList().clear();
        //temp.getUserList().clear();

        return company;
    }

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Company> findAll() {
//        return super.findAll();
//    }


    //
    @GET
    @Path("loadResultItemsByAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> loadResultItemsByAuthority(@PathParam("companyid") String id) {
        Company authority = find(id);
        List<ResultItem> results = new ArrayList<>();
        if (authority != null) {
            for (Contract contract : authority.getContractList()) {
                Company company = contract.getPartner();
                if (contract.getContractType().equals("CUSTOMER")) {
                    ResultItem item = new ResultItem();
                    item.setName(company.getName());
                    item.setDescription(company.getDescription());
                    item.setId(company.getCompanyId());
                    item.setClazz(Company.class.toString());
                    if (company.getImage() != null) {
                        item.setImage(company.getImage());
                    }
                    results.add(item);
                }
            }
        }
        return results;
    }

    public Company findNative(String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery(
                            "SELECT c.* " +
                                    "FROM company c " +
                                    "WHERE c.company_id = ?1",
                            Company.class)
                    .setParameter(1, id)
                    .getResultList();

            if (resultList.isEmpty()) {
                return null;
            }

            return resultList.get(0);
        } catch (Exception e) {
            System.out.println("Exception in findNative for Company ID: " + id);
            System.out.println("Error: " + e.getMessage());
            throw new InternalServerErrorException("Failed to find company: " + e.getMessage(), e);
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByIntegration(@PathParam("service") String service, @PathParam("primaryKey") String primaryKey) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery(
                            "SELECT c.* " +
                                    "FROM company c " +
                                    "JOIN company_has_integration chi ON chi.company_company_id = c.company_id " +
                                    "JOIN integration i ON i.integration_id = chi.integration_integration_id " +
                                    "WHERE i.keyy = 'PRIMARY_KEY' AND i.service = ?1 AND i.valuee = ?2",
                            Company.class)
                    .setParameter(1, service)
                    .setParameter(2, primaryKey)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByIntegration");
            System.out.println("Service: " + service + ", Primary key: " + primaryKey);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("incrementProjectCounter/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectNumber incrementProjectCounter(@PathParam("companyId") String companyId) {
        Company company = findNative(companyId);

        if (company == null) {
            System.out.println("Company not found for ID: " + companyId);
            ProjectNumber pn = new ProjectNumber();
            pn.setProjectCounter(0);
            return pn;
        }

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                company.setProjectCounter(company.getProjectCounter() + 1);
                int counter = company.getProjectCounter();

                tx.begin();
                em.createNativeQuery(
                                "UPDATE company SET project_counter = ? " +
                                        "WHERE company_id = ?")
                        .setParameter(1, counter)
                        .setParameter(2, company.getCompanyId())
                        .executeUpdate();
                tx.commit();

                ProjectNumber pn = new ProjectNumber();
                pn.setProjectCounter(counter);
                return pn;
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("------------------------------------------------");
                System.out.println("Exception while updating company.project_counter");
                System.out.println("Company ID: " + companyId);
                System.out.println("Error: " + e.getMessage());
                System.out.println("------------------------------------------------");

                ProjectNumber pn = new ProjectNumber();
                pn.setProjectCounter(0);
                return pn;
            }
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("licence")
    @Produces({MediaType.APPLICATION_JSON})
    public LicenceNumber licence() {
        LicenceNumber pn = new LicenceNumber();
        pn.setLicence(LICENCE);
        return pn;
    }


    @GET
    @Path("loadCompanyItem/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public CompanyItem loadCompanyItem(@PathParam("companyId") String companyId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<WalletCompany> resultList = em.createNativeQuery("""
                                    SELECT
                                        wallet_company.authority_id as authority_id,
                                        wallet_company.authority_name as authority_name,
                                        wallet_company.authority_address,
                                        wallet_company.demo_expire_date,
                                        wallet_company.disipline,
                                        SUM(wallet_company.least_counter) as point_counter,
                                        wallet_company.point_limit,
                                        wallet_company.package_price,
                                        wallet_company.package_discount,
                                        SUM(wallet_company.revenue * wallet_company.package_discount) as revenue,
                                        wallet_company.start_date as invoice_start_date,
                                        wallet_company.company_image
                                    FROM (
                                        SELECT
                                            authority.company_id as authority_id,
                                            authority.name as authority_name,
                                            authority.expire_date as demo_expire_date,
                                            d.name as disipline,
                                            p.name as project_name,
                                            p.disipline as project_disipline,
                                            p.created_date as project_date,
                                            d.max_children,
                                            d.point_price as point_factor,
                                            i.point_limit as point_limit,
                                            i.point_price,
                                            i.point_price as package_price,
                                            i.point_discount as package_discount,
                                            LEAST(COUNT(p.project_id), d.max_children) * d.point_price as least_counter,
                                            LEAST(COUNT(p.project_id), d.max_children) * d.point_price * i.point_price as revenue,
                                            i.start_date as start_date,
                                            (SELECT img.image_id
                                             FROM company
                                             JOIN company_has_image chi ON chi.company = company.company_id
                                             JOIN image img ON img.image_id = chi.image
                                             WHERE company.company_id = authority.company_id
                                             LIMIT 1) as company_image,
                                            (SELECT CONCAT(addr.address1, ', ', addr.zip_code, ' ', addr.city, ' - ', addr.country)
                                             FROM company
                                             JOIN company_has_address cha ON cha.company_company_id = company.company_id
                                             JOIN address addr ON addr.address_id = cha.address_address_id
                                             WHERE addr.zip_code != '' 
                                               AND cha.company_company_id = authority.company_id
                                             LIMIT 1) as authority_address
                                        FROM company as authority
                                        LEFT JOIN invoice i ON i.company = authority.company_id
                                        LEFT JOIN project p ON p.created_company = authority.company_id
                                        LEFT JOIN disipline d ON p.disipline = d.disipline_id
                                        LEFT JOIN project child ON child.parent = p.project_id
                                        WHERE authority.company_id = ?1
                                          AND i.end_date IS NULL
                                          AND p.parent IS NULL
                                          AND p.created_company = authority.company_id
                                          AND (p.deleted = 0 OR p.deleted IS NULL)
                                          AND (child.deleted = 0 OR child.deleted IS NULL)
                                          AND p.created_date > i.start_date
                                        GROUP BY authority.company_id, p.project_id, authority.name
                                        ORDER BY authority.name
                                    ) as wallet_company
                                    GROUP BY wallet_company.authority_id, wallet_company.authority_name
                                    ORDER BY wallet_company.authority_name
                                    """,
                            WalletCompany.class)
                    .setParameter(1, companyId)
                    .getResultList();

            if (!resultList.isEmpty()) {
                return new CompanyItem(resultList.get(0));
            }

            // Ingen prosjekter funnet - bygg fallback CompanyItem
            return buildFallbackCompanyItem(companyId);

        } catch (Exception e) {
            System.out.println("Exception in loadCompanyItem for Company ID: " + companyId);
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Returner fallback ved feil
            return buildFallbackCompanyItem(companyId);
        }
    }

    private CompanyItem buildFallbackCompanyItem(String companyId) {
        CompanyItem companyItem = new CompanyItem();
        companyItem.setPointCount(0.0);
        companyItem.setPointLimit(0.0);
        companyItem.setCompanyId(companyId);

        try {
            Company company = findNative(companyId);
            if (company != null) {
                companyItem.setName(company.getFullName());

                // Finn åpne fakturaer
                List<Invoice> openInvoices = company.getInvoiceList().stream()
                        .filter(r -> r.getEndDate() == null)
                        .collect(Collectors.toList());

                if (!openInvoices.isEmpty()) {
                    Invoice openInvoice = openInvoices.get(0);
                    companyItem.setPointLimit(openInvoice.getPointLimit());
                }
            }
        } catch (Exception e) {
            System.out.println("Error building fallback CompanyItem for ID: " + companyId);
            System.out.println("Error: " + e.getMessage());
        }

        return companyItem;
    }


    @PUT
    @Path("loadCompanyItems")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<CompanyItem> loadCompanyItems(ProjectRequestParameters parameters) {
        List<CompanyItem> companyItems = new ArrayList<>();
        return companyItems;
    }

    @GET
    @Path("loadAuthorityItems")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyItem> loadAuthorityItems() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<CompanyItem> companyItems = new ArrayList<>();

            // Hent alle AUTHORITY-selskaper
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    WHERE c.company_type = 'AUTHORITY'
                                    ORDER BY c.name
                                    """,
                            Company.class)
                    .getResultList();

            // Behandle hvert selskap
            for (Company company : resultList) {
                processCompany(company, companyItems, em);
            }

            return companyItems;
        } catch (Exception e) {
            System.out.println("Exception in loadAuthorityItems");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void processCompany(Company company, List<CompanyItem> companyItems, EntityManager em) {
        List<Invoice> invoices = company.getInvoiceList();
        if (invoices.isEmpty()) {
            return;
        }

        // Finn åpne fakturaer
        List<Invoice> openInvoices = invoices.stream()
                .filter(r -> r.getEndDate() == null)
                .collect(Collectors.toList());

        if (openInvoices.isEmpty()) {
            return;
        }

        Invoice invoice = openInvoices.get(0);

        // Beregn poeng
        Number counterUnassigned = calculateProjectPoints(company, invoice, em);

        // Opprett CompanyItem
        CompanyItem companyItem = new CompanyItem(company);
        companyItem.setPointLimit(invoice.getPointLimit());

        if (counterUnassigned != null) {
            Double counter = Double.parseDouble(counterUnassigned.toString());
            counter += invoice.getPointTransfered();
            companyItem.setPointCount(counter);
        } else {
            companyItem.setPointCount(0.0);
        }

        companyItems.add(companyItem);
    }

    private Number calculateProjectPoints(Company company, Invoice invoice, EntityManager em) {
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String strStartDate = dateFormatter.format(invoice.getStartDate());

            String counterQuery = """
                    SELECT SUM(ppC.projectCounter) as totalPoints
                    FROM (
                        SELECT
                            p.project_number as projectNumber,
                            (SELECT IF(customer.demo = 1, 0.0,
                                       (SELECT LEAST(GREATEST(COUNT(*), 1) * d.point_price, d.max_children * d.point_price)
                                        FROM project child
                                        WHERE child.parent = p.project_id
                                          AND (child.deleted = 0 OR child.deleted IS NULL)))
                             FROM company customer
                             JOIN company_has_project chp ON customer.company_id = chp.company_company_id
                             WHERE chp.project_project_id = p.project_id 
                               AND customer.company_type = 'OWNER' 
                             LIMIT 1) as projectCounter
                        FROM project p
                        JOIN disipline d ON p.disipline = d.disipline_id
                        WHERE p.created_date > ?1
                          AND p.created_company = ?2
                          AND (p.deleted = 0 OR p.deleted IS NULL)
                          AND p.parent IS NULL
                        ORDER BY p.project_number DESC
                    ) as ppC
                    """;

            return (Number) em.createNativeQuery(counterQuery)
                    .setParameter(1, strStartDate)
                    .setParameter(2, company.getCompanyId())
                    .getSingleResult();
        } catch (Exception e) {
            System.out.println("Error calculating project points for company: " + company.getCompanyId());
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    @GET
    @Path("getAuthorityIds")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> getAuthorityIds() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                            SELECT c.company_id
                            FROM company c
                            WHERE c.company_type = 'AUTHORITY'
                            """)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in getAuthorityIds");
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadCompanyItems/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyItem> loadCompanyItems(@PathParam("companyid") String id,
                                              @PathParam("batchOffset") String batchOffset,
                                              @PathParam("batchSize") String batchSize) {
        try {
            int offset = Integer.parseInt(batchOffset);
            int size = Integer.parseInt(batchSize);

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<WalletCompany> resultList = em.createNativeQuery("""
                                        SELECT
                                          c.company_id                                    AS authority_id,
                                          c.name                                          AS authority_name,
                                          c.expire_date                                   AS demo_expire_date,
                                          ''                                              AS disipline,
                                          COALESCE(pp.point_counter, 0.0)                 AS point_counter,
                                          ai.point_limit,
                                          ai.point_transfered,
                                          ai.start_date                                   AS invoice_start_date,
                                          (
                                            SELECT img.image_id
                                            FROM company co
                                            JOIN company_has_image chi ON chi.company = co.company_id
                                            JOIN image img ON img.image_id = chi.image
                                            WHERE co.company_id = c.company_id
                                            LIMIT 1
                                          )                                               AS company_image,
                                          (
                                            SELECT CONCAT(addr.address1, ', ', addr.zip_code, ' ', addr.city, ' - ', addr.country)
                                            FROM company co2
                                            JOIN company_has_address cha ON cha.company_company_id = co2.company_id
                                            JOIN address addr ON addr.address_id = cha.address_address_id
                                            WHERE addr.zip_code != ''
                                              AND cha.company_company_id = c.company_id
                                            LIMIT 1
                                          )                                               AS authority_address
                                        FROM company c
                                        JOIN (
                                          SELECT i.company,
                                                 i.start_date,
                                                 i.point_limit,
                                                 COALESCE(i.point_transfered, 0.0) AS point_transfered
                                          FROM invoice i
                                          WHERE i.end_date IS NULL
                                        ) ai ON ai.company = c.company_id
                                        LEFT JOIN (
                                          SELECT
                                            p.created_company AS company_id,
                                            SUM(
                                              LEAST(
                                                COALESCE(d.max_children, 1e9),
                                                GREATEST(1, COALESCE(child_counts.child_cnt, 0))
                                              ) * COALESCE(d.point_price, 0.0)
                                            ) AS point_counter
                                          FROM project p
                                          JOIN disipline d ON d.disipline_id = p.disipline
                                          LEFT JOIN (
                                            SELECT pc.parent AS root_project_id,
                                                   COUNT(*)  AS child_cnt
                                            FROM project pc
                                            WHERE pc.parent IS NOT NULL
                                              AND pc.deleted = 0
                                              AND pc.project_state <> 0
                                            GROUP BY pc.parent
                                          ) child_counts ON child_counts.root_project_id = p.project_id
                                          WHERE p.parent IS NULL
                                            AND p.deleted = 0
                                            AND p.project_state <> 0
                                            AND EXISTS (
                                              SELECT 1
                                              FROM invoice ai2
                                              WHERE ai2.company = p.created_company
                                                AND ai2.end_date IS NULL
                                                AND p.created_date >= ai2.start_date
                                                AND p.created_date <= CURRENT_TIMESTAMP
                                            )
                                          GROUP BY p.created_company
                                        ) pp ON pp.company_id = c.company_id
                                        WHERE c.company_type = 'AUTHORITY'
                                          AND c.name != ''
                                        ORDER BY c.name
                                        LIMIT ?1 OFFSET ?2
                                        """,
                                WalletCompany.class)
                        .setParameter(1, size)
                        .setParameter(2, offset)
                        .getResultList();

                return resultList.stream()
                        .map(CompanyItem::new)
                        .collect(Collectors.toList());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid batch parameters: offset=" + batchOffset + ", size=" + batchSize);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Exception in loadCompanyItems");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadAuthorities")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadAuthorities() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                                    SELECT *
                                    FROM company
                                    WHERE company_type = ?1
                                    """,
                            Company.class)
                    .setParameter(1, "AUTHORITY")
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadAuthorities");
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Double getPointCount(Project project) {
        Double counter = 0.0;
        if (project.getProjectList().isEmpty()) {
            counter = counter + project.getDisipline().getPointPrice();
        } else {
            List<Project> projects = new ArrayList<>(project.getProjectList());
            List<Project> activeChildren = projects.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
            if (activeChildren.size() >= project.getDisipline().getMaxChildren()) {
                counter = counter + (project.getDisipline().getMaxChildren() * project.getDisipline().getPointPrice());
            } else {
                for (Project child : activeChildren) {
                    counter = counter + project.getDisipline().getPointPrice();
                }
            }
        }
        return counter;
    }


    @GET
    @Path("loadByAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByAuthority(@PathParam("companyid") String id) {
        final long start = System.currentTimeMillis();
        Company authority = find(id);
        List<Company> results = new ArrayList<>();

        if (authority != null) {
            for (Contract contract : authority.getContractList()) {
                Company partner = contract.getPartner();
                if (!results.contains(partner)) {
                    results.add(partner);
                }
                Company company = contract.getCompany();
                if (!results.contains(company)) {
                    results.add(company);
                }
            }
            for (Contract contract : authority.getPartnerContracts()) {
                Company partner = contract.getPartner();
                if (!results.contains(partner)) {
                    results.add(partner);
                }
                Company company = contract.getCompany();
                if (!results.contains(company)) {
                    results.add(company);
                }
            }
        }
        final long end = System.currentTimeMillis();
        //return results.subList(0,10);
        return results;
    }

    @Deprecated
    @GET
    @Path("loadByAuthority2/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByAuthority2(@PathParam("companyid") String id,
                                          @PathParam("batchOffset") String batchOffset,
                                          @PathParam("batchSize") String batchSize) {
        try {
            int offset = Integer.parseInt(batchOffset);
            int size = Integer.parseInt(batchSize);

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                return em.createNativeQuery("""
                                        SELECT DISTINCT customer.*
                                        FROM company c
                                        JOIN contract cont ON c.company_id = cont.company
                                        JOIN company customer ON cont.partner = customer.company_id
                                        WHERE c.company_id = ?1
                                        ORDER BY CONCAT(customer.name, customer.lastname, ', ', customer.firstname) COLLATE utf8mb4_danish_ci ASC
                                        LIMIT ?2 OFFSET ?3
                                        """,
                                Company.class)
                        .setParameter(1, id)
                        .setParameter(2, size)
                        .setParameter(3, offset)
                        .getResultList();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid batch parameters for loadByAuthority2");
            System.out.println("Company ID: " + id + ", offset: " + batchOffset + ", size: " + batchSize);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Exception in loadByAuthority2 for Company ID: " + id);
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Deprecated
    @GET
    @Path("loadSubsidiaries/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadSubsidiaries(@PathParam("companyid") String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> companies = em.createNativeQuery("""
                                    SELECT DISTINCT customer.*
                                    FROM company c
                                    JOIN contract cont ON c.company_id = cont.company
                                    JOIN company customer ON cont.partner = customer.company_id
                                    WHERE cont.contract_type = 'SUBSIDIARY' 
                                      AND c.company_id = ?1
                                    ORDER BY CONCAT(customer.name, customer.lastname, ', ', customer.firstname) COLLATE utf8mb4_danish_ci ASC
                                    """,
                            Company.class)
                    .setParameter(1, id)
                    .getResultList();

            return companies.stream()
                    .map(this::optimizeCompany)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in loadSubsidiaries for Company ID: " + id);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    @GET
    @Path("loadByAsset/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByAsset(@PathParam("assetId") String assetId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    JOIN company_has_asset cha ON c.company_id = cha.company_company_id
                                    JOIN asset a ON a.asset_id = cha.asset_asset_id
                                    WHERE a.asset_id = ?1
                                    ORDER BY CONCAT(c.name, c.lastname, ', ', c.firstname) COLLATE utf8mb4_danish_ci ASC
                                    """,
                            Company.class)
                    .setParameter(1, assetId)
                    .getResultList();

            return resultList.stream()
                    .map(company -> {
                        Company optimized = optimizeCompany(company);
                        optimized.getDisiplineList().clear();
                        optimized.getInvoiceList().clear();
                        optimized.getReportList().clear();
                        optimized.getAssetGroupList().clear();
                        return optimized;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in loadByAsset for Asset ID: " + assetId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadByAuthorityOptimized/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByAuthorityOptimized(@PathParam("companyid") String id,
                                                  @PathParam("batchOffset") String batchOffset,
                                                  @PathParam("batchSize") String batchSize) {
        List<Company> optimizedCompanies = new ArrayList<>();
        List<Company> companies = loadByAuthority2(id, batchOffset, batchSize);
        for (Company company : companies) {
            optimizedCompanies.add(optimizeCompany(company));
        }
        return optimizedCompanies;
    }

    /**
     * Added 09. may 2026. for use in Compose
     * Optimized even more than loadByAuthorityOptimized.
     * Created a new version to avoid messing up old ios/flow.
     * Even if it should work there as well. But you never know
     * when messing up....
     * @param id
     * @param batchOffset
     * @param batchSize
     * @return
     */

    @GET
    @Path("loadByAuthorityOptimized2/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByAuthorityOptimized2(@PathParam("companyid") String id,
                                                   @PathParam("batchOffset") String batchOffset,
                                                   @PathParam("batchSize") String batchSize) {

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            int offset = Integer.parseInt(batchOffset);
            int size = Integer.parseInt(batchSize);

            // 1) Hent kun de feltene vi trenger fra company-tabellen
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("""
                            SELECT DISTINCT
                                customer.company_id,
                                customer.name,
                                customer.firstname,
                                customer.lastname,
                                customer.organization_number,
                                customer.phone,
                                customer.email,
                                customer.web_site,
                                customer.company_type,
                                customer.default_language_code,
                                customer.is_person_customer,
                                customer.is_lite_account,
                                customer.is_free_account,
                                customer.demo,
                                customer.deleted,
                                customer.project_counter,
                                customer.created_date,
                                customer.expire_date
                            FROM company c
                            JOIN contract cont ON c.company_id = cont.company
                            JOIN company customer ON cont.partner = customer.company_id
                            WHERE c.company_id = ?1
                            ORDER BY CONCAT(customer.name, customer.lastname, ', ', customer.firstname) COLLATE utf8mb4_danish_ci ASC
                            LIMIT ?2 OFFSET ?3
                            """)
                    .setParameter(1, id)
                    .setParameter(2, size)
                    .setParameter(3, offset)
                    .getResultList();

            if (rows.isEmpty()) {
                return new ArrayList<>();
            }

            // 2) Bygg lette Company-objekter (uten å gå via persistence context)
            List<Company> result = new ArrayList<>(rows.size());
            Map<String, Company> companyById = new LinkedHashMap<>(rows.size());

            for (Object[] row : rows) {
                Company company = new Company();
                String companyId = (String) row[0];
                company.setCompanyId(companyId);
                company.setName((String) row[1]);
                company.setFirstname((String) row[2]);
                company.setLastname((String) row[3]);
                company.setOrganizationNumber((String) row[4]);
                company.setPhone((String) row[5]);
                company.setEmail((String) row[6]);
                company.setWebSite((String) row[7]);
                company.setCompanyType((String) row[8]);
                if (row[9] != null) {
                    company.setDefaultLanguageCode((String) row[9]);
                }
                company.setIsPersonCustomer(toBoolean(row[10]));
                company.setIsLiteAccount(toBoolean(row[11]));
                company.setFreeAccount(toBoolean(row[12]));
                company.setDemo(toBoolean(row[13]));
                company.setDeleted(toBoolean(row[14]));
                company.setProjectCounter(row[15] != null ? ((Number) row[15]).intValue() : 0);
                company.setCreatedDate(toDate(row[16]));
                company.setExpireDate(toDate(row[17]));

                // Initialiser tomme lister så JSON-serialisering ikke trigger lazy loading
                company.setImageList(new ArrayList<>());
                company.setUserList(new ArrayList<>());
                company.setAddressList(new ArrayList<>());
                company.setAssetList(new ArrayList<>());
                company.setAssetGroupList(new ArrayList<>());
                company.setProjectList(new ArrayList<>());
                company.setContractList(new ArrayList<>());
                company.setPartnerContracts(new ArrayList<>());
                company.setDisiplineList(new ArrayList<>());
                company.setDeviationList(new ArrayList<>());
                company.setReportList(new ArrayList<>());
                company.setInvoiceList(new ArrayList<>());
                company.setIntegrationList(new ArrayList<>());
                company.setLanguageList(new ArrayList<>());
                company.setUserRoleList(new ArrayList<>());
                company.setParameterList(new ArrayList<>());

                companyById.put(companyId, company);
                result.add(company);
            }

            // ... existing code ...
            // 3) Hent ALLE bilder for disse selskapene i én batch-spørring.
            //    Vi tar med alle skalarfelter fra image-tabellen (alt utenom relasjoner).
            //    EclipseLink + native query støtter ikke kolleksjons-ekspandering av
            //    navngitte parametere, så vi bygger IN-listen med posisjonsparametere.
            List<String> companyIdList = new ArrayList<>(companyById.keySet());
            String placeholders = companyIdList.stream()
                    .map(x -> "?")
                    .collect(Collectors.joining(", "));

            String mediaSql = """
                    SELECT
                        chi.company        AS company_id,
                        img.image_id,
                        img.deleted,
                        img.name,
                        img.original_name,
                        img.url_small,
                        img.url_medium,
                        img.url_large,
                        img.media_type,
                        img.media_purpose,
                        img.flir_configuration,
                        img.order_index
                    FROM company_has_image chi
                    JOIN image img ON img.image_id = chi.image
                    WHERE chi.company IN (%s)
                    ORDER BY chi.company, img.order_index
                    """.formatted(placeholders);

            Query mediaQuery = em.createNativeQuery(mediaSql);
            for (int i = 0; i < companyIdList.size(); i++) {
                mediaQuery.setParameter(i + 1, companyIdList.get(i));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> mediaRows = mediaQuery.getResultList();

            // 4) Distribuer Media-objektene til riktig Company
            for (Object[] r : mediaRows) {
                String companyId = (String) r[0];
                Company company = companyById.get(companyId);
                if (company == null) {
                    continue;
                }

                Media media = new Media();
                media.setMediaId((String) r[1]);
                media.setDeleted(toBoolean(r[2]));
                media.setName((String) r[3]);
                media.setOriginalName((String) r[4]);
                media.setUrlSmall((String) r[5]);
                media.setUrlMedium((String) r[6]);
                media.setUrlLarge((String) r[7]);
                media.setMediaType((String) r[8]);
                media.setMediaPurpose((String) r[9]);
                media.setFlirConfiguration((String) r[10]);
                media.setOrderIndex(r[11] != null ? ((Number) r[11]).intValue() : null);

                company.getImageList().add(media);
            }

            return result;
// ... existing code ...
        } catch (NumberFormatException e) {
            System.out.println("Invalid batch parameters for loadByAuthorityOptimized2");
            System.out.println("Company ID: " + id + ", offset: " + batchOffset + ", size: " + batchSize);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Exception in loadByAuthorityOptimized2 for Company ID: " + id);
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Trygg konvertering av JDBC-verdier til Boolean.
     * Håndterer Boolean (MySQL tinyint(1) via moderne Connector/J),
     * Number (eldre drivere) og String ("1"/"0"/"true"/"false").
     */
    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }

    /**
     * Trygg konvertering av JDBC-verdier til Date.
     * Håndterer java.sql.Timestamp, java.sql.Date og java.util.Date.
     */
    private static Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date d) {
            return new Date(d.getTime());
        }
        return null;
    }
// ... existing code ...

    @GET
    @Path("countByAuthority/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer countByAuthority(@PathParam("authorityId") String authorityId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Number counter = (Number) em.createNativeQuery("""
                            SELECT COUNT(*)
                            FROM company c
                            JOIN contract cont ON c.company_id = cont.company
                            JOIN company customer ON cont.partner = customer.company_id
                            WHERE c.company_id = ?1
                            """)
                    .setParameter(1, authorityId)
                    .getSingleResult();

            return counter.intValue();
        } catch (Exception e) {
            System.out.println("Exception in countByAuthority for Authority ID: " + authorityId);
            System.out.println("Error: " + e.getMessage());
            return 0;
        }
    }

    private Company optimizeCompany(Company company) {
        Company temp = company.duplicate();
        temp.getUserList().clear();

        if (!temp.getAssetGroupList().isEmpty()) {
            AssetGroup tempAssetGroup = temp.getAssetGroupList().get(0);
            List<Asset> tempList = tempAssetGroup.getAssetList().subList(0, 0);
            tempAssetGroup.setAssetList(tempList);
        }
        temp.setLanguageList(company.getLanguageList());
        temp.setProjectList(new ArrayList<>());
        temp.setAssetList(new ArrayList<>());
        temp.setContractList(new ArrayList<>());
        temp.setPartnerContracts(new ArrayList<>());
        temp.setDefaultLanguageCode(company.getDefaultLanguageCode());
        return temp;
    }


    @GET
    @Path("loadUserCompanyLites/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> loadUserCompanyLites(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    JOIN company_has_user chu ON chu.company = c.company_id
                                    JOIN user u ON chu.user = u.user_id
                                    WHERE u.user_id = ?1
                                    """,
                            Company.class)
                    .setParameter(1, userId)
                    .getResultList();

            return resultList.stream()
                    .map(CompanyLite::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in loadUserCompanyLites for User ID: " + userId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadUserCompaniesOptimized/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadUserCompaniesOptimized(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    JOIN company_has_user chu ON chu.company = c.company_id
                                    JOIN user u ON chu.user = u.user_id
                                    WHERE u.user_id = ?1
                                    """,
                            Company.class)
                    .setParameter(1, userId)
                    .getResultList();

            return resultList.stream()
                    .map(this::optimizeCompany)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in loadUserCompaniesOptimized for User ID: " + userId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadUserCompaniesOptimized2/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadUserCompaniesOptimized2(@PathParam("userId") String userId) {
        List<Company> resultList = loadUserCompaniesOptimized(userId);
        for (Company company : resultList) {
            for (Disipline disipline : company.getDisiplineList()) {
                disipline.setEquipmentTypeList(new ArrayList<>());
                disipline.setMeasurementList(new ArrayList<>());
            }
            //company.setReportList(new ArrayList<>());
        }
        return resultList;
    }

    @GET
    @Path("loadUserCompaniesOptimized3/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadUserCompaniesOptimized3(@PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    JOIN company_has_user chu ON chu.company = c.company_id
                                    JOIN user u ON chu.user = u.user_id
                                    WHERE u.user_id = ?1
                                    """,
                            Company.class)
                    .setParameter(1, userId)
                    .getResultList();

//            resultList = resultList.stream()
//                    .map(this::optimizeCompany)
//                    .collect(Collectors.toList());

            for (Company company : resultList) {
                for (Disipline disipline : company.getDisiplineList()) {
                    disipline.setEquipmentTypeList(new ArrayList<>());
                    disipline.setMeasurementList(new ArrayList<>());
                }
                company.setAssetList(new ArrayList<>());
                company.setAssetGroupList(new ArrayList<>());
                //company.setLanguageList(company.getLanguageList());
                company.setProjectList(new ArrayList<>());
                company.setAssetList(new ArrayList<>());
                company.setContractList(new ArrayList<>());
                company.setPartnerContracts(new ArrayList<>());
                company.setUserList(new ArrayList<>());
                //company.setDefaultLanguageCode(company.getDefaultLanguageCode());
                //company.setReportList(new ArrayList<>());
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception in loadUserCompaniesOptimized for User ID: " + userId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    @GET
    @Path("loadByAuthorityLite/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> loadByAuthorityLite(@PathParam("companyid") String id,
                                                 @PathParam("batchOffset") String batchOffset,
                                                 @PathParam("batchSize") String batchSize) {

        List<Company> companies = loadByAuthority2(id, batchOffset, batchSize);
        List<CompanyLite> companyLites = new ArrayList<>();
        for (Company company : companies) {
            companyLites.add(new CompanyLite(company));
        }
        return companyLites;
    }


    @GET
    @Path("loadPartners/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadPartners(@PathParam("authorityId") String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> companies = em.createNativeQuery("""
                                    SELECT DISTINCT customer.*
                                    FROM company c
                                    JOIN contract cont ON c.company_id = cont.company
                                    JOIN company customer ON cont.partner = customer.company_id
                                    WHERE c.company_id = ?1 
                                      AND cont.contract_type = ?2
                                    """,
                            Company.class)
                    .setParameter(1, id)
                    .setParameter(2, "ENTREPRENEUR")
                    .getResultList();

            for (Company company : companies) {
                company.setAssetGroupList(new ArrayList<>());
                company.setDisiplineList(new ArrayList<>());
                company.setContractList(new ArrayList<>());
                company.setReportList(new ArrayList<>());
            }
            return companies;
        } catch (Exception e) {
            System.out.println("Exception in loadPartners for Authority ID: " + id);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadPartnerLites/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> loadPartnerLites(@PathParam("authorityId") String id) {
        List<Company> companies = loadPartners(id);
        List<CompanyLite> companyLites = new ArrayList<>();
        for (Company company : companies) {
            companyLites.add(new CompanyLite(company));
        }
        return companyLites;
    }


    @GET
    @Path("queryByAuthority/{companyid}/{querystring}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryByAuthority(@PathParam("companyid") String id, @PathParam("querystring") String queryString) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String searchPattern = "%" + queryString + "%";

            List<Company> resultList = em.createNativeQuery("""
                                    SELECT customer.*
                                    FROM company c
                                    JOIN contract cont ON c.company_id = cont.company
                                    JOIN company customer ON cont.partner = customer.company_id
                                    WHERE c.company_id = ?1 
                                      AND customer.name LIKE ?2
                                    ORDER BY customer.name ASC
                                    """,
                            Company.class)
                    .setParameter(1, id)
                    .setParameter(2, searchPattern)
                    .getResultList();

            return resultList.stream()
                    .map(company -> {
                        ResultItem item = new ResultItem();
                        item.setName(company.getName());
                        item.setId(company.getCompanyId());
                        return item;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in queryByAuthority");
            System.out.println("Company ID: " + id + ", Query: " + queryString);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("queryByAllAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryAllByAuthority(@PathParam("companyid") String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Company> resultList = em.createNativeQuery("""
                                    SELECT DISTINCT partner.*
                                    FROM contract cont
                                    JOIN company partner ON partner.company_id = cont.company 
                                                         OR partner.company_id = cont.partner
                                    WHERE (cont.partner = ?1 OR cont.company = ?1)
                                      AND (partner.deleted = 0 OR partner.deleted IS NULL)
                                    ORDER BY partner.name
                                    """,
                            Company.class)
                    .setParameter(1, id)
                    .getResultList();

            return resultList.stream()
                    .map(company -> {
                        ResultItem item = new ResultItem();
                        item.setName(company.getName());
                        item.setId(company.getCompanyId());
                        return item;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception in queryAllByAuthority for Company ID: " + id);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("queryByOrganizationNumber/{organizationnumber}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryByOrganizationNumber(@PathParam("organizationnumber") String organizationNumber) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    WHERE c.organization_number = ?1
                                    """,
                            Company.class)
                    .setParameter(1, organizationNumber)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in queryByOrganizationNumber for Organization Number: " + organizationNumber);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadByOrganizationNumber/{organizationnumber}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByOrganizationNumber(@PathParam("organizationnumber") String organizationNumber) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    WHERE c.organization_number = ?1
                                    """,
                            Company.class)
                    .setParameter(1, organizationNumber)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByOrganizationNumber for Organization Number: " + organizationNumber);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }


    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public String pingREST() {
        return String.valueOf(1);
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    @GET
    @Path("contracts/{contracttype}/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> findCustomerContracts(@PathParam("contracttype") String contracttype, @PathParam("id") String id) {
        Company company = super.find(id);
        if (company != null) {
            return company.getContractList();
        }
        return null;
    }


    @PUT
    @Path("getCustomerIds/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ResultItem> getCustomerId(List<String> projectIds) {
        List<ResultItem> resultItems = new ArrayList<>();
        for (String projectId : projectIds) {
            List<Company> companies = loadByProjectId(projectId);
            Company customer = null;
            for (Company company : companies) {
                if (company.getCompanyType().equalsIgnoreCase("OWNER")) {
                    customer = company;
                }
            }
            if (customer != null) {
                ResultItem resultItem = new ResultItem();
                resultItem.setId(customer.getCompanyId());
                resultItem.setName(customer.getFullName());
                resultItem.setClazz(projectId);
                if (customer.getImage() != null) {
                    resultItem.setImage(customer.getImage());
                }
                resultItems.add(resultItem);
            }
        }
        return resultItems;
    }

    @Deprecated
    @GET
    @Path("loadByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByProject(@PathParam("projectId") String projectId) {
        return loadByProjectId(projectId);
    }

    @GET
    @Path("loadByProjectOptimized/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByProjectOptimized(@PathParam("projectId") String projectId) {
        List<Company> optimizedCompanies = new ArrayList<>();
        List<Company> companies = loadByProjectId(projectId);
        for (Company company : companies) {
            optimizedCompanies.add(optimizeCompany(company));
        }
        for (Company company : optimizedCompanies) {
            company.setDisiplineList(new ArrayList<>());
            company.setAddressList(new ArrayList<>());
            company.setAssetList(new ArrayList<>());
            company.setAssetGroupList(new ArrayList<>());
            //company.setReportList(new ArrayList<>());
            company.setUserRoleList(new ArrayList<>());
            company.setInvoiceList(new ArrayList<>());

        }
        return optimizedCompanies;
    }

    @GET
    @Path("loadByProjectOptimized2/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByProjectOptimized2(@PathParam("projectId") String projectId) {
        List<Company> optimizedCompanies = new ArrayList<>();
        List<Company> companies = loadByProjectId(projectId);
        for (Company company : companies) {
            optimizedCompanies.add(optimizeCompany(company));
        }
        for (Company company : optimizedCompanies) {
            company.setDisiplineList(new ArrayList<>());
            company.setAddressList(new ArrayList<>());
            company.setAssetList(new ArrayList<>());
            company.setAssetGroupList(new ArrayList<>());
            company.setReportList(new ArrayList<>());
            company.setUserRoleList(new ArrayList<>());
            company.setInvoiceList(new ArrayList<>());

        }
        return optimizedCompanies;
    }

    @GET
    @Path("loadByProjectLite/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> loadByProjectLite(@PathParam("projectId") String projectId) {
        List<Company> companies = loadByProjectId(projectId);
        List<CompanyLite> lites = new ArrayList<>();
        for (Company company : companies) {
            lites.add(new CompanyLite(company));
        }
        return lites;
    }

    private List<Company> loadByProjectId(String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                                    SELECT c.*
                                    FROM company c
                                    JOIN company_has_project chp ON chp.company_company_id = c.company_id
                                    WHERE chp.project_project_id = ?1
                                    """,
                            Company.class)
                    .setParameter(1, projectId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByProjectId for Project ID: " + projectId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Company getReducedCompany(Company source) {
        Company reduced = new Company();
        reduced.setCompanyType(source.getCompanyType());
        reduced.setCompanyId(source.getCompanyId());
        reduced.setOrganizationNumber((source.getOrganizationNumber()));
        reduced.setName(source.getName());
        reduced.setPhone(source.getPhone());
        reduced.setDeleted(source.isDeleted());
        reduced.setFirstname(source.getFirstname());
        reduced.setLastname(source.getLastname());
        reduced.setIsPersonCustomer(source.getIsPersonCustomer());
        reduced.setImageList(source.getImageList());
        reduced.setUserList(source.getUserList());
        reduced.setAssetGroupList(source.getAssetGroupList());
        return reduced;
    }

    @GET
    @Path("queryCompanyLites/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> queryCompanyLites(@PathParam("authorityId") String authorityId, @PathParam("queryString") String queryString) {
        List<Company> companies = queryCompanies(authorityId, queryString);
        List<CompanyLite> companyLites = new ArrayList<>();
        for (Company company : companies) {
            companyLites.add(new CompanyLite(company));
        }
        return companyLites;
    }

    @GET
    @Path("queryCompanies/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryCompanies(@PathParam("authorityId") String authorityId, @PathParam("queryString") String queryString) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String decodedQuery = URLDecoder.decode(queryString, StandardCharsets.UTF_8);

            // Hvis det er 9 siffer, søk etter organisasjonsnummer
            if (decodedQuery.length() == 9 && decodedQuery.matches("^[0-9]+$")) {
                return searchByOrganizationNumber(em, authorityId, decodedQuery);
            }

            // Ellers søk etter navn
            return searchByName(em, authorityId, decodedQuery);
        } catch (Exception e) {
            System.out.println("Exception in queryCompanies");
            System.out.println("Authority ID: " + authorityId + ", Query: " + queryString);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Company> searchByOrganizationNumber(EntityManager em, String authorityId, String orgNumber) {
        return em.createNativeQuery("""
                                SELECT c.*
                                FROM company c
                                JOIN contract cont ON cont.partner = c.company_id
                                WHERE cont.company = ?1 
                                  AND c.organization_number = ?2
                                  AND (c.deleted = 0 OR c.deleted IS NULL)
                                LIMIT 7
                                """,
                        Company.class)
                .setParameter(1, authorityId)
                .setParameter(2, orgNumber)
                .getResultList();
    }

    private List<Company> searchByName(EntityManager em, String authorityId, String searchTerm) {
        String searchPattern = "%" + searchTerm + "%";

        return em.createNativeQuery("""
                                SELECT c.*
                                FROM company c
                                JOIN contract cont ON cont.partner = c.company_id
                                WHERE cont.company = ?1 
                                  AND (c.name LIKE ?2 
                                       OR CONCAT(c.firstname, ' ', c.lastname) LIKE ?2 
                                       OR CONCAT(c.lastname, ' ', c.firstname) LIKE ?2)
                                  AND (c.deleted = 0 OR c.deleted IS NULL)
                                LIMIT 7
                                """,
                        Company.class)
                .setParameter(1, authorityId)
                .setParameter(2, searchPattern)
                .getResultList();
    }

    @GET
    @Path("queryCompaniesOptimized/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryCompaniesOptimized(@PathParam("authorityId") String authorityId, @PathParam("queryString") String queryString) {
        List<Company> companies = queryCompanies(authorityId, queryString);
        List<Company> optimizedCompanies = new ArrayList<>();
        companies = companies.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
        for (Company company : companies) {
            Company optimizedCompany = optimizeCompany(company);
            optimizedCompany.setAssetList(new ArrayList<>());
            for (AssetGroup assetGroup : optimizedCompany.getAssetGroupList()) {
                assetGroup.setAssetList(new ArrayList());
            }
            //optimizedCompany.setAssetGroupList(new ArrayList<>());
            optimizedCompanies.add(optimizedCompany);
        }
        return optimizedCompanies;
    }

    @GET
    @Path("queryAllServicesCompanies/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryAllServicesCompanies(@PathParam("authorityId") String authorityId, @PathParam("queryString") String queryString) {

        queryString = queryString.replaceAll(" ", "+");
        List<Company> companies = new ArrayList<>(queryCompanies(authorityId, queryString));
        companies.addAll(searchBrreg(queryString));
        String replacedQueryString = queryString.replace("+", " ");
        List<Company> bestMatch = companies.stream()
                .filter(p -> p.getFullNameWithFirstNameFirst().toLowerCase().startsWith(replacedQueryString.toLowerCase())).collect(Collectors.toList());
        List<Company> remaining = companies.stream()
                .filter(p -> !p.getFullNameWithFirstNameFirst().toLowerCase().startsWith(replacedQueryString.toLowerCase())).collect(Collectors.toList());

        List<Company> sorted = new ArrayList<>();
        sorted.addAll(bestMatch);
        sorted.addAll(remaining);
        for (Company company : sorted) {
            optimizeCompany(company);
        }
        return sorted;
    }

    @GET
    @Path("queryCompaniesWithParameter/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryCompaniesWithParameter(@PathParam("authorityId") String authorityId, @QueryParam("queryString") String queryString) {
        return queryAllServicesCompanies(authorityId, queryString);
    }

    @GET
    @Path("queryAllServicesCompanyLites/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> queryAllServicesCompanyLites(@PathParam("authorityId") String authorityId, @PathParam("queryString") String queryString) {
        List<Company> companies = new ArrayList<>(queryAllServicesCompanies(authorityId, queryString));
        List<CompanyLite> companyLites = new ArrayList<>();
        for (Company company : companies) {
            companyLites.add(new CompanyLite(company));
        }
        return companyLites;
    }


    @GET
    @Path("companyHasAsset/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyHasAsset> companyHasAsset(@PathParam("companyId") String companyId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                                    SELECT *
                                    FROM company_has_asset
                                    WHERE company_company_id = ?1
                                    """,
                            CompanyHasAsset.class)
                    .setParameter(1, companyId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in companyHasAsset for Company ID: " + companyId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("searchBrreg/{searchString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> searchBrreg(@PathParam("searchString") String searchString) {
        List<Company> companies = new ArrayList<>();
        try {
            boolean isOrganizationNo = false;
            if (searchString.length() == 9) {
                String regex = "^[0-9]+";
                if (searchString.matches(regex)) {
                    isOrganizationNo = true;
                    Enhet enhet = BrregJsonClient.findOrganizationNo(searchString);
                    if (enhet != null) {
                        Company company = enhet.toCompany();
                        companies.add(company);
                    }
                }
            }
            if (isOrganizationNo) {
                String urlSearchString = URLEncoder.encode(searchString, "UTF-8");
            } else {
                String urlSearchString = URLEncoder.encode(searchString, "UTF-8");
                urlSearchString = urlSearchString.replaceAll("\\+", "%20");
                urlSearchString = urlSearchString.replaceAll(" ", "%20");
                BrregResult brregResult = BrregJsonClient.queryBrreg(urlSearchString);
                if (brregResult != null) {
                    if (brregResult.get_embedded() != null) {
                        companies = brregResult.toCompanies();
                    }
                }
            }
            if (companies.size() > 10) {
                companies = companies.subList(0, 9);
            }
            return companies;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return companies;
    }

    @GET
    @Path("users/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findUsers(@PathParam("id") String id) {
        Company company = super.find(id);
        if (company != null) {
            return company.getUserList();
        }
        return null;
    }

}
