/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.company")
@RolesAllowed({"ApplicationRole"})
public class CompanyFacadeREST extends AbstractFacade<Company> {

    public CompanyFacadeREST() {
        super(Company.class);
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
        super.create(entity);
    }

    @Deprecated
    @POST
    @Path("createWithContract/{companyId}/{contractType}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithContract(@PathParam("companyId") String companyId, @PathParam("contractType") String contractType, Company partner) {
        ContractFacadeREST contractFacadeREST = new ContractFacadeREST();
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
        contractFacadeREST.create(contract);
        partner.getPartnerContracts().add(contract);
        company.getContractList().add(contract);
        edit(company);
        edit(partner);
        contractFacadeREST.edit(contract);
    }

    @POST
    @Path("createWithContract2/{companyId}/{contractType}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Company createWithContract2(@PathParam("companyId") String companyId, @PathParam("contractType") String contractType, Company partner) {
        ContractFacadeREST contractFacadeREST = new ContractFacadeREST();
        // Check if partner exist in iDoc already
        boolean isPartnerAlready = false;
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

            AssetGroupFacadeREST assetGroupFacadeREST = new AssetGroupFacadeREST();
            assetGroupFacadeREST.create(assetGroup);
            partner.getAssetGroupList().add(assetGroup);
            assetGroupFacadeREST.linkToCompany(partner.getCompanyId(), assetGroup);

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
            contractFacadeREST.create(contract);
            partner.getPartnerContracts().add(contract);
            company.getContractList().add(contract);
            contractFacadeREST.edit(contract);
        } else {
            //edit(partner);
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
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Company company = this.find(entity.getCompanyId());
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        if (disipline != null && company != null) {
            if (!disipline.getCompanyList().contains(company)) {
                disipline.getCompanyList().add(company);
                disiplineFacadeREST.edit(disipline);
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
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Company company = this.find(entity.getCompanyId());
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        if (disipline != null && company != null) {
            if (disipline.getCompanyList().contains(company)) {
                disipline.getCompanyList().remove(company);
                disiplineFacadeREST.edit(disipline);
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
        Company temp = company.duplicate();

        List<AssetGroup> tempAssetGroups = new ArrayList<>();
        for (AssetGroup assetGroup : company.getAssetGroupList()) {
            AssetGroup tempAssetGroup = assetGroup.duplicate();
            List<Asset> tempList = assetGroup.getAssetList().subList(0, 0);
            tempAssetGroup.setAssetList(tempList);
            tempAssetGroups.add(tempAssetGroup);
        }
        temp.setAssetGroupList(tempAssetGroups);
        temp.setAssetList(new ArrayList<>());
        temp.setUserList(new ArrayList<>());
        temp.setProjectList(new ArrayList<>());
        temp.setReportList(new ArrayList<>());
        temp.setContractList(new ArrayList<>());
        //temp.getAssetList().clear();
        //temp.getUserList().clear();

        return temp;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> findAll() {
        return super.findAll();
    }


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

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT "
                        + "* FROM company c\n"
                        + "WHERE c.company_id = ?1",
                Company.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (resultList.isEmpty()) {
            return null;
        } else {
            Company company = resultList.get(0);

            return company;
            //return temp;
        }
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByIntegration(@PathParam("service") String service,@PathParam("primaryKey") String primaryKey) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT "
                                + "* FROM company c\n"
                                + "JOIN company_has_integration chi on chi.company_company_id = c.company_id \n"
                                + "JOIN integration i on i.integration_id = chi.integration_integration_id \n"
                                + "WHERE i.keyy = 'PRIMARY_KEY' AND  i.service = ?1 AND i.valuee = ?2",
                        Company.class)
                .setParameter(1, service)
                .setParameter(2, primaryKey)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("incrementProjectCounter/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectNumber incrementProjectCounter(@PathParam("companyId") String companyId) {
        Company company = findNative(companyId);
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            if (company != null) {
                company.setProjectCounter(company.getProjectCounter() + 1);
                int counter = company.getProjectCounter();

                tx.begin();
                final int i = em.createNativeQuery(
                        "UPDATE company SET project_counter = ?\n" +
                                "WHERE (company_id = ?);"
                ).setParameter(1, counter)
                        .setParameter(2, company.getCompanyId())
                        .executeUpdate();
                tx.commit();

                //edit(company);
                ProjectNumber pn = new ProjectNumber();
                pn.setProjectCounter(counter);
                return pn;
            } else {
                ProjectNumber pn = new ProjectNumber();
                pn.setProjectCounter(0);
                return pn;
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("------------------------------------------------");
            System.out.println("AAARGHHF!!! I da svartaste grønheitaste heeeæææælvetteeeeee.....");
            System.out.println("Exception while update company.project_counter: " + exp.getMessage());
            System.out.println("**************************************************************");
            ProjectNumber pn = new ProjectNumber();
            pn.setProjectCounter(0);
            return pn;
        } finally {
            em.close();
        }
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

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<WalletCompany> resultList = (List<WalletCompany>) em.createNativeQuery(
                "SELECT \n" +
                        "    wallet_company.authority_id as authority_id,\n" +
                        "    wallet_company.authority_name as authority_name, \n" +
                        "    wallet_company.authority_address,\n" +
                        "    wallet_company.demo_expire_date,\n" +
                        "    wallet_company.disipline, \n" +
                        "    sum(wallet_company.least_counter) as point_counter, \n" +
                        "    wallet_company.point_limit, \n" +
                        "    wallet_company.package_price, \n" +
                        "    wallet_company.package_discount, \n" +
                        "    sum(wallet_company.revenue * wallet_company.package_discount) as revenue,\n" +
                        "    wallet_company.start_date as invoice_start_date,\n" +
                        "    company_image\n" +
                        "FROM\n" +
                        "(SELECT \n" +
                        "   authority.company_id as authority_id,\n" +
                        "   authority.name as authority_name,\n" +
                        "   authority.expire_date as demo_expire_date,\n" +
                        "   d.name as disipline,\n" +
                        "   p.name as project_name,\n" +
                        "   p.disipline as project_disipline,\n" +
                        "   p.created_date as project_date,\n" +

                        "   d.max_children,\n" +
                        "   d.point_price as point_factor,\n" +
                        "   i.point_limit as point_limit,\n" +
                        "   i.point_price,\n" +
                        "   i.point_price as package_price,\n" +
                        "   i.point_discount as package_discount,\n" +
                        "   least(count(p.project_id),d.max_children) * d.point_price as least_counter,\n" +
                        "   least(count(p.project_id),d.max_children) * d.point_price * i.point_price as revenue,\n" +
                        "   i.start_date as start_date,\n" +
                        "   (select \n" +
                        "       img.image_id\n" +
                        "    from company\n" +
                        "       join company_has_image chi on chi.company = company.company_id\n" +
                        "       join image img on img.image_id = chi.image\n" +
                        "    where \n" +
                        "       company.company_id = authority.company_id\n" +
                        "    limit 0,1\n" +
                        "   ) as company_image,\n" +
                        "   (select \n" +
                        "       concat(addr.address1, ', ', addr.zip_code, ' ', addr.city, ' - ', addr.country)\n" +
                        "   from company\n" +
                        "       join company_has_address cha on cha.company_company_id = company.company_id\n" +
                        "       join address addr on addr.address_id = cha.address_address_id\n" +
                        "   where \n" +
                        "       addr.zip_code != \"\" and\n" +
                        "       cha.company_company_id = authority.company_id\n" +
                        "       limit 0,1\n" +
                        "   ) as authority_address\n" +
                        "    \n" +
                        "FROM company as authority\n" +
                        "   left join invoice i on i.company = authority.company_id\n" +
                        "   left join project p on p.created_company = authority.company_id\n" +
                        "   left join disipline d on p.disipline = d.disipline_id\n" +
                        "   left join project child on child.parent = p.project_id\n" +
                        "WHERE \n" +
                        "   authority.company_id = ?1 and\n" +
                        "   i.end_date is null and\n" +
                        "   p.parent is null and\n" +
                        "   p.created_company = authority.company_id and\n" +
                        "   (p.deleted = 0 or p.deleted is null) AND     \n" +
                        "   (child.deleted = 0 or child.deleted is null) and    \n" +
                        "   p.created_date > i.start_date\n" +
                        "group by authority.company_id, p.project_id\n" +
                        "order by authority.name) as wallet_company\n" +
                        "group by wallet_company.authority_id\n" +
                        "order by authority_name",
                WalletCompany.class)
                .setParameter(1, companyId)
                .getResultList();
        if(!resultList.isEmpty()) {
            return new CompanyItem(resultList.get(0));
        } else {
            // Maybe nothing in wallet since it is no projects there yet, find latest invoice tp find point limit
            CompanyItem companyItem = new CompanyItem();
            companyItem.setPointCount(0.0);
            companyItem.setPointLimit(0.0); // Forces to buy, in there is no openInvoices
            CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
            Company company = companyFacadeREST.findNative(companyId);
            List<Invoice> openInvoices = company.getInvoiceList().stream().filter(r -> r.getEndDate() == null).collect(Collectors.toList());
            if(!openInvoices.isEmpty()) {
                Invoice openInvoice = openInvoices.get(0);
                companyItem.setPointLimit(openInvoice.getPointLimit());
            }
            companyItem.setCompanyId(companyId);
            companyItem.setName(company.getFullName());
            return companyItem;
        }
    }

    @PUT
    @Path("loadCompanyItems")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<CompanyItem> loadCompanyItems(ProjectRequestParameters parameters) {
        List<CompanyItem> companyItems = new ArrayList<>();
        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        List<Project> projects = projectFacadeREST.loadProjects(parameters);
        return companyItems;
    }


    @GET
    @Path("loadCompanyItems/{companyid}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyItem> loadCompanyItems(@PathParam("companyid") String id,
                                              @PathParam("batchOffset") String batchOffset,
                                              @PathParam("batchSize") String batchSize) {
        List<CompanyItem> companyItems = new ArrayList<>();
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        //findByCompanyId
        String sqlQuery = "SELECT \n" +
                "    wallet_company.authority_id as authority_id,\n" +
                "    wallet_company.authority_name as authority_name, \n" +
                "    wallet_company.authority_address,\n" +
                "    wallet_company.demo_expire_date,\n" +
                "    wallet_company.disipline, \n" +
                "    sum(wallet_company.least_counter) as point_counter, \n" +
                "    wallet_company.point_limit, \n" +
                "    wallet_company.point_transfered, \n" +
                "    wallet_company.package_price, \n" +
                "    wallet_company.package_discount, \n" +
                "    sum(wallet_company.revenue * wallet_company.package_discount) as revenue,\n" +
                "    wallet_company.start_date as invoice_start_date,\n" +
                "    company_image\n" +
                "FROM\n" +
                "(SELECT \n" +
                "   authority.company_id as authority_id,\n" +
                "   authority.name as authority_name,\n" +
                "   authority.expire_date as demo_expire_date,\n" +
                "   d.name as disipline,\n" +
                "   p.name as project_name,\n" +
                "   p.disipline as project_disipline,\n" +
                "   p.created_date as project_date,\n" +
                "   d.max_children,\n" +
                "   d.point_price as point_factor,\n" +
                "   i.point_limit as point_limit,\n" +
                "   i.point_transfered as point_transfered,\n" +
                "   i.point_price,\n" +
                "   i.point_price as package_price,\n" +
                "   i.point_discount as package_discount,\n" +
                "   least(count(p.project_id),d.max_children) * d.point_price as least_counter,\n" +
                "   least(count(p.project_id),d.max_children) * d.point_price * i.point_price as revenue,\n" +
                "   i.start_date as start_date,\n" +
                "   (select \n" +
                "       img.image_id\n" +
                "    from company\n" +
                "       join company_has_image chi on chi.company = company.company_id\n" +
                "       join image img on img.image_id = chi.image\n" +
                "    where \n" +
                "       company.company_id = authority.company_id\n" +
                "    limit 0,1\n" +
                "   ) as company_image,\n" +
                "   (select \n" +
                "       concat(addr.address1, ', ', addr.zip_code, ' ', addr.city, ' - ', addr.country)\n" +
                "   from company\n" +
                "       join company_has_address cha on cha.company_company_id = company.company_id\n" +
                "       join address addr on addr.address_id = cha.address_address_id\n" +
                "   where \n" +
                "       addr.zip_code != \"\" and\n" +
                "       cha.company_company_id = authority.company_id\n" +
                "       limit 0,1\n" +
                "   ) as authority_address\n" +
                "    \n" +
                "FROM company as authority\n" +
                "   left join invoice i on i.company = authority.company_id\n" +
                "   left join project p on p.created_company = authority.company_id\n" +
                "   left join disipline d on p.disipline = d.disipline_id\n" +
                "   left join project child on child.parent = p.project_id\n" +
                "WHERE \n" +
                "   authority.company_type = \"AUTHORITY\" and\n" +
                "   p.created_company != \"E07121A7-024A-4D0E-8B58-A064F0BC4A22\" and\n" +
                "   p.created_company = authority.company_id and\n" +
                "   i.end_date is null and\n" +
                "   p.parent is null and\n" +
                "   (p.deleted = 0 or p.deleted is null) AND     \n" +
                "   (child.deleted = 0 or child.deleted is null) and    \n" +
                "   p.created_date > i.start_date\n" +
                "group by authority.company_id, p.project_id\n" +
                "order by authority.name) as wallet_company\n" +
                "group by wallet_company.authority_id\n" +
                "order by authority_name LIMIT ?2,?3";
        List<WalletCompany> resultList = (List<WalletCompany>) em.createNativeQuery(
                sqlQuery,
                WalletCompany.class)
                .setParameter(1, id)
                .setParameter(2, offset)
                .setParameter(3, size)
                .getResultList();
        for (WalletCompany walletCompany : resultList) {
            CompanyItem companyItem = new CompanyItem(walletCompany);
            //calculatePackagePoints(companyItem, company);
            companyItems.add(companyItem);
        }

        // Find companies which have not started invoices

        List<Company> authorities = loadAuthorities();
        Collections.sort(authorities, (Company o1, Company o2) -> o1.getFullName().compareTo(o2.getFullName()));
        for (Company authority : authorities) {
            CompanyItem authorityItem = new CompanyItem(authority);
            boolean exists = false;
            for (CompanyItem companyItem : companyItems) {
                if (companyItem.getCompanyId().equalsIgnoreCase(authorityItem.getCompanyId())) {
                    exists = true;
                }
            }
            if (!exists) {
                companyItems.add(authorityItem);
            }
//            List<CompanyItem> existing = companyItems.stream()
//                    .filter(r ->r.getCompanyId() == authorityItem.getCompanyId())
//                    .collect(Collectors.toList());
//            if(existing.isEmpty()) {
//                companyItems.add(authorityItem);
//            }
        }
        em.close();
        return companyItems;
    }

    @GET
    @Path("loadAuthorities")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadAuthorities() {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        //findByCompanyId
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT * \n"
                        + "FROM company\n"
                        + "WHERE company_type = ?1",
                Company.class)
                .setParameter(1, "AUTHORITY")
                .getResultList();
        em.close();
        return resultList;
    }

//    private void calculatePackagePoints(CompanyItem companyItem, Company company) {
//        List<Invoice> invoices = company.getInvoiceList();
//        if(!invoices.isEmpty()){
//            ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
//            List<Invoice> sortedInvoices = new ArrayList<>(invoices);
//            Collections.sort(sortedInvoices, (Invoice o1, Invoice o2) -> o2.getStartDate().compareTo(o1.getStartDate()));
//            Invoice latestInvoice = sortedInvoices.get(0);
//            DateTime jodaFromDateTime = new DateTime(latestInvoice.getStartDate());
//            DateTime jodaToDateTime = new DateTime(new Date());
//
//            Double pointCount = latestInvoice.getPointTransfered();
//            List<Project> projects = projectFacadeREST.loadProjectsForCompany(company.getCompanyId(),company.getCompanyId(),"99",jodaFromDateTime.toString(), jodaToDateTime.toString(),"0","10000");
//            for(Project project:projects){
//                pointCount = pointCount + getPointCount(project);
//            }
//            companyItem.setPointPrice(latestInvoice.getPointPrice());
//            companyItem.setPointCount(pointCount);
//            companyItem.setPointLimit(latestInvoice.getPointLimit());
//        }
//    }

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

        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        //findByCompanyId
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT DISTINCT customer.* \n"
                        + "FROM company c\n"
                        + "JOIN contract cont\n"
                        + "    on c.company_id = cont.company\n"
                        + "JOIN company customer\n"
                        + "	on cont.partner = customer.company_id\n"
                        + "WHERE c.company_id = ?1 ORDER BY CONCAT(customer.name,customer.lastname,', ',customer.firstname) COLLATE utf8mb4_danish_ci ASC "
                        + "LIMIT ?2,?3",
                Company.class)
                .setParameter(1, id)
                .setParameter(2, offset)
                .setParameter(3, size)
                .getResultList();
        em.close();
        return resultList;
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

    private Company optimizeCompany(Company company) {
        Company temp = company.duplicate();
        temp.getUserList().clear();
        if (!temp.getAssetGroupList().isEmpty()) {
            AssetGroup tempAssetGroup = temp.getAssetGroupList().get(0);
            List<Asset> tempList = tempAssetGroup.getAssetList().subList(0, 0);
            tempAssetGroup.setAssetList(tempList);
        }
        temp.setProjectList(new ArrayList<>());
        temp.setAssetList(new ArrayList<>());
        temp.setContractList(new ArrayList<>());
        temp.setPartnerContracts(new ArrayList<>());
        return temp;
    }


    @GET
    @Path("loadUserCompanyLites/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CompanyLite> loadUserCompanyLites(@PathParam("userId") String userId) {
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
        List<CompanyLite> companyLites = new ArrayList<>();
        for (Company company : resultList) {
            companyLites.add(new CompanyLite(company));
        }
        return companyLites;
    }

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
        List<Company> optimizedCompanies = new ArrayList<>();
        for (Company company : resultList) {
            optimizedCompanies.add(optimizeCompany(company));
        }
        return optimizedCompanies;
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

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        //findByCompanyId
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT DISTINCT customer.* \n"
                        + "FROM company c\n"
                        + "JOIN contract cont\n"
                        + "    on c.company_id = cont.company\n"
                        + "JOIN company customer\n"
                        + "	on cont.partner = customer.company_id\n"
                        + "WHERE c.company_id = ?1 AND cont.contract_type = ?2",
                Company.class)
                .setParameter(1, id)
                .setParameter(2, "ENTREPRENEUR")

                .getResultList();
        em.close();
        return resultList;
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
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        //findByCompanyId
        List<ResultItem> resultItems = new ArrayList<>();
        queryString = "%" + queryString + "%";
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT customer.company_id, customer.name\n"
                        + "FROM company c\n"
                        + "JOIN contract cont\n"
                        + "    on c.company_id = cont.company\n"
                        + "JOIN company customer\n"
                        + "	on cont.partner = customer.company_id\n"
                        + "WHERE c.company_id = ?1 AND customer.name LIKE ?2 ORDER BY customer.name ASC",
                Company.class)
                .setParameter(1, id)
                .setParameter(2, queryString)
                .getResultList();
        for (Company company : resultList) {
            ResultItem item = new ResultItem();
            item.setName(company.getName());
            item.setId(company.getCompanyId());
            resultItems.add(item);
        }
        em.close();
        return resultItems;
    }

    @GET
    @Path("queryByAllAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryAllByAuthority(@PathParam("companyid") String id) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        //findByCompanyId
        List<ResultItem> resultItems = new ArrayList<>();
        List<Company> resultList = (List<Company>) em.createNativeQuery(
                "SELECT DISTINCT firma.name, firma.company_id, firma.deleted FROM (\n"
                        + "	SELECT * FROM contract cont\n"
                        + "	JOIN company partner ON partner.company_id = cont.company OR partner.company_id = cont.partner\n"
                        + "	WHERE cont.partner = ?1\n"
                        + "		OR cont.company = ?1) firma ORDER BY firma.name",
                Company.class)
                .setParameter(1, id)
                .getResultList();
        for (Company company : resultList) {
            if (!company.isDeleted()) {
                ResultItem item = new ResultItem();
                item.setName(company.getName());
                item.setId(company.getCompanyId());
                resultItems.add(item);
            }
        }
        em.close();
        return resultItems;


    }

    @GET
    @Path("queryByOrganizationNumber/{organizationnumber}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> queryByOrganizationNumber(@PathParam("organizationnumber") String organizationNumber) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT DISTINCT c.company_id, c.name\n"
                        + "FROM company c\n"
                        + "WHERE c.organization_number = ?1",
                Company.class)
                .setParameter(1, organizationNumber)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadByOrganizationNumber/{organizationnumber}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Company> loadByOrganizationNumber(@PathParam("organizationnumber") String organizationNumber) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        List<Company> resultList = (List<Company>) em.createNativeQuery("SELECT DISTINCT c.company_id, c.name\n"
                        + "FROM company c\n"
                        + "WHERE c.organization_number = ?1",
                Company.class)
                .setParameter(1, organizationNumber)
                .getResultList();

        em.close();
        return resultList;
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
            company.getDisiplineList().clear();
            company.getAddressList().clear();
            company.getReportList().clear();
            company.getReportList().clear();
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
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Company> companies = em.createNativeQuery("SELECT * FROM company c\n" +
                        "JOIN company_has_project chp\n" +
                        "   ON chp.company_company_id = c.company_id\n" +
                        "WHERE chp.project_project_id = ?1",
                Company.class)
                .setParameter(1, projectId)
                .getResultList();
        em.close();
//        List<Company> reducedCompanies = new ArrayList<>();
//        for (Company source : companies) {
//            reducedCompanies.add(getReducedCompany(source));
//        }
        return companies;
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

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            if (queryString.length() == 9) {
                String regex = "^[0-9]+";
                if (queryString.matches(regex)) {
                    List<Company> companies = (List<Company>) em.createNativeQuery("SELECT "
                                            + "* FROM company c\n"
                                            + "JOIN contract cont\n"
                                            + "	ON cont.partner = c.company_id\n"
                                            + "WHERE "
                                            + "cont.company = ?1 AND "
                                            + "(c.organization_number = ?2) LIMIT 0,7",
                                    Company.class)
                            .setParameter(1, authorityId)
                            .setParameter(2, queryString)
                            //.setParameter(3,null)
                            .getResultList();
                }
            }


            queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.toString());
            queryString =  "%" + queryString + "%";
            List<Company> companies = (List<Company>) em.createNativeQuery("SELECT "
                            + "* FROM company c\n"
                            + "JOIN contract cont\n"
                            + "	ON cont.partner = c.company_id\n"
                            + "WHERE "
                            + "cont.company = ?1 AND "
                            + "(c.name LIKE ?2 OR "
                            + "CONCAT(c.firstname, ' ', c.lastname) LIKE ?2 OR "
                            + "CONCAT(c.lastname, ' ', c.firstname) LIKE ?2) LIMIT 0,7",
                    Company.class)
                    .setParameter(1, authorityId)
                    .setParameter(2, queryString)
                    //.setParameter(3,null)
                    .getResultList();
            companies = companies.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
            return companies;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<Company>();
        } finally {
            em.close();
        }
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
            for(AssetGroup assetGroup: optimizedCompany.getAssetGroupList()) {
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
        List<CompanyHasAsset> companyHasAssets = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        companyHasAssets = (List<CompanyHasAsset>) em.createNativeQuery("SELECT "
                        + "* FROM company_has_asset \n"
                        + "WHERE "
                        + "company_company_id = ?1",
                CompanyHasAsset.class)
                .setParameter(1, companyId)
                .getResultList();
        em.close();
        return companyHasAssets;


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
                if (brregResult.get_embedded() != null) {
                    companies = brregResult.toCompanies();
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
